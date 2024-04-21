package dkv.thermo.db

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class InMemoryDevicesDb : IDevicesDb {
    private val TAG = this::class.simpleName
    protected val inMemoryDevices = mutableMapOf<String, Device>()
    override val devices
        get() = inMemoryDevices

    override val liveDevices = MutableLiveData<Map<String, Device>>()

    protected open val permissions: Array<String> = arrayOf()

    protected open suspend fun scan() {
        repeat(10) {
            inMemoryDevices[it.toString()] =
                Device(key = it.toString(), location = "Room", enabled = false)
        }
        delay(5000L)
    }

    // to make sure we scan once at a time
    private val stillScanning = AtomicInteger(0)
    override fun checkPermissionsAndStartScanning(
        activity: Activity,
        scope: CoroutineScope,
    ): Boolean {
        if (stillScanning.addAndGet(1) == 1) {
            scope.launch(Dispatchers.IO) {
                checkAndScan(activity)
            }.invokeOnCompletion {
                stillScanning.addAndGet(-1)
            }
            return true
        } else {
            return false
        }
    }

    private var permissionsContinuation: Continuation<Boolean>? = null
    override fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        val cont = permissionsContinuation
        if (requestCode == REQUEST_BT_PERMISSIONS && cont != null) {
            if (validatePermissions(activity)) {
                cont.resume(true)
            } else {
                cont.resume(false)
            }
            permissionsContinuation = null
        }
    }

    private val REQUEST_BT_PERMISSIONS = 1
    private fun validatePermissions(activity: Activity) = permissions.all {
        ActivityCompat.checkSelfPermission(
            activity,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun waitForPermissions(): Boolean =
        suspendCoroutine { cont -> permissionsContinuation = cont }

    private suspend fun checkAndScan(activity: Activity) {
        if (!validatePermissions(activity)) {
            Log.d(TAG, "Asking for permissions...")
            ActivityCompat.requestPermissions(
                activity,
                permissions,
                REQUEST_BT_PERMISSIONS
            )
            if (!waitForPermissions()) {
                Log.e(TAG, "User didn't grant permissions")
                return
            }
        }
        Log.d(TAG, "Permissions are good, scanning for devices...")
        scan()
        Log.d(TAG, "Scanning complete")
        withContext(Dispatchers.Main) {
            liveDevices.value = inMemoryDevices
        }
    }
}
