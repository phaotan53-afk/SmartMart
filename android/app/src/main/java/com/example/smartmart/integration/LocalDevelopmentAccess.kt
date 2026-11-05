package com.example.smartmart.integration

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smartmart.R

private const val LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

/** API 37 protects the local development server. This permission exists in debug only. */
@Composable
fun LocalDevelopmentAccess(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val requiresPermission = Build.VERSION.SDK_INT >= 37 &&
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    var granted by rememberSaveable {
        mutableStateOf(!requiresPermission || context.checkSelfPermission(LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED)
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) request.launch(LOCAL_NETWORK) }
    if (granted) {
        content()
    } else {
        Scaffold { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.host_local_permission_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.host_local_permission_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { request.launch(LOCAL_NETWORK) }) {
                    Text(stringResource(R.string.host_local_permission_action))
                }
            }
        }
    }
}
