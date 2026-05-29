package com.crescenzi.remagic.external.sensor

import com.crescenzi.remagic.external.sensor.exception.IllegalNativeSensorException
import dalvik.annotation.optimization.FastNative

// Native binding
class NativeSensors{

    @Throws(IllegalNativeSensorException::class)
    external fun startGameSensor(sensorCallback: SensorCallback)

    @FastNative
    external fun destroyGameSensor()

}