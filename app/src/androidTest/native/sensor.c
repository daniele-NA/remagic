#include <stdio.h>
#include <android/looper.h>
#include <android/sensor.h>
#include <stdlib.h>

#define PRINT_MODE int
#define PRINT_NEUTRAL 0
#define PRINT_RED 1
#define PRINT_BLUE 2


#define KTimeoutMilliSecs 10

void rgbPrint(PRINT_MODE pMode, const char *fmt, ...);

int main(int argc, char *argv[]) {
    printf("\nSTART\n\n");


    ASensorManager *sensorManager = ASensorManager_getInstanceForPackage(NULL);


    int looperId = 0;
    ASensorEventQueue *event_queue = ASensorManager_createEventQueue(sensorManager, ALooper_prepare(
            ALOOPER_PREPARE_ALLOW_NON_CALLBACKS), looperId, NULL, NULL);

    if (event_queue == NULL) {
        printf("Null queue");
    }


    const ASensor *gyro_sensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_GAME_ROTATION_VECTOR);


    ASensorEventQueue_enableSensor(event_queue,gyro_sensor);

    ASensorEventQueue_setEventRate(event_queue, gyro_sensor, 1000000L / 60);

    ASensorEvent event;
    while (1) {
        // poll with short timeout for smoother updates
        ALooper_pollOnce(KTimeoutMilliSecs, NULL, NULL, NULL);

        while (ASensorEventQueue_getEvents(event_queue, &event, 1) > 0) {
            if (event.type == ASENSOR_TYPE_GAME_ROTATION_VECTOR) {
                rgbPrint(PRINT_RED, "x %.2f \n ", event.vector.x);
            }
        }
    }



    return 0;

}

void rgbPrint(PRINT_MODE pMode, const char *fmt, ...) {


    va_list args;
    va_start(args, fmt);


    int necessaryLen = vsnprintf(NULL, 0, fmt, args);

    va_end(args);

    char *buffer = malloc(necessaryLen + 1); //close character \0

    if (!buffer)return;

    va_start(args, fmt);
    vsnprintf(buffer, necessaryLen + 1, fmt, args);
    va_end(args);


    switch (pMode) {
        case PRINT_NEUTRAL:
            printf("%s", buffer);
            break;
        case PRINT_RED:
            printf("\033[1;31m%s\033[0m\n", buffer);
            break;
        case PRINT_BLUE:
            printf("\033[1;34m%s\033[0m\n", buffer);
            break;
    }

    free(buffer);
}