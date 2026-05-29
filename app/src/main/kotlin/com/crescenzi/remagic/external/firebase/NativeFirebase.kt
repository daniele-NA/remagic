package com.crescenzi.remagic.external.firebase

import androidx.activity.ComponentActivity
import com.crescenzi.remagic.core.LOG
import com.google.firebase.FirebaseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Handle native calls //
class NativeFirebase {

    enum class AuthAction {
        SIGN_IN, SIGN_UP
    }

    private val _isLogged = MutableStateFlow(false)
    val isLogged = _isLogged.asStateFlow()

    // == CALLED BY NATIVE SIDE == //
    @Suppress("Unused")
    fun updateLoggedState(state: Boolean){
        _isLogged.update { state }
        LOG("Updated _isLogged with => $state")
    }

    private val scope=CoroutineScope(Dispatchers.IO)

    external fun initialize(activity: ComponentActivity)


    @Throws(FirebaseException::class)
    private external fun auth(email: String, password: String,activity: ComponentActivity,authAction: Int)



    fun tryAuthAction(activity: ComponentActivity,email: String, password: String, authAction: AuthAction) {
        scope.launch {
            try {
                auth(email,password,activity,authAction.ordinal)
            } catch (e: FirebaseException) {
                LOG("FirebaseException => ${e.message}")
            } catch (e: Exception) {
                LOG("Generic Exception => ${e.message}")
            }
        }
    }
}