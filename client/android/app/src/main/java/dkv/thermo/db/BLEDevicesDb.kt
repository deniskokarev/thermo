package dkv.thermo.db

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.delay
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BLEDevicesDb : InMemoryDevicesDb() {
    private val TAG = this::class.simpleName

    override val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    )

    private val REQUEST_ENABLE_BT = 2

    // to be fulfilled by onActivityResult()
    private var btEnableContinuation: Continuation<Boolean>? = null

    override fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        val cont = btEnableContinuation
        if (cont != null) {
            if (requestCode == REQUEST_ENABLE_BT) {
                Log.d(TAG, "Bluetooth enabling status ${resultCode == Activity.RESULT_OK}")
                cont.resume(resultCode == Activity.RESULT_OK)
                btEnableContinuation = null
            }
        }
    }

    /**
     * BT may be disabled in which case we're asking user to enable it
     * @return true if BT was finally enabled
     */
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
    private suspend fun waitForBtEnable(activity: Activity, adapter: BluetoothAdapter) =
        suspendCoroutine { cont ->
            if (!adapter.isEnabled) {
                Log.d(TAG, "Asking to enable Bluetooth")
                btEnableContinuation = cont
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                cont.resume(true)
            }
        }

    private inner class ThermoScanCallback(private val adapter: BluetoothAdapter) : ScanCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
        private fun handleOne(result: ScanResult) {
            val device = adapter.getRemoteDevice(result.device.address)
            if (device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
                // fresh result
                val name = result.device.name ?: "NoName"
                val key = result.device.address.toString()
                Log.d(TAG, "name=$name, addr=$key")
                inMemoryDevices[key] =
                    Device(key = "${name}_${key}", location = "Room", enabled = false)
            }
        }

        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                handleOne(result)
            }
        }

    }

    private val environmentalSensingUUID =
        ParcelUuid.fromString("0000181a-0000-1000-8000-00805f9b34fb")

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        ]
    )
    override suspend fun scan(activity: Activity) {
        val adapter = activity.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter != null) {
            if (waitForBtEnable(activity, adapter)) {
                Log.d(TAG, "Start scanning")
                val bleScanner = adapter.bluetoothLeScanner
                val scanFilters = listOf(
                    ScanFilter.Builder()
                        .setServiceUuid(environmentalSensingUUID)
                        .build()
                )
                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setReportDelay(0L)
                    .build()
                val scanCallback = ThermoScanCallback(adapter)
                bleScanner.startScan(scanFilters, scanSettings, scanCallback)
                delay(5000L)
                bleScanner.stopScan(scanCallback)
                Log.d(TAG, "End scanning")
            } else {
                Log.e(TAG, "User didn't allow turning bluetooth ON")
            }
        }
    }
}
