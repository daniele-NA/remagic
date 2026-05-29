/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.crescenzi.remagic.game

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.asImageBitmap
import com.crescenzi.remagic.R
import com.crescenzi.remagic.core.values.GameConstants
import com.crescenzi.remagic.core.values.GameConstants.TARGET_SPEED
import com.crescenzi.remagic.game.domain.move.MoveDirection
import com.crescenzi.remagic.game.domain.status.GameStatus
import com.crescenzi.remagic.game.domain.target.EnemyTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * It handles all the Game Flows
 */
class GameState(scope: CoroutineScope, private val resources: Resources) {

    internal val _gameStatusFlow = MutableStateFlow(GameStatus.IDLE)
    internal val _targets = MutableStateFlow<List<EnemyTarget>>(emptyList())
    internal val _score = MutableStateFlow(0)
    internal val _moveDirection=MutableStateFlow(MoveDirection.STAND)

    /**
     * Raw -> ImageBitmap
     */
    private val fireballWhite = BitmapFactory.decodeResource(resources, R.raw.target_white).asImageBitmap()
    private val fireballOrange = BitmapFactory.decodeResource(resources, R.raw.target_orange).asImageBitmap()
    private val fireballYellow = BitmapFactory.decodeResource(resources, R.raw.target_yellow).asImageBitmap()
    private val fireballPink = BitmapFactory.decodeResource(resources, R.raw.target_pink).asImageBitmap()
    private val fireballRed = BitmapFactory.decodeResource(resources, R.raw.target_red).asImageBitmap()
    private val fireballGreen = BitmapFactory.decodeResource(resources, R.raw.target_green).asImageBitmap()

    private val bitmapMap = mapOf(
        R.raw.target_white to fireballWhite,
        R.raw.target_orange to fireballOrange,
        R.raw.target_yellow to fireballYellow,
        R.raw.target_pink to fireballPink,
        R.raw.target_red to fireballRed,
        R.raw.target_green to fireballGreen,
    )

    // ==== From 0 to screenWidth ==== //
    init {
        scope.launch(Dispatchers.Default) {
            _gameStatusFlow.collect { status ->
                if (status == GameStatus.STARTED) {
                    _score.value=0
                    while (_gameStatusFlow.value == GameStatus.STARTED) {
                        delay(GameConstants.TARGET_SPAWN_RATE)
                        val randomX = (0..resources.displayMetrics.widthPixels).random()
                        var rawResId = R.raw.target_white
                        when {
                            randomX % 2 == 0 -> { rawResId = R.raw.target_white;}
                            randomX % 3 == 0 -> { rawResId = R.raw.target_orange; }
                            randomX % 5 == 0 -> { rawResId = R.raw.target_yellow;}
                            randomX % 7 == 0 -> { rawResId = R.raw.target_pink;}
                            randomX % 9 == 0 -> { rawResId = R.raw.target_red; }
                            randomX % 11 == 0 -> { rawResId = R.raw.target_green; }
                        }

                        val newTarget = EnemyTarget(
                            x = randomX.toFloat(),
                            y = Animatable(0f),
                            fallingSpeed = TARGET_SPEED,
                            rawResId = rawResId
                        )

                        _targets.update { currentList ->
                            (currentList + newTarget).toMutableList()
                        }

                    }
                } else if (status == GameStatus.OVER) {
                    _targets.update {
                        emptyList()
                    }
                }
            }
        }
    }


    fun updateMoveDirection(value: Float) {
        if(_gameStatusFlow.value== GameStatus.STARTED){
            val target = when {
                value > 0f -> MoveDirection.RIGHT
                value < 0f -> MoveDirection.LEFT
                else       -> MoveDirection.STAND
            }

            // ==== Avoid repetition ==== //
            if (_moveDirection.value != target) {
                _moveDirection.value = target
            }
        }
    }



    fun getBitmapForTarget(rawResId: Int) = bitmapMap[rawResId] ?: fireballRed

    /**
     * Remove Target and update Score
     */
    fun collision(index: Int) {
        _targets.update { currentList ->
            if (index in currentList.indices) {
                currentList.toMutableList().apply { removeAt(index) }
            } else {
                currentList
            }
        }
        _score.value=_score.value+5
    }
}