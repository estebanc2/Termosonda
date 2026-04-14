package com.mtc.termosonda.data.model

import com.mtc.termosonda.R

data class SondaUIData(
    val bleSet: Set<String> = setOf("no_unit"),
    val bleConnected: Boolean = false,
    val tempList: List<Int> = (1..9).toList(),
    val downloadProgress: Int = 0,
    val state: NvsState = NvsState.UNKNOWN,
    val capture: Boolean = false,
    val showList: Boolean = false,
    val preShow: Boolean = false,
    val nvsImage:Int = R.drawable.empty,
    val recordLevel:Float = 0.0f
)
