package com.crescenzi.remagic.external.llm

import com.crescenzi.remagic.device.data.DeviceRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.jvm.Throws

// == LIBCURL HANDLING == //
object NativeLLM {

    private external fun getLLMResponse(prompt:String) : String?


    @Throws(Exception::class)
    suspend fun getLLMResponse(deviceRepo: DeviceRepo, prompt: String): String = withContext(
        Dispatchers.IO) {
        if (!deviceRepo._internetState.value) throw Exception("Internet not available")

        val rawResponse:String = getLLMResponse(prompt) ?: throw Exception("Received 'null' as llm response")

        // CLEAN OUTPUT //
        return@withContext rawResponse
            .replace(Regex("[\\n\\t\\\\/]+"), " ")  // \n \t \\ /
            .replace(Regex("\\s+"), " ")            // spazi multipli
            .trim()
    }

}