package jp.coe.simpleble.viewmodels

import android.arch.lifecycle.LiveData
import android.os.Parcelable

/**
 * Created by tsuyoshihyuga on 2018/04/23.
 */
interface ScanListViewModelInterface {
    fun getData() : LiveData<List<Parcelable>>
}