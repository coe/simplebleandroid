package jp.coe.simpleble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.os.Bundle
import android.os.ParcelUuid
import android.os.Parcelable
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import jp.coe.simpleble.fragments.MainFragment
import jp.coe.simpleble.fragments.ScanlistFragment
import jp.coe.simpleble.handlers.MainHandler
import jp.coe.simpleble.handlers.ScanListHandler
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
    private val MTU = 512

    private var mMtu = 32

    private var mGatt:BluetoothGatt? = null
    override fun onClickSend(imageBitmap: Bitmap?)
    {
        Log.d(TAG,"onClickSend:"+imageBitmap?.byteCount)
        val baoStream = ByteArrayOutputStream()
        imageBitmap?.compress(CompressFormat.JPEG, 90, baoStream)
        baoStream.flush()
        val bArray = baoStream.toByteArray()
        baoStream.close()

        sendBytes(bArray)

    }

    private fun sendBytes(bArray: ByteArray?) {
        //書き込む
        val settingsCharacteristic = mGatt?.getService(UUID.fromString(SERVICE_UUID))
                ?.getCharacteristic(UUID.fromString(IMAGE_WRITE_CHARACTERISTIC_UUID))
//                settingsCharacteristic?.value = baseByte
        mGatt?.beginReliableWrite()

        sendingByte = bArray
        sendByte(settingsCharacteristic!!)
    }

    private var sendingByte:ByteArray? = null
    private var mOffset = 0
    private var sendingBytesList:LinkedList<ByteArray> = LinkedList()

    private fun sendByte(settingsCharacteristic:BluetoothGattCharacteristic){
        sendingByte?.let {
            mOffset = 0
            //mMtuずつ送る
            var maxsize = it.size
            Log.d(TAG,"maxsize:"+maxsize)
            var offset = 0
            val b = it.get(1)
            while (maxsize > mMtu) {
                val arr = it.sliceArray(offset..offset+mMtu-1)
                sendingBytesList.add(arr)
                offset += mMtu
                maxsize -= mMtu
                Log.d(TAG,"maxsize:"+maxsize)
            }
            val arr = it.sliceArray(offset..it.size-1)
            Log.d(TAG,"arrあまり:"+arr.size)
            sendingBytesList.add(arr)

            settingsCharacteristic.setValue(sendingBytesList.poll())
            mGatt?.writeCharacteristic(settingsCharacteristic)

        }
    }

    var nowDataSize = 0

    override fun onClickScanList(scanList: Parcelable) {
        //接続する
        Log.d(TAG,"onClickScanList")
        val sr:ScanResult = scanList as ScanResult

        mGatt = sr.device.connectGatt(this,false,object : BluetoothGattCallback(){
            override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
                super.onReadRemoteRssi(gatt, rssi, status)
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                super.onCharacteristicRead(gatt, characteristic, status)
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                super.onCharacteristicWrite(gatt, characteristic, status)

                var tmpSize = nowDataSize + mMtu

                if (tmpSize > MTU_MAX || sendingBytesList.size == 0) {
                    //512バイトまでしか送れないっぽい
                    Log.d(TAG,"executeReliableWrite:")
                    nowDataSize = 0
                    gatt?.executeReliableWrite()
                } else  {
                    nowDataSize += mMtu
                    val data = sendingBytesList.poll()
                    Log.d(TAG,"onCharacteristicWrite:"+data.size)
                    characteristic?.setValue(data)
                    gatt?.writeCharacteristic(characteristic!!)
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

            override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
                super.onPhyUpdate(gatt, txPhy, rxPhy, status)
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
                    val characteristic = mGatt?.getService(UUID.fromString(SERVICE_UUID))
                            ?.getCharacteristic(UUID.fromString(IMAGE_WRITE_CHARACTERISTIC_UUID))
                    characteristic?.setValue(data)
                    gatt?.writeCharacteristic(characteristic!!)
                }
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
                super.onDescriptorWrite(gatt, descriptor, status)
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                super.onCharacteristicChanged(gatt, characteristic)
            }

            override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
                super.onDescriptorRead(gatt, descriptor, status)
            }

            override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
                super.onPhyRead(gatt, txPhy, rxPhy, status)
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
            super.onServiceAdded(status, service)
        }
    }
    private val mAdvertiseCallback: AdvertiseCallback = object : AdvertiseCallback(){

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragment = MainFragment.newInstance()
        supportFragmentManager.beginTransaction().add(R.id.container,fragment).commit()

        val uuid = UUID.fromString(SERVICE_UUID)
        val manager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothGattServer = manager.openGattServer(this,mBluetoothGattServerCallback)

        val service = BluetoothGattService(
                uuid,BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        bluetoothGattServer?.addService(service)
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
        val uuid = UUID.fromString(SERVICE_UUID)

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

        manager.adapter.bluetoothLeAdvertiser.startAdvertising(settings,advertiseData,object : AdvertiseCallback(){

        })
    }

    companion object {
        private val PERMISSION_REQUEST = 1

        private val TAG = "MainActivity"
        val SERVICE_UUID = "D096F3C2-5148-410A-BA6A-20FEAD00D7CA"
        val IMAGE_WRITE_CHARACTERISTIC_UUID = "42184378-A26D-474B-82CA-43C03AA7A701"

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
