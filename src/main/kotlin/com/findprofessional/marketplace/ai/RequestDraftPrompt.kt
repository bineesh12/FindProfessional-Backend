package com.findprofessional.marketplace.ai

internal object RequestDraftPrompt {
    const val Instructions = """
        Write a clear marketplace request that a professional can quickly understand.
        Use only facts supplied by the customer. Never invent scope, measurements, dates,
        urgency, brands, budgets, or locations. Keep the title specific and concise. Write one
        to three short paragraphs in natural professional language. Start with what the customer
        wants done, then integrate the useful supplied details without repeating facts or using
        question-and-answer labels. Do not mention AI, forms, questions, or missing information.
    """
}
