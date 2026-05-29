/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.crescenzi.remagic.xml

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.crescenzi.remagic.R

val titlesFont = Font(R.font.cinzel_regular)
val bodiesFont = Font(R.font.cinzel_bold)

fun getTypography(context: Context): Typography {
    val h1 = (context.resources.getDimension(R.dimen.h1) / context.resources.displayMetrics.scaledDensity).sp
    val h2 = (context.resources.getDimension(R.dimen.h2) / context.resources.displayMetrics.scaledDensity).sp

    return Typography(
        // ==== TITLES ==== //
        titleMedium =
            TextStyle(
                fontFamily = FontFamily(titlesFont),
                fontWeight = FontWeight.Bold,
                fontSize = h1,
                lineHeight = 60.sp,
                letterSpacing = 0.sp,
            ),
        // ==== TEXTS ==== //
        labelMedium =
            TextStyle(
                fontFamily = FontFamily(bodiesFont),
                fontWeight = FontWeight.Normal,
                fontSize = h2,
                lineHeight = 25.sp,
                letterSpacing = 0.25.sp,
            ),
        // ==== BUTTONS ==== //
        bodyMedium =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = h2,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
    )
}
