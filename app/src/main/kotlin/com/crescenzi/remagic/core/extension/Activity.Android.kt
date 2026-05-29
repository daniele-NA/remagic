/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.crescenzi.remagic.core.extension

import android.app.Activity
import android.content.res.Configuration
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.ViewConfiguration
import com.crescenzi.remagic.core.LOG
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

fun Activity.checkStoreUpdate(requestCode: Int = 123) {
    val appUpdateManager = AppUpdateManagerFactory.create(this)

    // Controlla aggiornamento
    appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
        when {
            info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        AppUpdateType.IMMEDIATE,
                        this,
                        requestCode,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                // Se l’update era stato interrotto
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        AppUpdateType.IMMEDIATE,
                        this,
                        requestCode,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

// ==== RETURNED AS PX ==== //
fun Activity.getNavBarHeight(): Int {
    if (ViewConfiguration.get(this).hasPermanentMenuKey() || KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)) {
        return 0
    }

    val res = resources
    val orientation = res.configuration.orientation
    val isTablet = (res.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    val name =
        if (isTablet) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                "navigation_bar_height"
            } else {
                "navigation_bar_height_landscape"
            }
        } else {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                "navigation_bar_height"
            } else {
                "navigation_bar_width"
            }
        }

    val id = res.getIdentifier(name, "dimen", "android")
    if (id <= 0) return 0

    val px = res.getDimensionPixelSize(id)

    return px
}
