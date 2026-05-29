#include <jni.h>
#include "../firebase/firebase_wrapper.hpp"

// == HANDLE ALL THE AUTH LOGIC == //

static JavaVM *g_vm = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm,void *_){
    LOG_E("JNI INITIALIZED");
    g_vm=vm;
    return JNI_VERSION_1_6;
}

extern "C" {

// == Notify kotlin flown so it can be switch between screens  (FIXME) == //
void notifyKotlinFlow(JNIEnv *env, jobject jnativefirebaseinstance, bool result) {
    jclass clazz = env->GetObjectClass(jnativefirebaseinstance);
    if (!clazz) return;
    jmethodID methodID = env->GetMethodID(clazz, "updateLoggedState", "(Z)V");
    if (!methodID) return;

    // call the method with true/false
    env->CallVoidMethod(jnativefirebaseinstance, methodID, result);
}

JNIEXPORT void JNICALL Java_com_crescenzi_remagic_external_firebase_NativeFirebase_initialize(JNIEnv *env, jobject jnativefirebaseintance,
                                                                                              jobject activity) {

    // ==  WE INSTANTIATE THIS BECAUSE IT WILL TRIGGER THE LISTENER AND CHECK IF WE'RE LOGGED IN OR NOT == //

    auto firebaseInstance = env->NewGlobalRef(jnativefirebaseintance); // crea ref globale
    firebase_wrapper inst = firebase_wrapper::instance(env, activity,
                                                       [firebaseInstance](bool result) {
       bool didAttach = false;
       JNIEnv* env = nullptr;

       if (g_vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
           if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
           didAttach = true;
       }

       // == TRIGGER THE LISTENER == //
       notifyKotlinFlow(env, firebaseInstance, result);

       if (didAttach) g_vm->DetachCurrentThread();});
}

JNIEXPORT void JNICALL
Java_com_crescenzi_remagic_external_firebase_NativeFirebase_auth(JNIEnv *env,
                                                                 jobject jnativefirebaseintance,
                                                                 jstring jemail,
                                                                 jstring jpassword,
                                                                 jobject activity, jint action) {

    // Safely get UTF chars and copy to std::string
    const char* constant_email = jemail ? env->GetStringUTFChars(jemail, nullptr) : nullptr;
    const char* constant_pwd = jpassword ? env->GetStringUTFChars(jpassword, nullptr) : nullptr;
    char * email_normal = strdup(constant_email);
    char * pwd_normal = strdup(constant_pwd);
    std::string email = constant_email ? std::string(constant_email) : std::string();
    std::string password = constant_pwd ? std::string(constant_pwd) : std::string();
    if (constant_email) env->ReleaseStringUTFChars(jemail, constant_email);
    if (constant_pwd) env->ReleaseStringUTFChars(jpassword, constant_pwd);

    auto jniEnv = env;
    auto firebaseInstance = env->NewGlobalRef(jnativefirebaseintance); // crea ref globale

    firebase_wrapper inst = firebase_wrapper::instance(env, activity,
                                                       [firebaseInstance](bool result) {
       bool didAttach = false;
       JNIEnv* env = nullptr;

       if (g_vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
           if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
           didAttach = true;
       }
       notifyKotlinFlow(env, firebaseInstance, result);


       if (didAttach) g_vm->DetachCurrentThread();});

    switch (action) {
        case 0:inst.sign_in(email_normal, pwd_normal);break;
        case 1:inst.sign_up(email_normal, pwd_normal);break;
        default:LOG_E("Invalid auth action (default case) ");break;
    }

}}
