/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.crescenzi.remagic

import android.app.Application
import com.crescenzi.remagic.device.data.DeviceRepo
import com.crescenzi.remagic.presentation.game.GameViewModel
import com.crescenzi.remagic.presentation.menu.MenuViewModel
import com.crescenzi.remagic.system.NotificationManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Handle starting and DI
 */
class App : Application() {

    init {
        System.loadLibrary("remagic")
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(repoModule, viewModelModule)
        }
    }
}

// ============== DI ============== //

val repoModule =
    module {
        single { DeviceRepo() }
        single { NotificationManager() }
    }

val viewModelModule =
    module {
        viewModel { GameViewModel(deviceRepo = get(), notificationManager = get(), app = get()) }
        viewModel { MenuViewModel(deviceRepo = get()) }
    }
