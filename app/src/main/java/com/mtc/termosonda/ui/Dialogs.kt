package com.mtc.termosonda.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun NoPermissionDialog(
    show: Boolean,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(onDismissRequest = {}, confirmButton = {
            TextButton(onClick = {onConfirm()}) {
                Text(text = "ok",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
            }
        }, text = {
            Text(text = "this app doesn't run without Location permission. Please go to settings and allow it",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold)
        })
    }
}
