package jp.coe.simpleble.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import java.util.*

/**
 * Created by tsuyoshihyuga on 2018/04/23.
 */
class ScanListMockViewModel : ViewModel(),ScanListViewModelInterface {

    private var liveDevices: MutableLiveData<MutableMap<String,Parcelable>> = MutableLiveData()
    private var devicesMap = mutableMapOf<String,Parcelable>()


    private var mTimer: Timer? = null

    override fun getData(): LiveData<MutableMap<String,Parcelable>> {
        Log.d(TAG,"getData")
        mTimer = Timer()
        val timertask = object : TimerTask() {
            override fun run() {
                val vundle = Bundle()

                vundle.putString("param1",Date().toString())
                Log.d(TAG,"param1:"+Date().toString())

                devicesMap.put(Date().toString(),vundle)

                liveDevices.postValue(devicesMap)
            }
        }
        mTimer?.scheduleAtFixedRate(timertask, Date(), 1000)
        return liveDevices
    }

    override fun onCleared() {
        Log.d(TAG,"onCleared")
        super.onCleared()
        mTimer?.cancel()

    }

    companion object {

        const val ARG_COLUMN_COUNT = "column-count"
        const val TAG = "ScanListMockViewModel"

    }
}