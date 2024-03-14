package dkv.thermo.db

import kotlin.random.Random

data class Device(val key: String, var location: String, var enabled: Boolean) {
    val tempC: Double
        get() {
            return Random.nextDouble(0.0, 40.0)
        }
    val humid: Double
        get() {
            return Random.nextDouble(0.0, 1.0)
        }
}
