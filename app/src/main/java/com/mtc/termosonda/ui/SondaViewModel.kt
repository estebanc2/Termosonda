package com.mtc.termosonda.ui

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.termosonda.R
import com.mtc.termosonda.data.FileRepo
import com.mtc.termosonda.data.KeyStore
import com.mtc.termosonda.data.ble.BleListener
import com.mtc.termosonda.data.ble.BleManager
import com.mtc.termosonda.data.model.NvsState
import com.mtc.termosonda.data.model.SondaUIData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SondaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileRepo: FileRepo,
    bleAdapter: BluetoothAdapter): ViewModel(), BleListener {
    companion object {
        const val TAG = "Sonda"
        const val SONDA_SIZE = 9
    }

    private val bleManager = BleManager(bleAdapter, context, this)
    private val keyStore = KeyStore(context)
    private val _uiState = MutableStateFlow(SondaUIData())
    val uiState: StateFlow<SondaUIData> = _uiState.asStateFlow()
    private var deviceName = ""
    private var tryName = ""
    private var showDiscoveredDevices = true
    private var sondasPerBlock = 0
    private var blockQty = 0
    private var period = 0
    private var fileName = ""
    private var lastTemp = false
    private var content = ""
    private var recId = 0
    private val _fileCreationResult = MutableLiveData<FileResult>()

    sealed class FileResult {
        data class Success(val file: File) : FileResult()
        data class Error(val message: String) : FileResult()
    }

    init {
        getKey()
    }

    override fun notifyBleSet(newBleSet: Set<String>) {
        _uiState.value.let {
            _uiState.value = it.copy(bleSet = newBleSet)
        }
        _uiState.value.let {
            _uiState.value = it.copy(showList = true)
        }
        _uiState.value.let {
            _uiState.value = it.copy(preShow = false)
        }
        showDiscoveredDevices = false
    }

    override fun notifyBle(connected: Boolean) {
        _uiState.value.let {
            _uiState.value = it.copy(bleConnected = connected)
        }
        Log.i(TAG, "connected = $connected")
        if (connected && deviceName != tryName) {
            deviceName = tryName
            saveKey(deviceName)
        }
    }

    override fun notifySample(newSample: ByteArray) {
        if (newSample.size == 2 * SONDA_SIZE) {
            val sampleList = Array(SONDA_SIZE) { 0 }
            //Log.i(TAG, "new sample: $newSample")
            for (index in sampleList.indices) {
                sampleList[index] =
                    (bytesArray2ToInt(newSample[2 * index], newSample[2 * index + 1]))
            }
            _uiState.value.let {
                _uiState.value = it.copy(tempList = sampleList.asList())
            }
        } else {
            Log.i(TAG, "bad new sample: $newSample")
        }
    }

    override fun notifyState(newState: NvsState) {
        Log.i(TAG, "NVS state: $newState")
        _uiState.value.let {
            _uiState.value = it.copy(state = newState)
        }
    }

    override fun notifyDownload(b: ByteArray) {
        if (b.size != SONDA_SIZE * 2 * sondasPerBlock && lastTemp) {
            return
        }
        for (block in 0..<sondasPerBlock) {
            val tick = recId * period
            content += "$tick "
            for (i in 0..<SONDA_SIZE) {
                val recTempX100 = bytesArray2ToInt(
                    b[(2 * i) + SONDA_SIZE * 2 * block],
                    b[2 * i + 1 + SONDA_SIZE * 2 * block]
                )
                if (recTempX100 == 0x7F7F) {
                    lastTemp = true
                    break
                }
                content += "${recTempX100.toFloat() / 100.0} "
            }
            content += "\n"
            recId += 1
        }
        _uiState.value.let {
            _uiState.value =
                it.copy(recordLevel = recId.toFloat() / (blockQty * sondasPerBlock).toFloat())
        }
        if (recId == blockQty * sondasPerBlock || lastTemp) {
            lastTemp = false
            createFileAndShare(fileName, content)
        }
    }

    override fun notifyRecInfo(b: ByteArray) {
        if (b.size != 13) {
            Log.e(TAG, context.getString(R.string.rec_info_wrong_size))
            return
        }
        blockQty = bytesArray2ToInt(b[12], b[11])
        sondasPerBlock = bytesArray2ToInt(0, b[10])
        period = bytesArray2ToInt(b[0], b[1])
        val timeStamp = byteArrayToLong(byteArrayOf(b[2], b[3], b[4], b[5], b[6], b[7], b[8], b[9]))
        val date = Date(timeStamp)
        val format = SimpleDateFormat("yyMMddHHmm", Locale.getDefault())
        fileName = "SONDA_" + format.format(date) + ".txt"
        Log.i(
            TAG, "sondas per block: $sondasPerBlock, " +
                    "block qty: $blockQty, period: $period, file name: $fileName"
        )
        lastTemp = false
    }

    private fun createFileAndShare(fileName: String, content: String) {
        viewModelScope.launch {
            val file = fileRepo.createFileAndWrite(fileName, content)
            if (file != null) {
                _fileCreationResult.value = FileResult.Success(file)
            } else {
                _fileCreationResult.value = FileResult.Error("Failed to create file.")
            }
        }
    }


    private fun byteArrayToLong(byteArray: ByteArray): Long {
        val buffer = ByteBuffer.wrap(byteArray)
        buffer.order(ByteOrder.BIG_ENDIAN)
        return buffer.long
    }

    private fun byteArrayToString(sample: ByteArray): String {
        return sample.joinToString(separator = " ") {
            String.format("%02X", it)
        }
    }

    fun tryConnect(name: String) {
        bleManager.tryConnect(name)
        tryName = name
    }

    private fun bytesArray2ToInt(msb: Byte, lsb: Byte): Int {
        return (msb.toInt() shl 8 or (lsb.toInt() and 0xFF))
    }

    private fun intToBytesArray(anInt: Int): ByteArray {
        return byteArrayOf((anInt shr 8).toByte(), anInt.toByte())
    }

    private fun getKey() {
        CoroutineScope(Dispatchers.IO).launch {
            keyStore.getKey.collect { savedKey ->
                if (savedKey.isNotEmpty()) {
                    deviceName = savedKey
                    tryConnect(savedKey)
                }
            }
        }
    }

    private fun saveKey(keyToSave: String) {
        CoroutineScope(Dispatchers.IO).launch {
            keyStore.saveKey(keyToSave)
        }
    }

    fun showList() {
        if (showDiscoveredDevices) {
            _uiState.value.let {
                _uiState.value = it.copy(preShow = true)
            }
            bleManager.scanDevice()
        } else {
            _uiState.value.let {
                _uiState.value = it.copy(showList = false)
            }
            showDiscoveredDevices = true
        }
    }

    fun startCapture(period: Int) {
        val timeStamp: Long = System.currentTimeMillis()
        val byteArray: ByteArray =
            intToBytesArray(period) + ByteBuffer.allocate(Long.SIZE_BYTES).putLong(timeStamp)
                .array()
        Log.i(TAG, "capture begin with ${byteArrayToString(byteArray)}, timeStamp: $timeStamp")
        bleManager.sendToSonda(byteArray)
    }

    fun startDownload() {
        Log.i(TAG, "download begin")
        bleManager.startDownload()
        _uiState.value.let {
            _uiState.value = it.copy(recordLevel = 0f)
        }
    }
}
