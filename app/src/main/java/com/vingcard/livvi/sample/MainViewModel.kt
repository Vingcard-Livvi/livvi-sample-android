package com.vingcard.livvi.sample

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vingcard.livvi.sample.lk.LKScanner
import com.vingcard.livvi.sample.lk.LKScanResult
import com.vingcard.livvi.sample.lk.LKScannerProtocol
import com.vingcard.livvi.sample.lk.LKUnlockDeviceInteractor
import com.vingcard.livvi.sample.lk.LKUnlockResult
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainViewModel(private val _scanner: LKScanner): ViewModel(), LKScannerProtocol
{
    val renderedDevices: SnapshotStateList<LKScanResult> =  mutableStateListOf()
    val isLoading = mutableStateOf(false)
    val unlockResult = mutableStateOf<LKUnlockResult?>(null)

    private lateinit var _unlockInteractor: LKUnlockDeviceInteractor

    init {
        TODO("Insert your Livvi lock serial in base32 format and user key below.")
    }
    // Livvi lock properties
    // You should fill in the serial and key for the lock you want to unlock
    // In this example code, you can only fill the data for one lock (either Livvi or TTLock)
    val serialBase32: String? = ""
    val userKey: String? = ""

    // TTLock lock properties (only needed to unlock TTLock devices)
    val lockData: String? = null
    val lockMac: String? = null

    override fun didUpdateVisible(visibleDevices: List<LKScanResult>)
    {
        viewModelScope.launch {
            renderedDevices.clear()
            renderedDevices.addAll(visibleDevices)
        }
    }

    fun unlockTapped(device: LKScanResult) {
        if (serialBase32 != null && lockMac != null) {
            Log.e("UNLOCK", "You should only fill in the data for one lock (either Livvi or TTLock)")
            exitProcess(1)
        }
        if ((serialBase32 != null && device.serial != serialBase32)
            || (lockMac != null && device.macAddress != lockMac)) {
            unlockResult.value = LKUnlockResult.permissionError
            Log.d("UNLOCK", "Unlock requested to unknown device")
            return
        }

        isLoading.value = true
        unlockResult.value = null

        // This is the code to unlock the lock.
        device.userKey = userKey
        device.lockData = lockData
        _unlockInteractor.unlock(device) { result ->
            viewModelScope.launch {
                isLoading.value = false
                unlockResult.value = result
            }
        }
    }

    fun clearUnlockResult() {
        unlockResult.value = null
    }

    fun onPermissionsGranted()
    {
        this._unlockInteractor = LKUnlockDeviceInteractor(_scanner)
        _scanner.subscribe(this)
    }
}
