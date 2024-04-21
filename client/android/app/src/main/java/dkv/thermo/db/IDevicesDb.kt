package dkv.thermo.db

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope

interface IDevicesDb {
    val devices: Map<String, Device>
    val liveDevices: LiveData<Map<String, Device>>

    /**
     * @param activity - Main Activity to request for necessary permissions
     * @param scope - which scope the scanning function should be executed on
     * @return false if prior scanning is still running
     */
    fun checkPermissionsAndStartScanning(
        activity: Activity,
        scope: CoroutineScope,
    ): Boolean

    /**
     * redirect Activity's onRequestPermissionsResult() here to conclude the
     * user's permissions granting and resume scanning
     */
    fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    )

    /**
     * redirect Activity's onActivityResult() here to conclude the
     * BT adapter activation if it was required
     */
    fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    )
}
