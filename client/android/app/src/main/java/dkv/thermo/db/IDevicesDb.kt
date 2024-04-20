package dkv.thermo.db

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope

interface IDevicesDb {
    val devices: Map<String, Device>
    val liveDevices: LiveData<Map<String, Device>>
    fun startScanning(scope: CoroutineScope): Boolean
}
