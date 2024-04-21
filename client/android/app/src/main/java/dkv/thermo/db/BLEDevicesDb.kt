package dkv.thermo.db

import android.Manifest
import kotlinx.coroutines.delay

class BLEDevicesDb : InMemoryDevicesDb() {

    override val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    )

    override suspend fun scan() {
        repeat(10) {
            inMemoryDevices[it.toString()] =
                Device(key = it.toString(), location = "Room", enabled = false)
        }
        delay(5000L)
    }
}