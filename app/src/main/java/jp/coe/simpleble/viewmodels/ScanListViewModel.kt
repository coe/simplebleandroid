package jp.coe.simpleble.viewmodels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Parcelable
import java.lang.ref.WeakReference

/**
 * Created by tsuyoshihyuga on 2018/04/23.
 */
class ScanListViewModel(application: Application) : AndroidViewModel(application),ScanListViewModelInterface {
    private var mApplication = WeakReference(application)

    private var liveDevices: MutableLiveData<List<Parcelable>> = MutableLiveData()

    private var devices:ArrayList<Parcelable> = ArrayList()

    private val mDeviceScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            devices.add(result)

            liveDevices.postValue(devices)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)

        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    override fun getData() : LiveData<List<Parcelable>> {
        mApplication.get()?.let {
            val centralManager: BluetoothManager = it.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val filter = ScanFilter.Builder()
            val settings = ScanSettings.Builder()
                    .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build()
            centralManager.adapter.bluetoothLeScanner.startScan(arrayListOf(filter.build()), settings, mDeviceScanCallback)
        }

        return liveDevices
    }

}