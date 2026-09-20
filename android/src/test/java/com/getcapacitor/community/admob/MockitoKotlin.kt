package com.getcapacitor.community.admob

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers

// Mockito's matchers return null, which Kotlin refuses where a parameter is declared non-null. These register
// the same matcher and hand back a value that is only null at runtime.

@Suppress("UNCHECKED_CAST")
internal fun <T> anyK(): T {
    ArgumentMatchers.any<T>()
    return null as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> ArgumentCaptor<T>.captureK(): T {
    capture()
    return null as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> eqK(value: T): T {
    ArgumentMatchers.eq(value)
    return null as T
}
