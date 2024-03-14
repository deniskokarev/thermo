package dkv.thermo.db

class DevicesDb {
    val devices = mutableMapOf<String, Device>()

    val allDevices: Collection<Device>
        get() = devices.values

    fun discover() {
        repeat(10) {
            devices[it.toString()] =
                Device(key = it.toString(), location = "Room", enabled = false)
        }
    }
}
