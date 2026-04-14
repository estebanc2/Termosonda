package com.mtc.termosonda.data.ble

import com.mtc.termosonda.data.model.NvsState

interface BleListener {
    fun notifyBle(connected: Boolean)
    fun notifySample(newSample: ByteArray)
    fun notifyState(newState: NvsState)
    fun notifyDownload(b: ByteArray)
    fun notifyRecInfo(b: ByteArray)
    fun notifyBleSet(newBleSet: Set<String>)
}