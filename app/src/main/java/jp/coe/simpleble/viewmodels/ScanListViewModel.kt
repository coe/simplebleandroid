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

    private var liveDevices: MutableLiveData<MutableMap<String,Parcelable>> = MutableLiveData()

//    private var devices:ArrayList<Parcelable> = ArrayList()
    private var devicesMap = mutableMapOf<String,Parcelable>()

    private val mDeviceScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG,"onScanResult:"+callbackType)
            if (!devicesMap.containsKey(result.device.address)) {
                devicesMap.put(result.device.address,result)
                liveDevices.postValue(devicesMap)
            }

        }

        override fun onScanFailed(errorCode: Int) {
            Log.d(TAG,"onScanFailed:"+errorCode)
            super.onScanFailed(errorCode)

        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d(TAG,"onBatchScanResults:"+results?.size)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mApplication.get()?.let {
            val centralManager: BluetoothManager = it.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            centralManager.adapter.bluetoothLeScanner.stopScan(mDeviceScanCallback)
        }
    }

    override fun getData() : LiveData<MutableMap<String,Parcelable>> {
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
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build()
            //取れない場合がある(Bluetooth OFFとか)
            val leScanner = centralManager.adapter.bluetoothLeScanner
            leScanner.startScan(arrayListOf(filter), settings, mDeviceScanCallback)
        }

        return liveDevices
    }

    companion object {
        private val TAG = "ScanListViewModel"
    }
}