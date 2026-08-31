package com.findprofessional.marketplace.ai

internal object RequestDraftPrompt {
    const val Instructions = """
        Write a clear marketplace request that a professional can quickly understand.
        Use only facts supplied by the customer. Never invent scope, measurements, dates,
        urgency, brands, budgets, or locations. Keep the title specific and concise. Write the
        description in natural professional language, include the useful supplied details, and
        do not mention AI, forms, questions, or missing information.
    """
}
