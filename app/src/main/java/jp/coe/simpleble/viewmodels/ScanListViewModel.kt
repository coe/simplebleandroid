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
import android.os.ParcelUuid
import android.os.Parcelable
import android.util.Log
import jp.coe.simpleble.MainActivity
import java.lang.ref.WeakReference
import java.util.*

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
            Log.d(TAG,"onScanResult:"+callbackType)
            devices.add(result)

            liveDevices.postValue(devices)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)

        }
    }

    override fun onCleared() {
        super.onCleared()
        mApplication.get()?.let {
            val centralManager: BluetoothManager = it.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            centralManager.adapter.bluetoothLeScanner.stopScan(mDeviceScanCallback)
        }
    }

    override fun getData() : LiveData<List<Parcelable>> {
        mApplication.get()?.let {
            val centralManager: BluetoothManager = it.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val uuid = UUID.fromString(MainActivity.SERVICE_UUID)
            val parcelUuid = ParcelUuid(uuid)
            val filter = ScanFilter.Builder()
                    .setServiceUuid(parcelUuid)
                    .build()
            val settings = ScanSettings.Builder()
                    .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build()
            centralManager.adapter.bluetoothLeScanner.startScan(arrayListOf(filter), settings, mDeviceScanCallback)
        }

        return liveDevices
    }

    companion object {
        private val TAG = "ScanListViewModel"
    }
}