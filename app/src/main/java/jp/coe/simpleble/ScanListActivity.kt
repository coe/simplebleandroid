package jp.coe.simpleble

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import jp.coe.simpleble.handlers.ScanListHandler

class ScanListActivity : AppCompatActivity(), ScanListHandler {
    override fun onClickScanList(scanList: Parcelable) {
        val intent = Intent()
        intent.putExtra(EXTRA_SCAN,scanList)
        setResult(Activity.RESULT_OK,intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_list)
        title = "ScanListActivity"
    }

    companion object {
        val EXTRA_SCAN = "scan"
    }
}
