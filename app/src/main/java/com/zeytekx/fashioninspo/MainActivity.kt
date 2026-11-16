package com.zeytekx.fashioninspo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.zeytekx.fashioninspo.data.Store
import com.zeytekx.fashioninspo.ui.Access
import com.zeytekx.fashioninspo.ui.App
import com.zeytekx.fashioninspo.ui.FFTheme
import com.zeytekx.fashioninspo.ui.Incoming
import com.zeytekx.fashioninspo.ui.UI

class MainActivity : ComponentActivity() {
    private var access by mutableStateOf(Access.NONE)

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        checkAccess()
        UI.refresh++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        checkAccess()
        if (savedInstanceState == null) handle(intent)
        setContent {
            FFTheme {
                App(access, onRequest = { permLauncher.launch(perms()) }, onSettings = { openSettings() })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        checkAccess()
        UI.refresh++
    }

    override fun onPause() {
        super.onPause()
        Store.saveNow()
    }

    private fun perms(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun checkAccess() {
        access = when {
            Build.VERSION.SDK_INT >= 33 && granted(Manifest.permission.READ_MEDIA_IMAGES) -> Access.FULL
            Build.VERSION.SDK_INT < 33 && granted(Manifest.permission.READ_EXTERNAL_STORAGE) -> Access.FULL
            Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> Access.PARTIAL
            else -> Access.NONE
        }
    }

    private fun openSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }
    }

    private fun handle(i: Intent?) {
        if (i == null) return
        when (i.action) {
            Intent.ACTION_SEND -> {
                if (i.type?.startsWith("image/") == true) {
                    IntentCompat.getParcelableExtra(i, Intent.EXTRA_STREAM, Uri::class.java)?.let {
                        UI.incoming = Incoming.Images(listOf(it))
                    }
                } else {
                    val t = i.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
                    if (t.isNotEmpty()) UI.openSharedText(t, i.getStringExtra(Intent.EXTRA_SUBJECT))
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val l = IntentCompat.getParcelableArrayListExtra(i, Intent.EXTRA_STREAM, Uri::class.java)
                if (!l.isNullOrEmpty()) UI.incoming = Incoming.Images(l.toList())
            }
        }
    }
}
