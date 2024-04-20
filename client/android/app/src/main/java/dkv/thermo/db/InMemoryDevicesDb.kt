package dkv.thermo.db

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

open class InMemoryDevicesDb : IDevicesDb {
    private val inMemoryDevices = mutableMapOf<String, Device>()
    override val devices
        get() = inMemoryDevices

    override val liveDevices = MutableLiveData<Map<String, Device>>()

    private val stillScanning = AtomicInteger(0)
    protected open suspend fun scan() {
        repeat(10) {
            inMemoryDevices[it.toString()] =
                Device(key = it.toString(), location = "Room", enabled = false)
        }
        delay(5000L)
    }

    override fun startScanning(scope: CoroutineScope): Boolean {
        if (stillScanning.addAndGet(1) == 1) {
            scope.launch(Dispatchers.IO) {
                scan()
                withContext(Dispatchers.Main) {
                    liveDevices.value = inMemoryDevices
                }
            }.invokeOnCompletion {
                stillScanning.addAndGet(-1)
            }
            return true
        } else {
            return false
        }
    }
}
