package com.nshd.nurm3

import androidx.compose.ui.unit.Dp

/** Keeps shell-only numeric sizing explicit without leaking UI-unit imports across navigation code. */
internal val Int.dp: Dp get() = Dp(toFloat())
