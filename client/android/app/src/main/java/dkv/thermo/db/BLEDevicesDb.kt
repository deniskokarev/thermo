package dkv.thermo.db

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
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

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT])
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "name=${result?.device?.name}, addr=${result?.device?.address}")
        }
    }

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
                bleScanner.startScan(scanCallback)
                delay(3000L)
                bleScanner.stopScan(scanCallback)
                Log.d(TAG, "End scanning")
            } else {
                Log.e(TAG, "User didn't allow turning bluetooth ON")
            }
        }
    }
}
