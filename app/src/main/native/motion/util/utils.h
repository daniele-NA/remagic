
#pragma once

#include <android/sensor.h>

/**
 * Print all sensors
 */
void print_all_sensors(ASensorManager *sensorManager);

void try_enable_all_sensors(ASensorManager *sensorManager,ASensorEventQueue *eventQueue);