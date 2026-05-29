#include <jni.h>
#include "../api/regolo.h"
#include "../api/arcfour/arcfour.h"
#include "../core/debug.h"


JNIEXPORT jstring JNICALL
Java_com_crescenzi_remagic_external_llm_NativeLLM_getLLMResponse(JNIEnv *env, jobject thiz,jstring jprompt) {
    const char *prompt = (*env)->GetStringUTFChars(env, jprompt, NULL);

    // == retrieve key == //
    struct s_arcfour *arc;


    int16 cipher_len = strlen(REGOLO_API_KEY) / 2;

    // Convert HEX -> byte array
    string8 cipher_to_convert[cipher_len + 1];   // +1 per terminatore
    hex_to_bin(REGOLO_API_KEY, cipher_to_convert, cipher_len);
    cipher_to_convert[cipher_len] = '\0';        // terminatore stringa per printf

    // Decrypt
    arc = arcfour_init(ARCFOUR_SECRET_KEY, strlen((char*)ARCFOUR_SECRET_KEY));
    arcfour_decrypt(cipher_to_convert, cipher_len, arc);
    arcfour_destroy(arc);

    LOG_E("Decrypted regolo_key: %s\n", cipher_to_convert);

    char* response = llm((char*)prompt,(char *) cipher_to_convert);

    if(response!=NULL){
        (*env)->ReleaseStringUTFChars(env, jprompt, prompt);


        jstring jresponse = (*env)->NewStringUTF(env, response);
        free(response);

        return jresponse;
    }

    return NULL;

}