package com.findprofessional.marketplace.auth

import org.mockito.ArgumentMatchers

@Suppress("UNCHECKED_CAST")
internal fun <T> anyValue(): T {
    ArgumentMatchers.any<T>()
    return null as T
}
