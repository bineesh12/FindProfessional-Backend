#!/usr/bin/env python3
"""Generate and validate Arbio's checked-in mobile and backend translations."""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Lock
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MOBILE_STRINGS = ROOT / "mobileApp/src/commonMain/composeResources/values/strings.xml"
MOBILE_RESOURCES = MOBILE_STRINGS.parent.parent
BACKEND_RESOURCES = ROOT / "backend/src/main/resources"
WORK = ROOT / ".local/localization-generation"
MODEL = "gpt-6-luna"
BUDGET_USD = 1.0
INPUT_USD_PER_TOKEN = 0.10 / 1_000_000
OUTPUT_USD_PER_TOKEN = 0.50 / 1_000_000
DEFAULT_BATCH_SIZE = 60
DEFAULT_MAX_WORKERS = 4
MAX_CALL_COST_RESERVE_USD = 0.004
LOCALES = [
    ("bg", "Bulgarian"), ("hr", "Croatian"), ("cs", "Czech"), ("da", "Danish"),
    ("nl", "Dutch"), ("et", "Estonian"), ("fi", "Finnish"), ("fr", "French"),
    ("de", "German"), ("el", "Greek"), ("hu", "Hungarian"), ("ga", "Irish"),
    ("it", "Italian"), ("lv", "Latvian"), ("lt", "Lithuanian"), ("mt", "Maltese"),
    ("pl", "Polish"), ("pt", "Portuguese"), ("ro", "Romanian"), ("sk", "Slovak"),
    ("sl", "Slovenian"), ("es", "Spanish"), ("sv", "Swedish"), ("ru", "Russian"),
    ("hi", "Hindi"),
]
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[a-zA-Z]|\{\d+\}")
PROTECTED_TOKEN = re.compile(
    r"https?://\S+|[\w.+-]+@[\w.-]+|"
    r"\b(?:SEK|EUR|GBP|USD|NOK|DKK|PLN|CHF|CZK|HUF|RON|BGN|RUB|INR)\b"
)


class TranslationCallError(RuntimeError):
    def __init__(self, message: str, cost: float = 0.0):
        super().__init__(message)
        self.cost = cost


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="Promote validated output into source folders")
    parser.add_argument("--locales", help="Comma-separated locale subset, primarily for resuming")
    parser.add_argument("--psql", default=os.getenv("PSQL", "psql"))
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE)
    parser.add_argument("--workers", type=int, default=DEFAULT_MAX_WORKERS)
    return parser.parse_args()


def mobile_entries() -> list[dict[str, str]]:
    root = ET.parse(MOBILE_STRINGS).getroot()
    return [
        {"id": f"mobile:{node.attrib['name']}", "text": "".join(node.itertext()), "kind": "mobile"}
        for node in root.findall("string")
    ]


def catalog_entries(psql: str) -> list[dict[str, str]]:
    sql = r"""
    SELECT json_build_object('kind','category','code',code,'name',name)::text
      FROM service_categories ORDER BY code;
    SELECT json_build_object('kind','service','code',s.code,'name',s.name,
      'shortDescription',s.short_description,'searchKeywords',s.search_keywords)::text
      FROM marketplace_services s ORDER BY s.code;
    SELECT json_build_object('kind','question','serviceCode',s.code,'key',q.question_key,
      'prompt',q.prompt,'helperText',q.helper_text)::text
      FROM service_questions q JOIN marketplace_services s ON s.id=q.service_id
      ORDER BY s.code,q.display_order,q.question_key;
    SELECT json_build_object('kind','option','serviceCode',s.code,'questionKey',q.question_key,
      'value',o.option_value,'label',o.label,'description',o.description)::text
      FROM question_options o JOIN service_questions q ON q.id=o.question_id
      JOIN marketplace_services s ON s.id=q.service_id
      ORDER BY s.code,q.question_key,o.display_order,o.option_value;
    """
    env = os.environ.copy()
    env.setdefault("PGPASSWORD", os.getenv("DATABASE_PASSWORD", "find_professional"))
    command = [
        psql, "-h", os.getenv("DATABASE_HOST", "127.0.0.1"),
        "-U", os.getenv("DATABASE_USERNAME", "find_professional"),
        "-d", os.getenv("DATABASE_NAME", "find_professional"), "-At", "-c", sql,
    ]
    rows = subprocess.run(command, env=env, check=True, capture_output=True, text=True).stdout.splitlines()
    entries: list[dict[str, str]] = []
    for row in rows:
        value = json.loads(row)
        kind = value["kind"]
        if kind == "category":
            entries.append({"id": f"category:{value['code']}:name", "text": value["name"], "kind": kind})
        elif kind == "service":
            for field in ("name", "shortDescription", "searchKeywords"):
                entries.append({"id": f"service:{value['code']}:{field}", "text": value[field], "kind": kind})
        elif kind == "question":
            entries.append({"id": f"question:{value['serviceCode']}:{value['key']}:prompt", "text": value["prompt"], "kind": kind})
            if value.get("helperText"):
                entries.append({"id": f"question:{value['serviceCode']}:{value['key']}:helperText", "text": value["helperText"], "kind": kind})
        else:
            base = f"option:{value['serviceCode']}:{value['questionKey']}:{value['value']}"
            entries.append({"id": f"{base}:label", "text": value["label"], "kind": kind})
            if value.get("description"):
                entries.append({"id": f"{base}:description", "text": value["description"], "kind": kind})
    return entries


def message_entries() -> list[dict[str, str]]:
    entries = []
    for line in (BACKEND_RESOURCES / "messages.properties").read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        key, value = line.split("=", 1)
        entries.append({"id": f"message:{key}", "text": value, "kind": "message"})
    return entries


def call_openai(api_key: str, language: str, batch: list[dict[str, str]]) -> tuple[dict[str, str], float]:
    requested = [{"id": item["id"], "text": item["text"]} for item in batch]
    schema = {
        "type": "object", "additionalProperties": False,
        "properties": {"translations": {"type": "array", "items": {
            "type": "object", "additionalProperties": False,
            "properties": {"id": {"type": "string"}, "text": {"type": "string"}},
            "required": ["id", "text"],
        }}}, "required": ["translations"],
    }
    body = {
        "model": MODEL, "store": False,
        "reasoning": {"effort": "none"},
        "max_output_tokens": 6000,
        "instructions": (
            f"Translate UI and marketplace catalog text from English to {language}. Return every id exactly once. "
            "Preserve Arbio, printf placeholders, {number} placeholders, newlines, URLs, email addresses, currency codes, "
            "stable codes, and measurement abbreviations exactly. Translate concise UI text naturally. Do not add facts, "
            "explanations, markdown, or extra punctuation. Comma-separated search keywords must remain comma-separated."
        ),
        "input": json.dumps(requested, ensure_ascii=False),
        "text": {"format": {"type": "json_schema", "name": "arbio_translations", "strict": True, "schema": schema}},
    }
    request = urllib.request.Request(
        "https://api.openai.com/v1/responses",
        data=json.dumps(body).encode(),
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            payload = json.load(response)
    except urllib.error.HTTPError as error:
        raise RuntimeError(error.read().decode()) from error
    text = next(
        content["text"]
        for output in payload.get("output", [])
        for content in output.get("content", [])
        if content.get("type") == "output_text"
    )
    usage = payload.get("usage", {})
    cost = usage.get("input_tokens", 0) * INPUT_USD_PER_TOKEN + usage.get("output_tokens", 0) * OUTPUT_USD_PER_TOKEN
    try:
        translations = json.loads(text)["translations"]
    except (json.JSONDecodeError, KeyError, TypeError) as error:
        raise TranslationCallError(f"Malformed structured translation output: {error}", cost) from error
    return {item["id"]: item["text"] for item in translations}, cost


def validate(source: list[dict[str, str]], translated: dict[str, str]) -> None:
    expected = {item["id"]: item["text"] for item in source}
    if set(expected) != set(translated):
        missing = sorted(set(expected) - set(translated))[:10]
        extra = sorted(set(translated) - set(expected))[:10]
        raise ValueError(f"Translation key mismatch; missing={missing}, extra={extra}")
    for key, original in expected.items():
        result = translated[key].strip()
        if not result:
            raise ValueError(f"Blank translation: {key}")
        if sorted(PLACEHOLDER.findall(original)) != sorted(PLACEHOLDER.findall(result)):
            raise ValueError(f"Placeholder mismatch: {key}: {original!r} -> {result!r}")
        if original.count("\n") != result.count("\n"):
            raise ValueError(f"Newline mismatch: {key}")
        if original.count("Arbio") != result.count("Arbio"):
            raise ValueError(f"Arbio brand changed: {key}")
        if sorted(PROTECTED_TOKEN.findall(original)) != sorted(PROTECTED_TOKEN.findall(result)):
            raise ValueError(f"Protected token changed: {key}: {original!r} -> {result!r}")


def xml_escape(value: str) -> str:
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")


def write_mobile(locale_dir: Path, source: list[dict[str, str]], values: dict[str, str]) -> None:
    locale_dir.mkdir(parents=True, exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for item in source:
        if item["kind"] != "mobile":
            continue
        key = item["id"].split(":", 1)[1]
        lines.append(f'    <string name="{key}">{xml_escape(values[item["id"]])}</string>')
    lines.append("</resources>")
    (locale_dir / "strings.xml").write_text("\n".join(lines) + "\n")
    ET.parse(locale_dir / "strings.xml")


def sql(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def write_migration(path: Path, locale: str, values: dict[str, str]) -> None:
    category: dict[str, dict[str, str]] = {}
    service: dict[str, dict[str, str]] = {}
    question: dict[tuple[str, str], dict[str, str]] = {}
    option: dict[tuple[str, str, str], dict[str, str]] = {}
    for identifier, value in values.items():
        parts = identifier.split(":")
        if parts[0] == "category": category.setdefault(parts[1], {})[parts[2]] = value
        elif parts[0] == "service": service.setdefault(parts[1], {})[parts[2]] = value
        elif parts[0] == "question": question.setdefault((parts[1], parts[2]), {})[parts[3]] = value
        elif parts[0] == "option": option.setdefault((parts[1], parts[2], parts[3]), {})[parts[4]] = value
    lines = [f"-- Generated static catalog translations for {locale}; stable codes and values remain canonical."]
    for code, fields in category.items():
        lines.append("INSERT INTO service_category_translations(category_id,locale,name) "
                     f"SELECT id,{sql(locale)},{sql(fields['name'])} FROM service_categories WHERE code={sql(code)};")
    for code, fields in service.items():
        lines.append("INSERT INTO marketplace_service_translations(service_id,locale,name,short_description,search_keywords) "
                     f"SELECT id,{sql(locale)},{sql(fields['name'])},{sql(fields['shortDescription'])},{sql(fields['searchKeywords'])} "
                     f"FROM marketplace_services WHERE code={sql(code)};")
    for (service_code, key), fields in question.items():
        helper = sql(fields["helperText"]) if "helperText" in fields else "NULL"
        lines.append("INSERT INTO service_question_translations(question_id,locale,prompt,helper_text) "
                     f"SELECT q.id,{sql(locale)},{sql(fields['prompt'])},{helper} FROM service_questions q "
                     f"JOIN marketplace_services s ON s.id=q.service_id WHERE s.code={sql(service_code)} AND q.question_key={sql(key)};")
    for (service_code, key, stable_value), fields in option.items():
        description = sql(fields["description"]) if "description" in fields else "NULL"
        lines.append("INSERT INTO question_option_translations(option_id,locale,label,description) "
                     f"SELECT o.id,{sql(locale)},{sql(fields['label'])},{description} FROM question_options o "
                     "JOIN service_questions q ON q.id=o.question_id JOIN marketplace_services s ON s.id=q.service_id "
                     f"WHERE s.code={sql(service_code)} AND q.question_key={sql(key)} AND o.option_value={sql(stable_value)};")
    path.write_text("\n".join(lines) + "\n")


def property_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "''").replace("\n", "\\n")


def write_messages(path: Path, values: dict[str, str]) -> None:
    lines = []
    for identifier, value in values.items():
        if identifier.startswith("message:"):
            lines.append(f"{identifier.split(':', 1)[1]}={property_escape(value)}")
    path.write_text("\n".join(lines) + "\n")


def main() -> None:
    args = parse_args()
    if args.batch_size < 1 or args.batch_size > 120:
        raise SystemExit("--batch-size must be between 1 and 120")
    if args.workers < 1 or args.workers > 8:
        raise SystemExit("--workers must be between 1 and 8")
    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    if not api_key:
        raise SystemExit("OPENAI_API_KEY is required")
    selected = set(args.locales.split(",")) if args.locales else {code for code, _ in LOCALES}
    locale_items = [(code, name) for code, name in LOCALES if code in selected]
    source = mobile_entries() + catalog_entries(args.psql) + message_entries()
    ids = [item["id"] for item in source]
    if len(ids) != len(set(ids)):
        raise SystemExit("Source contains duplicate translation ids")
    WORK.mkdir(parents=True, exist_ok=True)
    cost_file = WORK / "cost.json"
    total_cost = {"value": json.loads(cost_file.read_text())["cost"] if cost_file.exists() else 0.0}
    cost_lock = Lock()
    generated: dict[str, dict[str, str]] = {}

    def generate_locale(locale: str, language: str) -> tuple[str, dict[str, str]]:
        checkpoint = WORK / f"{locale}.json"
        values = json.loads(checkpoint.read_text()) if checkpoint.exists() else {}
        source_ids = {item["id"] for item in source}
        values = {key: value for key, value in values.items() if key in source_ids}
        for item in source:
            if item["id"] not in values:
                continue
            try:
                validate([item], {item["id"]: values[item["id"]]})
            except ValueError:
                del values[item["id"]]
        pending = [item for item in source if item["id"] not in values]
        for offset in range(0, len(pending), args.batch_size):
            with cost_lock:
                reserve = MAX_CALL_COST_RESERVE_USD * args.workers
                if total_cost["value"] >= BUDGET_USD - reserve:
                    raise RuntimeError(f"Hard budget guard reached at ${total_cost['value']:.4f}")
            batch = pending[offset:offset + args.batch_size]
            translated = None
            for attempt in range(1, 4):
                try:
                    translated, cost = call_openai(api_key, language, batch)
                    validate(batch, translated)
                    break
                except TranslationCallError as error:
                    with cost_lock:
                        total_cost["value"] += error.cost
                        cost_file.write_text(json.dumps({"cost": total_cost["value"]}) + "\n")
                    if attempt == 3:
                        raise
                    print(f"{locale}: rejected malformed output, retrying ({attempt}/3)", flush=True)
                    time.sleep(attempt * 2)
                except ValueError as error:
                    with cost_lock:
                        total_cost["value"] += cost
                        cost_file.write_text(json.dumps({"cost": total_cost["value"]}) + "\n")
                    if attempt == 3:
                        raise
                    print(f"{locale}: rejected invalid output, retrying ({attempt}/3): {error}", flush=True)
                    time.sleep(attempt * 2)
                except (urllib.error.URLError, TimeoutError, RuntimeError) as error:
                    if attempt == 3:
                        raise
                    print(f"{locale}: provider call failed, retrying ({attempt}/3): {error}", flush=True)
                    time.sleep(attempt * 2)
            if translated is None:
                raise RuntimeError(f"No translation result for {locale}")
            expected_batch = {item["id"] for item in batch}
            if set(translated) != expected_batch:
                raise ValueError(f"{locale} batch returned mismatched ids")
            values.update(translated)
            with cost_lock:
                total_cost["value"] += cost
                cost_file.write_text(json.dumps({"cost": total_cost["value"]}) + "\n")
            checkpoint.write_text(json.dumps(values, ensure_ascii=False, indent=2) + "\n")
            print(f"{locale}: {len(values)}/{len(source)} entries; measured cost ${total_cost['value']:.4f}", flush=True)
        validate(source, values)
        return locale, values

    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = [executor.submit(generate_locale, locale, language) for locale, language in locale_items]
        for future in as_completed(futures):
            locale, values = future.result()
            generated[locale] = values
    if total_cost["value"] > BUDGET_USD:
        raise RuntimeError(f"Translation generation exceeded ${BUDGET_USD:.2f}: ${total_cost['value']:.4f}")

    with tempfile.TemporaryDirectory(prefix="arbio-i18n-") as directory:
        staging = Path(directory)
        for index, (locale, _) in enumerate(LOCALES, start=43):
            if locale not in generated:
                continue
            write_mobile(staging / "mobile" / f"values-{locale}", source, generated[locale])
            write_migration(staging / f"V{index}__add_{locale}_catalog_translations.sql", locale, generated[locale])
            write_messages(staging / f"messages_{locale}.properties", generated[locale])
        if args.apply:
            for locale, _ in locale_items:
                target = MOBILE_RESOURCES / f"values-{locale}"
                shutil.rmtree(target, ignore_errors=True)
                shutil.copytree(staging / "mobile" / f"values-{locale}", target)
                index = 43 + [code for code, _ in LOCALES].index(locale)
                shutil.copy2(staging / f"V{index}__add_{locale}_catalog_translations.sql", BACKEND_RESOURCES / "db/migration")
                shutil.copy2(staging / f"messages_{locale}.properties", BACKEND_RESOURCES)
        else:
            preview = WORK / "preview"
            shutil.rmtree(preview, ignore_errors=True)
            shutil.copytree(staging, preview)
    print(f"Validated {len(locale_items)} locales; measured generation cost ${total_cost['value']:.4f}")


if __name__ == "__main__":
    main()
