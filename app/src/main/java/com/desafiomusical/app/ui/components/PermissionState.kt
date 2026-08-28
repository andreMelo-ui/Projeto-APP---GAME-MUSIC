package com.desafiomusical.app.ui.components

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Estado atual de uma permissão em runtime + como pedi-la. */
data class PermissionState(val granted: Boolean, val request: () -> Unit)

/**
 * Acompanha e solicita uma permissão em runtime (ex.: `CAMERA` pra escanear QR,
 * `NEARBY_WIFI_DEVICES` pra descoberta NSD em API 33+ — em versões mais
 * antigas o SO considera essa permissão automaticamente concedida). Não havia
 * nenhum padrão de permissão em runtime no projeto antes desta tela.
 */
@Composable
fun rememberPermissionState(permission: String): PermissionState {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            granted = isGranted
        }
    return PermissionState(granted = granted, request = { launcher.launch(permission) })
}
