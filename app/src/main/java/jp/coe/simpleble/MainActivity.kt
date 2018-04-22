package jp.coe.simpleble

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import jp.coe.simpleble.fragments.MainFragment
import jp.coe.simpleble.fragments.ScanlistFragment
import jp.coe.simpleble.handlers.MainHandler
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),MainHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragment = MainFragment.newInstance()
        supportFragmentManager.beginTransaction().add(R.id.container,fragment).commit()
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    override fun onClickCentral() {
        Log.d(TAG,"onClickCentral")
        //リスト画面
        val fragment = ScanlistFragment.newInstance()
        supportFragmentManager.beginTransaction()

                .replace(R.id.container,fragment)
                .addToBackStack(null)
                .commit()

    }

    override fun onClickPeripheral() {
    }

    companion object {

        private val TAG = "MainActivity"

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
