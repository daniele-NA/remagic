
#include <jni.h>
#include <android/looper.h>
#include <android/sensor.h>
#include "../core/debug.h"

#define KTimeoutMilliSecs 10


static ASensorManager *sensorManager = NULL;
static ASensorEventQueue *event_queue = NULL;
static const ASensor *gyro_sensor = NULL;

_Noreturn JNIEXPORT void JNICALL
Java_com_crescenzi_remagic_external_sensor_NativeSensors_startGameSensor(JNIEnv *env, jobject thiz,
                                                                         jobject sensor_callback) {
    jclass interface_class = (*env)->GetObjectClass(env, sensor_callback);
    jmethodID callback_method_id = (*env)->GetMethodID(env, interface_class, "onEvent", "(F)V");


    sensorManager = ASensorManager_getInstanceForPackage(NULL);

    int looperId = 0;
    event_queue = ASensorManager_createEventQueue(sensorManager, ALooper_prepare(
            ALOOPER_PREPARE_ALLOW_NON_CALLBACKS), looperId, NULL, NULL);

    if (event_queue == NULL) {
        jclass ex_class = (*env)->FindClass(env,
                                            "com/crescenzi/remagic/external/sensor/exception/IllegalNativeSensorException");
        (*env)->ThrowNew(env, ex_class, "Cannot Instantiate event_queue variable");
    }

    gyro_sensor = ASensorManager_getDefaultSensor(sensorManager,
                                                  ASENSOR_TYPE_GAME_ROTATION_VECTOR);
    ASensorEventQueue_enableSensor(event_queue, gyro_sensor);
    // Set rate to ~60 Hz (microseconds)
    ASensorEventQueue_setEventRate(event_queue, gyro_sensor, 1000000L / 60);

    ASensorEvent event;
    while (1) {
        // poll with short timeout for smoother updates
        ALooper_pollOnce(KTimeoutMilliSecs, NULL, NULL, NULL);

        while (ASensorEventQueue_getEvents(event_queue, &event, 1) > 0) {
            if (event.type == ASENSOR_TYPE_GAME_ROTATION_VECTOR) {
                float x = event.vector.x;
                float r = roundf(x * 10.0f) / 10.0f;
                if (fabsf(r) < 0.01f) continue;
                (*env)->CallVoidMethod(env, sensor_callback, callback_method_id, r);
            }
        }

    }


}


JNIEXPORT void JNICALL
Java_com_crescenzi_remagic_external_sensor_NativeSensors_destroyGameSensor(JNIEnv *env,
                                                                           jobject thiz) {
    if (event_queue && gyro_sensor) { // Null check
        ASensorEventQueue_disableSensor(event_queue, gyro_sensor);
    }
    if (event_queue && sensorManager) {
        ASensorManager_destroyEventQueue(sensorManager, event_queue);
    }
    event_queue = NULL;
    sensorManager = NULL;
    gyro_sensor = NULL;

    LOG_E("Destroying sensors");

}