package com.crescenzi.remagic.core


import android.os.Build

/**
 * Version checker
 */
object AndroidVersionManager {


    // == minore o uguale ad Android 10 == //
    inline fun <T> isAndroid10OrBelow(onSuccess: () -> T): T? {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Q = 29 (Android 10)
            onSuccess()
        } else null
    }

    // == maggiore o uguale ad Android 11 == //
    inline fun <T> isAndroid11OrAbove(onSuccess: () -> T): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // R = 30 (Android 11)
            onSuccess()
        } else null
    }

    inline fun <T> isAndroid12OrAbove(onSuccess: () -> T): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            onSuccess()
        } else null
    }
    inline fun <T> isAndroid13OrAbove(onSuccess: () -> T): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onSuccess()
        } else null
    }
    inline fun <T> isAndroid14OrAbove(onSuccess: () -> T): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            onSuccess()
        } else null
    }
    inline fun <T> isAndroid15OrAbove(onSuccess: () -> T): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            onSuccess()
        } else null
    }

}