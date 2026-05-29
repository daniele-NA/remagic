/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.crescenzi.remagic.presentation.game

import android.graphics.BitmapFactory
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.graphics.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crescenzi.remagic.R
import com.crescenzi.remagic.core.extension.getNavBarHeight
import com.crescenzi.remagic.core.values.GameConstants
import com.crescenzi.remagic.core.values.GameConstants.MAGICIAN_FRAME_SIDE
import com.crescenzi.remagic.game.domain.move.MoveDirection
import com.crescenzi.remagic.game.domain.status.GameStatus
import com.crescenzi.remagic.presentation.menu.MenuViewModel
import com.crescenzi.remagic.presentation.menu.idle.IdleScreen
import com.crescenzi.remagic.presentation.menu.over.OverScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun GameScreen(gameViewModel: GameViewModel, menuViewModel: MenuViewModel) {

    val context = LocalContext.current
    val resources = LocalResources.current
    val uiState by gameViewModel.uiFlow.collectAsStateWithLifecycle()
    val gameStatus by gameViewModel.gameStatusFlow.collectAsStateWithLifecycle()
    var screenWidth by remember { mutableIntStateOf(0) }
    var screenHeight by remember { mutableIntStateOf(0) }
    val magicianOffsetX =
        remember(key1 = screenWidth) { Animatable(initialValue = ((screenWidth.toFloat()) / 2 - (MAGICIAN_FRAME_SIDE / 2))) }
    val bgPainter = remember {
        BitmapPainter(
            BitmapFactory.decodeResource(resources, R.raw.game_bg).asImageBitmap()
        )
    }
    val navBarThickness = LocalActivity.current?.getNavBarHeight() ?: 0

    // Memoizza le bitmap per evitare decode ripetuti ad ogni recomposition
    val magicianLeftBmp = remember(resources) {
        BitmapFactory.decodeResource(resources, R.raw.magician_left)
            .scale(MAGICIAN_FRAME_SIDE, MAGICIAN_FRAME_SIDE)
            .asImageBitmap()
    }

    val magicianStandBmp = remember(resources) {
        BitmapFactory.decodeResource(resources, R.raw.magician_stand)
            .scale(MAGICIAN_FRAME_SIDE, MAGICIAN_FRAME_SIDE)
            .asImageBitmap()
    }

    val magicianRightBmp = remember(resources) {
        BitmapFactory.decodeResource(resources, R.raw.magician_right)
            .scale(MAGICIAN_FRAME_SIDE, MAGICIAN_FRAME_SIDE)
            .asImageBitmap()
    }

    val magicianBmp = remember(uiState.moveDirection) {
        listOf(
            magicianLeftBmp, magicianStandBmp, magicianRightBmp
        )[uiState.moveDirection.ordinal]
    }


    val magicianY = (screenHeight - magicianBmp.height).toFloat()


    // Move the magician whit sensor
    LaunchedEffect(
        uiState
            .moveDirection
    ) {
        if (uiState.moveDirection == MoveDirection.LEFT) {
            while (uiState.moveDirection == MoveDirection.LEFT) {
                magicianOffsetX.animateTo(
                    targetValue = if ((magicianOffsetX.value - GameConstants.MAGICIAN_SPEED) >= 0f)
                        magicianOffsetX.value - GameConstants.MAGICIAN_SPEED else magicianOffsetX.value,
                    animationSpec = tween(30)
                )
            }

        } else if (uiState.moveDirection == MoveDirection.RIGHT) {
            while (uiState.moveDirection == MoveDirection.RIGHT) {
                magicianOffsetX.animateTo(
                    // ==== MINUS navBarThickness in order to avoid the magician under SystemBar ==== //
                    targetValue = if ((magicianOffsetX.value + GameConstants.MAGICIAN_SPEED + MAGICIAN_FRAME_SIDE) <= screenWidth.toFloat() - navBarThickness)
                        magicianOffsetX.value + GameConstants.MAGICIAN_SPEED else magicianOffsetX.value,
                    animationSpec = tween(30)
                )
            }


        }
    }


    // ====== COLLISION ====== //
    LaunchedEffect(gameStatus) {
        while (gameStatus == GameStatus.STARTED) {
            delay(16)
            val targets = uiState.targets

            for (t in targets) {
                launch(Dispatchers.Main) {
                    t.y.animateTo(t.y.value + t.fallingSpeed, animationSpec = tween(0))
                }
            }

            // compute magician center for collision
            val magicianCenterX = magicianOffsetX.value + MAGICIAN_FRAME_SIDE / 2f
            val magicianYTop = (screenHeight - MAGICIAN_FRAME_SIDE - (MAGICIAN_FRAME_SIDE / 2))
            val magicianCenterY = magicianYTop + MAGICIAN_FRAME_SIDE / 2f
            val magicianRadius = (MAGICIAN_FRAME_SIDE.coerceAtMost(MAGICIAN_FRAME_SIDE) / 2f)

            // check collisions (iterate backwards to safely remove)
            for (i in targets.size - 1 downTo 0) {
                val target = targets[i]
                val dx = magicianCenterX - target.x
                val dy = magicianCenterY - target.y.value

                val verticalTolerance = MAGICIAN_FRAME_SIDE * 0.7f
                val distance = sqrt(dx * dx + dy * dy)

                if (distance < (magicianRadius + verticalTolerance)) {
                    gameViewModel.collision(context, i)
                }
            }


            // ==== If any target went off screen -> Game Over ==== //
            val offScreenTarget = targets.firstOrNull {
                it.y.value > screenHeight
            }
            if (offScreenTarget != null) {
                gameViewModel.gameOver()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                screenWidth = it.size.width
                screenHeight = it.size.height
            }
    ) {


        Image(
            modifier = Modifier.fillMaxSize(),
            painter = bgPainter,
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ==== draw targets ==== //
            uiState.targets.forEach { target ->
                val fireball = gameViewModel.targetBmp(target.rawResId)

                // === Start drawing from left to right === //
                drawImage(
                    image = fireball,
                    topLeft = Offset(
                        target.x.coerceAtMost((screenWidth - navBarThickness * 2).toFloat()),
                        target.y.value
                    )
                )
            }
            // ==== draw magician ==== //
            drawImage(
                image = magicianBmp,
                topLeft = Offset(
                    x = magicianOffsetX.value,
                    y = magicianY
                )
            )

        }


        GameHeader(uiState.score)

        if (gameStatus == GameStatus.IDLE) IdleScreen(
            menuViewModel = menuViewModel,
            onStart = gameViewModel::start
        )

        if (gameStatus == GameStatus.OVER) OverScreen(
            menuViewModel = menuViewModel,
            onStart = gameViewModel::start,
            score = uiState.score
        )

    }
}