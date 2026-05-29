/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.crescenzi.remagic.xml

import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.crescenzi.remagic.R

/**
 * Configurati allo stesso modo
 */
private fun getColorScheme(context: Context): ColorScheme {

    return darkColorScheme().copy(
        primary = Color(ContextCompat.getColor(context, R.color.md_theme_primary)),
        onPrimary = Color(ContextCompat.getColor(context, R.color.md_theme_onPrimary)),

        background = Color(ContextCompat.getColor(context, R.color.md_theme_background)),
        onBackground = Color(ContextCompat.getColor(context, R.color.md_theme_onBackground)),

        surface = Color(ContextCompat.getColor(context, R.color.md_theme_surface)),
        onSurface = Color(ContextCompat.getColor(context, R.color.md_theme_onSurface)),

        error = Color(ContextCompat.getColor(context, R.color.md_theme_error)),
        onError = Color(ContextCompat.getColor(context, R.color.md_theme_onError)),
    )



}


/**
 * Gestione dei colori del Tema via Xml,i colori vengono tutti dall'XML,
 * si occupa lui di fare colors -> colors-night
 */
@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    /*
    Va inizializzato per primo per non avere problemi di colori in Type.kt
     */
    val customColorScheme = getColorScheme(LocalContext.current)

    MaterialTheme(
        colorScheme = customColorScheme,
        typography = getTypography(LocalContext.current),
        content = content
    )
}
