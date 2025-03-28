package com.example.wifidirecthotspot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Panel
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.wifidirecthotspot.ui.theme.WIFIDirectHotspotTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    val manager: WifiP2pManager by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }

    lateinit var channel: WifiP2pManager.Channel
    // At the top level of your kotlin file:
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    lateinit var wifipassword: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WIFIDirectHotspotTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting()
                }
            }
        }

        GlobalScope.launch { wifipassword = readPassword().toString()}

        channel = manager.initialize(this, mainLooper, null)

        requetPermion(Manifest.permission.ACCESS_FINE_LOCATION)
        requetPermion(Manifest.permission.NEARBY_WIFI_DEVICES)

    }

    private suspend fun readPassword(): String? {
        val dataStoreKey = stringPreferencesKey("password")
        val preferences = dataStore.data.first()
        val newValue = preferences[dataStoreKey]
        if (newValue  == null){
            return "123456789"
        }else{
            return newValue
        }
    }

    private fun requetPermion(permission: String) {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                } else {
                    // Explain to the user that the feature is unavailable because the
                }
            }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                //
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, permission
            ) -> {
                // In an educational UI, explain to the user why your app requires this
            }

            else -> {
                requestPermissionLauncher.launch(
                    permission
                )
            }
        }
    }

    @Composable
    fun Greeting(modifier: Modifier = Modifier) {
        val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            //
        }
        var wifipassword by remember {
            mutableStateOf(wifipassword)
        }

        Column (modifier = Modifier.fillMaxSize()){
            Column(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(color = Color.Blue),verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "DIRECT-WIFI-Hotspot")
                TextField(value = wifipassword,
                    onValueChange = {
                        wifipassword = it
                        GlobalScope.launch { storePassword(it) }
                    },
                    label = { Text("WIFI Direct Password (Length - between 8 and 63)")},
                    modifier = Modifier,
                    singleLine = true)

                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        launcher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                    }else{
                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    discov(wifipassword)
                }) {
                    Text(text = "Create WIFI Direct")
                }

                Button(onClick = {
                    disconnect()
                }) {
                    Text(text = "Remove WIFI Direct")
                }

            }
        }
    }

    private suspend fun storePassword(wifipassword: String) {
        val dataStoreKey = stringPreferencesKey("password")
        dataStore.edit {
            it[dataStoreKey] = wifipassword
        }
    }

    private fun discov(wifipassword: String) {
        val config = WifiP2pConfig.Builder().apply {
            setNetworkName("DIRECT-WIFI-Hotspot")
            setPassphrase(wifipassword)
        }.build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        manager.createGroup(channel,config,object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Toast.makeText(this@MainActivity, "Successfully Created", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(reason: Int) {
                Intent(Panel.ACTION_WIFI).also {
                    startActivity(it)
                }
            }}
        )
    }

    private fun disconnect() {
        manager.removeGroup(channel,object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Toast.makeText(this@MainActivity, "Successfully Removed", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT).show()
            } }
        )
    }
}