package io.getstream.chat.android.compose.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun <K1, T> rememberLambda(key: K1, lambda: () -> T) = remember(key1 = key) {
    lambda
}

@Composable
internal fun <K1, P1, T> rememberLambda(key: K1, lambda: (P1) -> T) = remember(key1 = key) {
    lambda
}

@Composable
internal fun <K1, P1, P2, T> rememberLambda(key: K1, lambda: (p1: P1, p2: P2) -> T) = remember(key1 = key) {
    lambda
}

