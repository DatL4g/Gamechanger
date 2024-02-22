package dev.datlag.gamechanger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import com.kmpalette.DominantColorState
import com.kmpalette.rememberDominantColorState
import com.kmpalette.rememberPainterDominantColorState
import com.materialkolor.AnimatedDynamicMaterialTheme
import dev.datlag.gamechanger.LocalDarkMode
import dev.datlag.tooling.compose.launchIO
import dev.datlag.tooling.compose.toLegacyColors
import dev.datlag.tooling.compose.withIOContext
import dev.datlag.tooling.decompose.lifecycle.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

data object SchemeTheme {

    internal val commonSchemeKey = MutableStateFlow<Any?>(null)
    internal val colorState = MutableStateFlow<Map<Any, DominantColorState<Painter>>>(emptyMap())
    internal val itemScheme = MutableStateFlow<Map<Any, Color?>>(emptyMap())

    internal var _state: DominantColorState<Painter>? = null
    internal val state: DominantColorState<Painter>
        get() = _state!!

    fun setCommon(key: Any?) {
        commonSchemeKey.update { key }
    }

    @Composable
    fun update(key: Any?, input: Painter?) {
        if (_state == null || input == null) {
            return
        }

        LaunchedEffect(key, input) {
            suspendUpdate(key, input)
        }
    }

    fun update(key: Any?, input: Painter?, scope: CoroutineScope) {
        scope.launchIO {
            suspendUpdate(key, input)
        }
    }

    suspend fun suspendUpdate(key: Any?, input: Painter?) {
        if (_state == null || key == null ||  input == null) {
            return
        }

        withIOContext {
            val useState = (colorState.firstOrNull() ?: colorState.value)[key] ?: state
            useState.updateFrom(input)

            itemScheme.getAndUpdate {
                it.toMutableMap().apply {
                    put(key, useState.color)
                }
            }
        }
    }
}

@Composable
fun rememberSchemeThemeDominantColor(
    key: Any?
): Color? {
    if (SchemeTheme._state == null) {
        SchemeTheme._state = rememberPainterDominantColorState()
    }

    val color by remember(key) {
        SchemeTheme.itemScheme.map { it[key] }
    }.collectAsStateWithLifecycle(SchemeTheme.itemScheme.value[key])

    return color
}

@Composable
fun rememberSchemeThemeDominantColorState(
    key: Any?,
    defaultColor: Color = MaterialTheme.colorScheme.primary,
    defaultOnColor: Color = MaterialTheme.colorScheme.onPrimary,
    clearFilter: Boolean = false,
    applyMinContrast: Boolean = false,
    minContrastBackgroundColor: Color = Color.Transparent
): DominantColorState<Painter> {
    val state by remember(key) {
        SchemeTheme.colorState.map { it[key] }
    }.collectAsStateWithLifecycle(SchemeTheme.colorState.value[key])

    return state ?: rememberPainterDominantColorState(
        defaultColor = defaultColor,
        defaultOnColor = defaultOnColor,
        builder = {
            if (clearFilter) {
                clearFilters()
            }
        },
        isSwatchValid = { swatch ->
            if (applyMinContrast) {
                Color(swatch.bodyTextColor).contrastAgainst(minContrastBackgroundColor) >= MinContrastRatio
            } else {
                true
            }
        }
    ).also {
        if (key != null) {
            SchemeTheme.colorState.update { map ->
                map.toMutableMap().apply {
                    put(key, it)
                }
            }
        }
    }
}

@Composable
fun SchemeTheme(key: Any?, content: @Composable () -> Unit) {
    AnimatedDynamicMaterialTheme(
        seedColor = rememberSchemeThemeDominantColor(key) ?: MaterialTheme.colorScheme.primary,
        useDarkTheme = LocalDarkMode.current
    ) {
        androidx.compose.material.MaterialTheme(
            colors = MaterialTheme.colorScheme.toLegacyColors(LocalDarkMode.current)
        ) {
            content()
        }
    }
}

@Composable
fun CommonSchemeTheme(content: @Composable () -> Unit) {
    val key by SchemeTheme.commonSchemeKey.collectAsStateWithLifecycle()

    SchemeTheme(key, content)
}

@Composable
expect fun SchemeThemeSystemProvider(scheme: ColorScheme, content: @Composable () -> Unit)

private fun Color.contrastAgainst(background: Color): Float {
    val fg = if (alpha < 1f) compositeOver(background) else this

    val fgLuminance = fg.luminance() + 0.05f
    val bgLuminance = background.luminance() + 0.05f

    return maxOf(fgLuminance, bgLuminance) / minOf(fgLuminance, bgLuminance)
}

private const val MinContrastRatio = 3f