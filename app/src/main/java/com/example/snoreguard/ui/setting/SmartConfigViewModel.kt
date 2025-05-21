package com.example.snoreguard.ui.setting

import android.content.Context
import android.net.wifi.WifiManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espressif.iot.esptouch.EsptouchTask
import com.espressif.iot.esptouch.IEsptouchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmartConfigViewModel : ViewModel() {
    private val _isConnecting = MutableLiveData<Boolean>()
    val isConnecting: LiveData<Boolean> = _isConnecting

    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    private val _availableSSIDs = MutableLiveData<List<String>>()
    val availableSSIDs: LiveData<List<String>> = _availableSSIDs

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        _isConnecting.value = false
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    fun scanWifiNetworks(context: Context) {
        viewModelScope.launch {
            try {
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                val ssid = wifiInfo.ssid.replace("\"", "")
                _availableSSIDs.postValue(listOf(ssid))
            } catch (e: Exception) {
                _error.postValue("Failed to scan WiFi networks: ${e.message}")
            }
        }
    }

    fun startSmartConfig(context: Context, ssid: String, bssid: String, password: String) {
        if (_isConnecting.value == true) return

        _isConnecting.value = true
        _connectionStatus.value = ConnectionStatus.CONNECTING

        viewModelScope.launch {
            try {
                val task = withContext(Dispatchers.IO) {
                    EsptouchTask(ssid, bssid, password, context)
                }
                val result: IEsptouchResult = withContext(Dispatchers.IO) {
                    task.executeForResult()
                }
                if (result.isSuc) {
                    _connectionStatus.postValue(ConnectionStatus.CONNECTED)
                } else {
                    _connectionStatus.postValue(ConnectionStatus.FAILED)
                    _error.postValue("Provision failed")
                }
            } catch (e: Exception) {
                _connectionStatus.postValue(ConnectionStatus.FAILED)
                _error.postValue("SmartConfig failed: ${e.message}")
            } finally {
                _isConnecting.postValue(false)
            }
        }
    }

    fun cancelSmartConfig() {
        viewModelScope.launch {
            try {
                _isConnecting.value = false
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            } catch (e: Exception) {
                _error.postValue("Error canceling SmartConfig: ${e.message}")
            }
        }
    }

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        FAILED
    }
}