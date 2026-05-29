/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.crescenzi.remagic.presentation.game

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.crescenzi.remagic.R
import com.crescenzi.remagic.core.LOG
import com.crescenzi.remagic.core.values.GameConstants.NOTIFICATION_LLM_PROMPT
import com.crescenzi.remagic.core.values.GameConstants.NOTIFICATION_RATE
import com.crescenzi.remagic.device.data.DeviceRepo
import com.crescenzi.remagic.external.llm.NativeLLM
import com.crescenzi.remagic.game.GameState
import com.crescenzi.remagic.game.domain.move.MoveDirection
import com.crescenzi.remagic.game.domain.status.GameStatus
import com.crescenzi.remagic.game.ui.UiState
import com.crescenzi.remagic.system.NotificationManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// == HANDLE GameScreen states == //
class GameViewModel(
    private val deviceRepo: DeviceRepo,
    private val notificationManager: NotificationManager,
    app: Application,
) : AndroidViewModel(app) {
    private val startMediaPlayer by lazy { MediaPlayer.create(app, R.raw.game_start) }
    private val collisionMediaPlayer by lazy { MediaPlayer.create(app, R.raw.game_collision) }
    private val overMediaPlayer by lazy { MediaPlayer.create(app, R.raw.game_over) }

    private var gamesNumber = 0

    private val gameState =
        GameState(scope = viewModelScope, resources = getApplication<Application>().resources)

    fun targetBmp(rawResId: Int) = gameState.getBitmapForTarget(rawResId)

    val uiFlow: StateFlow<UiState> =
        combine(
            gameState._targets,
            gameState._score,
            gameState._moveDirection,
        ) { targets, score, moveDirection ->
            UiState(targets, score, moveDirection)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UiState(emptyList(), 0, MoveDirection.STAND),
        )

    // ==== Separated from others ==== //
    val gameStatusFlow = gameState._gameStatusFlow

    fun start() {
        gameState._gameStatusFlow.value = GameStatus.STARTED
        if (deviceRepo._audioState.value) {
            startMediaPlayer.start()
        }
        gamesNumber++

        // == EACH 'NOTIFICATION_RATE'  TIMES WE SEND A CONGRATULATIONS NOTIFICATION == //
        if (gamesNumber % NOTIFICATION_RATE == 0) {
            viewModelScope.launch {
                try {
                    val body = NativeLLM.getLLMResponse(deviceRepo,NOTIFICATION_LLM_PROMPT)
                    notificationManager.newNotification(
                        application,
                        title = application.getString(R.string.winner_notification_title),
                        body = body
                    )
                }catch (e: Exception){
                    LOG("failed showing notification into GameViewModel => ${e.message}")
                }
            }
        }
    }

    fun gameOver() {
        gameState._gameStatusFlow.value = GameStatus.OVER
        if (deviceRepo._audioState.value) {
            overMediaPlayer.start()
        }
    }

    fun newMoveDirection(value: Float) = gameState.updateMoveDirection(value)

    /**
     * Remove Target,update Score and reproduce sound
     */
    fun collision(
        context: Context,
        index: Int,
    ) {
        gameState.collision(index)
        reproduceSound(context)
    }

    /**
     * If The Audio has been enabled by the user
     */
    private fun reproduceSound(context: Context) {
        if (!deviceRepo._audioState.value) return
        try {
            if (collisionMediaPlayer.isPlaying) {
                collisionMediaPlayer.stop()
                collisionMediaPlayer.prepare()
            }
            collisionMediaPlayer.start()
        } catch (e: Exception) {
            deviceRepo._audioState.value = false
            Toast
                .makeText(
                    context,
                    context.getString(R.string.audio_error),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }
}
