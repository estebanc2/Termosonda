package com.mtc.termosonda.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.util.Log
import com.mtc.termosonda.data.model.NvsState
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BleManager @Inject constructor(
    bluetoothAdapter: BluetoothAdapter,
    private val context: Context,
    private val listener: BleListener
) {
    companion object {

        val SERV_UUID: UUID     = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb") //service
        val DATA_UUID: UUID     = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb") //notify temperatures
        val CAPTURE_UUID: UUID  = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb") //write capture date/period
        val STATE_UUID: UUID    = UUID.fromString("0000ffe3-0000-1000-8000-00805f9b34fb") //notify nvs state
        val DOWNLOAD_UUID: UUID = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb") //indicate stored data
        val REC_INFO_UUID: UUID = UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb") //read info to setup the download
        const val SCAN_IN_MS: Long = 3000
        const val TAG = "Ble"
    }
    private var connected = false
    private var myGatt: BluetoothGatt? = null
    private var dataChar: BluetoothGattCharacteristic? = null
    private var captureChar: BluetoothGattCharacteristic? = null
    private var stateChar: BluetoothGattCharacteristic? = null
    private var downloadChar: BluetoothGattCharacteristic? = null
    private var recInfoChar: BluetoothGattCharacteristic? = null
    private var stateDescriptor: BluetoothGattDescriptor? = null
    private var scanning = false
    private val bleSet = mutableSetOf("no_unit")
    private val bleScanner = bluetoothAdapter.bluetoothLeScanner
    private val handler = Handler()
    private var tryCall = false

    fun scanDevice() {
        Log.i(TAG,"entro al scanDevice")
        tryCall = false
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                bleScanner.stopScan(scanCallback)
                listener.notifyBleSet(bleSet)
                Log.w(TAG, "scan devices timeout")
            }, SCAN_IN_MS)
            scanning = true
            bleScanner.startScan(scanCallback)
        } else {
            scanning = false
            bleScanner.stopScan(scanCallback)
        }
    }

    fun tryConnect(device: String) {
        listener.notifyState(NvsState.UNKNOWN)
        Log.i(TAG,"try connect to $device")
        if(myGatt != null){
            myGatt!!.disconnect()
            Log.w(TAG,"unexpected not null gatt")
        }
        val scanFilter = ScanFilter.Builder()
            .setDeviceName(device)
            .build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)//  .SCAN_MODE_LOW_LATENCY)
            .build()
        //Log.i(TAG, "device to try: $device")
        handler.postDelayed({
            bleScanner.stopScan(scanCallback)
            Log.w(TAG, "try connect timeout")
        }, SCAN_IN_MS)
        tryCall = true
        bleScanner.startScan(mutableListOf(scanFilter), scanSettings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            //super.onScanResult(callbackType, result)
            if (tryCall) {
                myGatt = result.device.connectGatt(
                    context, true, gattCallback, // German source: autoConnect = false??
                    BluetoothDevice.TRANSPORT_LE
                )
                bleScanner.stopScan(this)
                tryCall = false
            }
            if(scanning){
                val aDevice = result.device
                if (aDevice != null && aDevice.name != null) {
                    Log.i(TAG, "discovered device: ${result.device.name}")
                    bleSet.add(aDevice.name)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "error on scan failed: $errorCode")
            bleScanner.stopScan(this)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            //Log.i(TAG, "en el gatt callback en on connect change")
            gatt.requestMtu(517) //(14 x 2 x 18) + 3
            if (connected) {
                Log.w(TAG, "in on connection change: connected = true ==> should disconnect")
                listener.notifyBle(false)
                connected = false
                gatt.close() // Close the GATT client here as well
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, " - - - connected!- - -")
                    myGatt = gatt
                } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    // Handle the disconnection, e.g., update UI, stop services
                    Log.w(TAG, "Peripheral disconnected: ${gatt.device.name}")
                    gatt.close() // Close the GATT client here as well
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.i(TAG, "new mtu: $mtu")
                gatt.discoverServices()
                Log.i(TAG, " - - - go to discover services- - -")
            } else {
                Log.e(TAG, "mtu change fails with code $status")
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "NO HAY GATT SUCCESS")
            } else {
                val service = gatt.getService(SERV_UUID)
                if (service != null) {
                    if (!connected) {
                        listener.notifyBle(true)
                        connected = true
                    }
                    dataChar = service.getCharacteristic(DATA_UUID)
                    captureChar = service.getCharacteristic(CAPTURE_UUID)
                    stateChar = service.getCharacteristic(STATE_UUID)
                    downloadChar = service.getCharacteristic(DOWNLOAD_UUID)
                    recInfoChar = service.getCharacteristic(REC_INFO_UUID)
                    if (stateChar != null) {
                        Log.i(TAG,"state notify: ${gatt.setCharacteristicNotification(stateChar, true)}")
                        stateDescriptor = stateChar!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (stateDescriptor != null){
                            stateDescriptor!!.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            gatt.writeDescriptor(stateDescriptor)
                        }
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor == stateDescriptor){
                if (dataChar != null) {
                    Log.i(TAG,"data notify: ${gatt.setCharacteristicNotification(dataChar, true)}")
                    val descriptor = dataChar!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val sample: ByteArray = characteristic.value
            when(characteristic){
                dataChar -> listener.notifySample(sample)
                stateChar -> listener.notifyState(NvsState.entries[sample[0].toInt()])
                downloadChar -> listener.notifyDownload(sample)
                else -> Log.w(TAG, "unexpected characteristic??")
            }
        }
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic == recInfoChar) {
                listener.notifyRecInfo(characteristic.value)
                Log.i(TAG, "     recInfoChar was reed")
                if (downloadChar != null){
                    Log.i(TAG,"download indicate: ${myGatt!!.setCharacteristicNotification(downloadChar, true)}")
                    val descriptor = downloadChar!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    myGatt!!.writeDescriptor(descriptor)
                    listener.notifyState(NvsState.WAIT)
                }
            }
        }
    }

    fun startDownload(){
        if (myGatt == null) {
            Log.i(TAG, "     in startDownload: myGatt = null")
            return
        }
        if (recInfoChar == null){
            Log.i(TAG, "     in startDownload: recInfoChar = null")
            return
        }
        myGatt!!.readCharacteristic(recInfoChar)
    }

    fun sendToSonda(data: ByteArray) {
        if (myGatt == null) {
            Log.i(TAG, "     in sendToSonda and gatt = null")
            return
        }
        if (captureChar == null) {
            Log.i(TAG, "     in sendToOnMast and myChar = null")
            return
        }
        captureChar!!.value = data
        myGatt!!.writeCharacteristic(captureChar)
    }
    /*
        fun reconnect() {
            myGatt?.connect()
        }

        fun disconnect() {
            myGatt?.disconnect()
        }

        fun closeBle() {
            Log.i(TAG, "Close?")
            if (myGatt != null) {
                myGatt!!.close()
            }
        }
        */

}