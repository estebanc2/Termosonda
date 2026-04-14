package com.mtc.termosonda.ui

import android.icu.text.DecimalFormat
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtc.termosonda.ui.permissions.PermissionsUtils
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mtc.termosonda.data.model.NvsState
import com.mtc.termosonda.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: SondaViewModel = hiltViewModel()) {
    val sondaUI by viewModel.uiState.collectAsState()
    val activity = LocalActivity.current
    val permissionState =
        rememberMultiplePermissionsState(permissions = PermissionsUtils.permissions)
    NoPermissionDialog(
        show = !permissionState.allPermissionsGranted,
        onConfirm = { activity?.finish() }
    )
    Column(
        modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {//(horizontalAlignment = Alignment.CenterHorizontally){
        SondaHeader(
            sondaUI.bleConnected,
            sondaUI.showList,
            progress = sondaUI.recordLevel,
            preShow =sondaUI.preShow,
            bleSet = sondaUI.bleSet,
            state = sondaUI.state,
            nvsImage = sondaUI.nvsImage,
            viewModel = viewModel)
        VerticalList(sondaUI.tempList)
    }
}

@Composable
fun SondaHeader(bleState: Boolean, show: Boolean, progress: Float, preShow: Boolean, bleSet: Set<String>, state: NvsState, nvsImage: Int, viewModel: SondaViewModel) {
    // var showList by remember { mutableStateOf(false) }
    val valuesPickerState = rememberPickerState()
    val values = remember { (1..60).map { it.toString() } }
    Card{
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ble72),
                    contentDescription = "background",
                    contentScale = ContentScale.Inside,//.Fit,
                    colorFilter = ColorFilter.tint(color = if (bleState) Green else Red),
                    modifier = Modifier
                        .clickable { viewModel.showList() }
                        .padding(10.dp),
                )
                Button(
                    onClick = { viewModel.startCapture(valuesPickerState.selectedItem.toInt()) },
                    enabled = state == NvsState.EMPTY
                ) {
                    Text(
                        text = stringResource(R.string.capture),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = { viewModel.startDownload() },
                    enabled = state == NvsState.FULL || state == NvsState.PARTIAL || state == NvsState.LOADING
                ) {
                    Text(
                        text = stringResource(R.string.download),
                        fontSize = 16.sp
                    )
                }
                Image(
                    painter = painterResource(id = nvsImage),
                    contentDescription = "background",
                    contentScale = ContentScale.Inside,//.Fit,
                    //colorFilter = ColorFilter.tint(color = if(bleState) Green else Red),
                    modifier = Modifier.padding(10.dp)//,.clickable { viewModel.showList() }
                )
            }
            if (state == NvsState.EMPTY) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Text(stringResource(R.string.capture_interval))
                    Picker(
                        pickerState = valuesPickerState,
                        items = values,
                        visibleItemsCount = 3,
                        modifier = Modifier.fillMaxWidth(0.5f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 16.sp),
                        dividerColor = Color(0xFFE8E8E8)
                    )
                }
            } else if(state== NvsState.WAIT){
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    color = ProgressIndicatorDefaults.linearColor,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }
            if (preShow){
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp))
            }
            if (show){
                Column {
                    bleSet.forEach { device ->
                        Text(text = device,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable { viewModel.tryConnect(device) }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun VerticalList(temps: List<Int>) {
    val decimal = DecimalFormat("0.0")
    Card {
        LazyColumn {
            itemsIndexed(temps) { index, item ->
                Text(
                    text = stringResource(R.string.temperature) + " ${index+1}: ${decimal.format(item/100.0)}" +stringResource(
                        R.string.grad),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}
