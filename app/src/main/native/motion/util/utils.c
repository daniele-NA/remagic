
#include "utils.h"
#include "../../core/debug.h"


void print_all_sensors(ASensorManager *sensorManager) {

    ASensorList sensorGroup;
    int numSensors = ASensorManager_getSensorList(sensorManager, &sensorGroup);

    for (int i = 0; i < numSensors; ++i) {
        LOG_I("SensorName %s\n", ASensor_getName(sensorGroup[i]));
    }
}

void try_enable_all_sensors(ASensorManager *sensorManager,ASensorEventQueue *eventQueue) {
    ASensorList sensorGroup;
    int numSensors = ASensorManager_getSensorList(sensorManager, &sensorGroup);

    for (int count = 0; count < numSensors; count++) {
        if (ASensorEventQueue_enableSensor(eventQueue, sensorGroup[count]) != 0)
            LOG_E("Error with sensor %s\n", ASensor_getName(sensorGroup[count]));
    }
}