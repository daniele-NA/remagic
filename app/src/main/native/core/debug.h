#pragma once

#include <android/log.h>
#define TAG "MY-LOG"

// DEBUG MACROS
#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
