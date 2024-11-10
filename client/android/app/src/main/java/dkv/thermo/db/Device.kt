package dkv.thermo.db

import kotlin.random.Random

open class Device(val key: String, var location: String, var enabled: Boolean) {
    open suspend fun getTempC(): Double {
        return Random.nextDouble(0.0, 40.0)
    }

    open suspend fun getHumidity(): Double {
        return Random.nextDouble(0.0, 1.0)
    }
}
