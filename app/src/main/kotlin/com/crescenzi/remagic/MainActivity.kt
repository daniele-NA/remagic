/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.crescenzi.remagic

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crescenzi.remagic.compose.AnimatedNavHost
import com.crescenzi.remagic.core.AndroidVersionManager.isAndroid12OrAbove
import com.crescenzi.remagic.core.LOG
import com.crescenzi.remagic.core.extension.checkStoreUpdate
import com.crescenzi.remagic.external.firebase.NativeFirebase
import com.crescenzi.remagic.external.sensor.NativeSensors
import com.crescenzi.remagic.presentation.AuthPage
import com.crescenzi.remagic.presentation.GamePage
import com.crescenzi.remagic.presentation.auth.AuthScreen
import com.crescenzi.remagic.presentation.game.GameScreen
import com.crescenzi.remagic.presentation.game.GameViewModel
import com.crescenzi.remagic.presentation.menu.MenuViewModel
import com.crescenzi.remagic.system.InternetReceiver
import com.crescenzi.remagic.xml.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class MainActivity : ComponentActivity() {


    // Native binders //
    private val nativeSensors = NativeSensors()
    private val nativeFirebase = NativeFirebase()

    val gameViewModel: GameViewModel by viewModel()
    val menuViewModel: MenuViewModel by viewModel()

    private val internetReceiver = InternetReceiver()

    override fun onStart() {
        super.onStart()
        registerReceiver(internetReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        nativeFirebase.initialize(this)

        // ==== CALL NATIVE METHOD ==== //
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                nativeSensors.startGameSensor { valueX ->
                    gameViewModel.newMoveDirection(valueX)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, e.message.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        checkStoreUpdate()
        isAndroid12OrAbove {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {  // LANDSCAPE MODE
                LOG("HIDDEN SYSTEM BARS WITH API : ${Build.VERSION.SDK_INT}")
                window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }

        setContent {
            AppTheme {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    LOG("NotificationPermission => $isGranted")
                }

                val controller = rememberNavController()
                val authorized by nativeFirebase.isLogged.collectAsStateWithLifecycle()
                LaunchedEffect(authorized) {
                    if (authorized) controller.navigate(GamePage)
                }


                // Richiesta permesso appena entra nello schermo, dopo che la composizione è pronta
                LaunchedEffect(Unit) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                AnimatedNavHost(
                    navController = controller,
                    startDestination = AuthPage
                ) {
                    composable<AuthPage> {
                        AuthScreen{
                            email, pwd, authAction ->
                            nativeFirebase.tryAuthAction(this@MainActivity,email,pwd,authAction)
                        }
                    }
                    composable<GamePage> {
                        GameScreen(gameViewModel, menuViewModel)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(internetReceiver)
        nativeSensors.destroyGameSensor()
    }
}
