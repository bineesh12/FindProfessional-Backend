# Arbio localization generation

The generator reads only checked-in English mobile resources, backend notification templates,
and static catalog/questionnaire rows from the local development database. It never reads user
accounts, requests, messages, offers, or other customer data.

```bash
export OPENAI_API_KEY="..."
export PSQL="/path/to/psql"
python3 backend/tools/localization/generate_translations.py --apply
```

Output is checkpointed below `.local/localization-generation`, validated there first, and promoted
only after a complete locale passes key, placeholder, newline, XML, and stable-value checks. The
tool uses `gpt-6-luna`, sends `store: false`, and stops before its one-dollar generation ceiling.

Machine translations are drafts. Authentication, privacy, payment, verification, and legal-facing
copy require human review before all locales are enabled in production.
