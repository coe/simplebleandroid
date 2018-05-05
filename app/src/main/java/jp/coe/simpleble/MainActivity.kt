package jp.coe.simpleble

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.os.Bundle
import android.os.ParcelUuid
import android.os.Parcelable
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import jp.coe.simpleble.databinding.ActivityMainBinding
import jp.coe.simpleble.fragments.MainFragment
import jp.coe.simpleble.fragments.ScanlistFragment
import jp.coe.simpleble.handlers.MainHandler
import jp.coe.simpleble.handlers.ScanListHandler
import jp.coe.simpleble.observable.MainObservable
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.*

/**
 * 画面起動時にBLEの準備を行う>OK
 * 画面起動時にPeripheralとしてのアドバタイズを行う > OK
 * スキャン画面に移行する機能をもつ > OK
 * カメラボタンで画像を取得する
 * 送信ボタンを押したら画像を送信する
 * Centralからデータを受信した、画像を表示する
 */
class MainActivity : AppCompatActivity(),MainHandler, ScanListHandler {
    /**
     * 一回の交換で送れるexecuteReliableWriteあたりのバイト数
     * これ以上は一旦executeReliableWriteしてから再度送る
     */
    private val MTU_MAX = 512

    /**
     * 一回の交換で送れるバイト数
     * 一般的なAndroid端末であれば512で送れるが、FREETELが250、XPeriaが180前後が限度になる
     */
    private val MTU = 517

    private var mMtu = 32

    private var mGatt:BluetoothGatt? = null

    private var mainObservable: MainObservable = MainObservable()

    private val advertiseCallback:AdvertiseCallback = object : AdvertiseCallback(){

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this,R.layout.activity_main)
        binding.mainObservable = mainObservable

        val manager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothGattServer = manager.openGattServer(this,mBluetoothGattServerCallback)

        val service = BluetoothGattService(
                LONG_DATA_SERVICE_UUID,BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        service.addCharacteristic(BluetoothGattCharacteristic(LONG_DATA_WRITE_CHARACTERISTIC_UUID,BluetoothGattCharacteristic.PROPERTY_WRITE,BluetoothGattCharacteristic.PERMISSION_WRITE))
        service.addCharacteristic(BluetoothGattCharacteristic(LONG_DATA_WRITE_LENGTH_CHARACTERISTIC_UUID,BluetoothGattCharacteristic.PROPERTY_WRITE,BluetoothGattCharacteristic.PERMISSION_WRITE))

        bluetoothGattServer?.addService(service)
        //アドバタイジング開始
        val parcelUuid = ParcelUuid(LONG_DATA_SERVICE_UUID)
        val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build()

        val advertiseData = AdvertiseData.Builder()
                .addServiceUuid(parcelUuid)
                .setIncludeDeviceName(true)
                .build()

        manager.adapter.bluetoothLeAdvertiser.startAdvertising(settings,advertiseData,advertiseCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        return true
    }

    private var byteArray:ByteArray? = null
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.menu_camera -> {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
            R.id.menu_scan -> {
                val intent = Intent(this,ScanListActivity::class.java)
                startActivityForResult(intent,REQUEST_SCAN_LIST)
            }
            R.id.menu_send -> {
                Log.d(TAG,"onClickSend:"+mainObservable.imageBitmap?.byteCount)
                val baoStream = ByteArrayOutputStream()
                mainObservable.imageBitmap?.compress(CompressFormat.JPEG, 90, baoStream)
                baoStream.flush()
                byteArray = baoStream.toByteArray()
                baoStream.close()


                byteArray?.let {
                    mOffset = 0
                    //mMtuずつ送る
                    var maxsize = it.size
                    var offset = 0
                    val b = it.get(1)
                    while (maxsize > mMtu) {
                        val arr = it.sliceArray(offset..offset+mMtu-1)
                        sendingBytesList.add(arr)
                        offset += mMtu
                        maxsize -= mMtu
                    }
                    val arr = it.sliceArray(offset..it.size-1)
                    sendingBytesList.add(arr)

                }

//                mGatt?.beginReliableWrite()

                //書き込む
                val lengthCharacteristic = mGatt!!.getService(LONG_DATA_SERVICE_UUID)!!.getCharacteristic(LONG_DATA_WRITE_LENGTH_CHARACTERISTIC_UUID)
                val length = byteArray!!.size
                Log.d(TAG,"length:"+length)

                val ret = lengthCharacteristic.setValue(length,BluetoothGattCharacteristic.FORMAT_UINT32,0)

                Log.d(TAG,"descriptor ret:${ret}")
                mGatt?.writeCharacteristic(lengthCharacteristic)



            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(resultCode) {
            Activity.RESULT_OK -> {
                when(requestCode) {
                    REQUEST_IMAGE_CAPTURE -> {
                        val extras = data?.getExtras()
                        val imageBitmap = extras?.get("data") as Bitmap
                        mainObservable.imageBitmap = imageBitmap
                    }
                    REQUEST_SCAN_LIST -> {
                        val extras = data?.getExtras()
                        val scanResult: ScanResult? = extras?.getParcelable(ScanListActivity.EXTRA_SCAN)
                        mGatt = scanResult?.device?.connectGatt(this,false,object : BluetoothGattCallback(){

                            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                                Log.d(TAG,"onCharacteristicWrite:")
                                super.onCharacteristicWrite(gatt, characteristic, status)


                                if (sendingBytesList.size == 0) {
                                    //512バイトまでしか送れないっぽい
                                    Log.d(TAG,"終了:")
                                } else  {
                                    nowDataSize += mMtu
                                    val datas = sendingBytesList.poll()
                                    Log.d(TAG,"onCharacteristicWrite:"+datas.size)
                                    val lengthCharacteristic = gatt!!.getService(LONG_DATA_SERVICE_UUID)!!.getCharacteristic(LONG_DATA_WRITE_CHARACTERISTIC_UUID)

                                    lengthCharacteristic?.setValue(datas)
                                    gatt.writeCharacteristic(lengthCharacteristic!!)
                                }


                            }

                            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                                Log.d(TAG,"onServicesDiscovered")
                                super.onServicesDiscovered(gatt, status)
                                mGatt = gatt
                                mGatt?.services?.map {
                                    Log.d(TAG,"onServicesDiscovered service:"+it.uuid.toString())
                                    it.characteristics.map {
                                        Log.d(TAG,"onServicesDiscovered characteristic:"+it.uuid.toString())
                                    }

                                }
                                //MainFragment
                                supportFragmentManager.popBackStack()
                            }

                            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                                Log.d(TAG,"onMtuChanged:"+mtu)
                                mMtu = mtu-5
                                gatt?.discoverServices()

                                super.onMtuChanged(gatt, mtu, status)
                            }

                            override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
                                super.onReliableWriteCompleted(gatt, status)
                                if (sendingBytesList.size > 0) {
                                    val data = sendingBytesList.poll()
                                    Log.d(TAG,"onReliableWriteCompleted:"+data.size)
                                    //書き込む
                                    val characteristic = mGatt?.getService(LONG_DATA_SERVICE_UUID)
                                            ?.getCharacteristic(LONG_DATA_WRITE_CHARACTERISTIC_UUID)
                                    characteristic?.setValue(data)
                                    gatt?.writeCharacteristic(characteristic!!)
                                }
                            }

                            override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
                                Log.d(TAG,"onDescriptorWrite:${status}")
                                super.onDescriptorWrite(gatt, descriptor, status)

                            }

                            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                                super.onCharacteristicChanged(gatt, characteristic)
                            }

                            override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
                                super.onDescriptorRead(gatt, descriptor, status)
                            }

                            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                                Log.d(TAG,"onConnectionStateChange:"+newState)
                                super.onConnectionStateChange(gatt, status, newState)
                                when (newState) {
                                    BluetoothProfile.STATE_CONNECTED -> {
                                        //onServicesDiscoveredに移行
                                        gatt?.requestMtu(MTU)


                                    }
                                    BluetoothProfile.STATE_DISCONNECTED -> {
                                    }
                                    BluetoothProfile.STATE_CONNECTING -> {
                                    }
                                    BluetoothProfile.STATE_DISCONNECTING -> {

                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    override fun onClickSend(imageBitmap: Bitmap?)
    {

    }
    private var mOffset = 0
    private var sendingBytesList:LinkedList<ByteArray> = LinkedList()


    var nowDataSize = 0

    override fun onClickScanList(scanList: Parcelable) {

    }

    override fun onClickImage() {

    }

    private var connectingDevice:BluetoothDevice? = null
    private var state = -1

    private var bluetoothGattServer:BluetoothGattServer? = null

    private val mBluetoothGattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            Log.d(TAG,"onMtuChanged:${mtu}")
            super.onMtuChanged(device, mtu)
        }

        override fun onPhyUpdate(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(device, txPhy, rxPhy, status)
        }

        override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
            super.onExecuteWrite(device, requestId, execute)
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            val log = value?.toString(Charset.defaultCharset())
            Log.d(TAG,"onCharacteristicWriteRequest:"+log)
            bluetoothGattServer?.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset, value)
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
        }

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
        }

        override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(device, txPhy, rxPhy, status)
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            Log.d(TAG,"onServiceAdded:"+status)
            service?.characteristics?.forEach {
                Log.d(TAG,"onServiceAdded characteristics:"+it.uuid.toString())
            }
            super.onServiceAdded(status, service)
        }
    }
    private val mAdvertiseCallback: AdvertiseCallback = object : AdvertiseCallback(){

    }

    override fun onStart() {
        super.onStart()
        val permissioncheck = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permissioncheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_COARSE_LOCATION)) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.not_permission)
                        .setPositiveButton(android.R.string.ok, { _,_ ->
                            finish()
                        })
                        .show()
            } else {
                ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),PERMISSION_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    override fun onClickCentral() {
        Log.d(TAG,"onClickCentral")


    }

    override fun onClickPeripheral() {

    }

    companion object {
        val LONG_DATA_SERVICE_UUID = UUID.fromString("D096F3C2-5148-410A-BA6A-20FEAD00D7CA")
        val LONG_DATA_WRITE_CHARACTERISTIC_UUID = UUID.fromString("E053BD84-1E5B-4A6C-AD49-C672A737880C")
        private val LONG_DATA_WRITE_LENGTH_CHARACTERISTIC_UUID = UUID.fromString("C4BDAB8A-BAC1-477A-925C-E1665553953C")

        private val PERMISSION_REQUEST = 1

        private val REQUEST_IMAGE_CAPTURE = 1
        private val REQUEST_SCAN_LIST = 2

        private val TAG = "MainActivity"



        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
