/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.crescenzi.remagic.presentation.menu

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crescenzi.remagic.R

/**
 * It Wraps the @Composable content passed in
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuWrapper(
    modifier: Modifier = Modifier,
    menuViewModel: MenuViewModel,
    onClickButton: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val bg = remember {
        BitmapFactory.decodeResource(resources, R.raw.menu_bg)
    }

    val audioState by menuViewModel.audioState.collectAsStateWithLifecycle()
    val internetState by menuViewModel.internetState.collectAsStateWithLifecycle()

    val baseHorizontalPadding = dimensionResource(R.dimen.menu_base_horizontal_padding)
    val baseVerticalPadding = dimensionResource(R.dimen.menu_base_vertical_padding)
    val rowHorizontalPadding = dimensionResource(R.dimen.menu_row_horizontal_padding)
    val rowVerticalPadding = dimensionResource(R.dimen.menu_row_vertical_padding)
    val buttonBottomPadding = dimensionResource(R.dimen.menu_btn_bottom_padding)
    val btnTextSize = dimensionResource(R.dimen.menu_btn_text_size)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(vertical = baseVerticalPadding, horizontal = baseHorizontalPadding)
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = BitmapPainter(bg.asImageBitmap()),
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = rowHorizontalPadding, vertical = rowVerticalPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Image(
                    modifier = Modifier.clickable { menuViewModel.toggleAudio() },
                    painter = if (audioState) painterResource(R.drawable.volume_on)
                    else painterResource(R.drawable.volume_off),
                    contentDescription = null,
                )

                Image(
                    painter = if (internetState) painterResource(R.drawable.wifi_on_icon)
                    else painterResource(R.drawable.wifi_off_icon),
                    contentDescription = null,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }

            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.BottomEnd
            ) {
                TextButton(
                    modifier = Modifier.padding(bottom = buttonBottomPadding),
                    onClick = { onClickButton() }
                ) {
                    Text(
                        text = stringResource(R.string.menu_btn),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = btnTextSize.value.sp
                        )
                    )
                }
            }
        }
    }
}
