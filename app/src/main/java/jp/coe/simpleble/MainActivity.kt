package jp.coe.simpleble

import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.os.Parcelable
import android.support.v7.app.AppCompatActivity
import android.util.Log
import jp.coe.simpleble.ble.MyAdvertiseCallback
import jp.coe.simpleble.ble.MyBluetoothGattCallback
import jp.coe.simpleble.ble.MyBluetoothGattServerCallback
import jp.coe.simpleble.fragments.MainFragment
import jp.coe.simpleble.fragments.ScanlistFragment
import jp.coe.simpleble.handlers.MainHandler
import jp.coe.simpleble.handlers.ScanListHandler
import java.util.*


class MainActivity : AppCompatActivity(),MainHandler, ScanListHandler {
    override fun onClickSend() {

    }

    override fun onClickImage() {

    }



    private val mBluetoothGattCallback:BluetoothGattCallback = MyBluetoothGattCallback()
    private val mBluetoothGattServerCallback: BluetoothGattServerCallback = MyBluetoothGattServerCallback()
    private val mAdvertiseCallback: AdvertiseCallback = MyAdvertiseCallback()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_IMAGE_CAPTURE -> {

            }
        }
    }

    override fun onClickScanList(scanList: Parcelable) {
        //接続する
        Log.d(TAG,"onClickScanList")
        val sr:ScanResult = scanList as ScanResult
        sr.device.connectGatt(this,false,mBluetoothGattCallback)
    }

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
        //アドバタイジング開始
        val manager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val server = manager.openGattServer(this,mBluetoothGattServerCallback)

        val uuid = UUID.fromString(SERVICE_UUID)
        val service = BluetoothGattService(
                uuid,BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        server.addService(service)
        val parcelUuid = ParcelUuid(uuid)
        val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build()

        val advertiseData = AdvertiseData.Builder()
                .addServiceUuid(parcelUuid)
                .setIncludeDeviceName(true)
                .build()

        manager.adapter.bluetoothLeAdvertiser.startAdvertising(settings,advertiseData,mAdvertiseCallback)
    }

    companion object {
        private val REQUEST_IMAGE_CAPTURE = 1

        private val TAG = "MainActivity"
        private val SERVICE_UUID = "D096F3C2-5148-410A-BA6A-20FEAD00D7CA"

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
