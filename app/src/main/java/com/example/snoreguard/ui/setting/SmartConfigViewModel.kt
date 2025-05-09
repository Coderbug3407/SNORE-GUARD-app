package com.example.snoreguard.ui.setting

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets

class SmartConfigViewModel : ViewModel() {

    private val _isConnecting = MutableLiveData<Boolean>()
    val isConnecting: LiveData<Boolean> = _isConnecting

    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    private val _availableSSIDs = MutableLiveData<List<String>>()
    val availableSSIDs: LiveData<List<String>> = _availableSSIDs

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Default port for ESP32 SmartConfig
    private val ESP_PORT = 18266
    private var socket: DatagramSocket? = null

    init {
        _isConnecting.value = false
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    fun scanWifiNetworks(context: Context) {
        viewModelScope.launch {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

                if (!wifiManager.isWifiEnabled) {
                    _error.value = "WiFi is disabled. Please enable WiFi to scan for networks."
                    return@launch
                }

                // Start a scan
                val scanSuccess = wifiManager.startScan()
                if (!scanSuccess) {
                    _error.value = "Failed to start WiFi scan"
                    return@launch
                }

                // Get scan results
                val results = wifiManager.scanResults

                // Extract SSIDs
                val ssids = results.mapNotNull { it.SSID }
                    .filter { it.isNotEmpty() }
                    .distinct()

                _availableSSIDs.value = ssids

                if (ssids.isEmpty()) {
                    _error.value = "No WiFi networks found"
                }
            } catch (e: Exception) {
                _error.value = "Error scanning WiFi networks: ${e.message}"
                Log.e("SmartConfigViewModel", "Error scanning WiFi networks", e)
            }
        }
    }

    fun startSmartConfig(ssid: String, password: String, deviceName: String = "esp32_device") {
        if (_isConnecting.value == true) return

        _isConnecting.value = true
        _connectionStatus.value = ConnectionStatus.CONNECTING

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Create UDP socket
                    socket = DatagramSocket()
                    socket?.broadcast = true

                    // Create SmartConfig data packet
                    val smartConfigData = createSmartConfigPacket(ssid, password, deviceName)

                    // Send the packet to broadcast address
                    val broadcastAddress = InetAddress.getByName("255.255.255.255")
                    val packet = DatagramPacket(
                        smartConfigData,
                        smartConfigData.size,
                        broadcastAddress,
                        ESP_PORT
                    )

                    // Send the packet multiple times to ensure it's received
                    repeat(10) {
                        socket?.send(packet)
                        Thread.sleep(100)
                    }

                    // Wait for acknowledgment from ESP32
                    val buffer = ByteArray(1024)
                    val receivePacket = DatagramPacket(buffer, buffer.size)

                    // Set timeout for response
                    socket?.soTimeout = 10000 // 10 seconds

                    try {
                        socket?.receive(receivePacket)
                        val response = String(
                            receivePacket.data,
                            receivePacket.offset,
                            receivePacket.length,
                            StandardCharsets.UTF_8
                        )

                        if (response.contains("success")) {
                            _connectionStatus.postValue(ConnectionStatus.CONNECTED)
                        } else {
                            _connectionStatus.postValue(ConnectionStatus.FAILED)
                            _error.postValue("Device failed to connect: $response")
                        }
                    } catch (e: Exception) {
                        // Timeout or error receiving response
                        _connectionStatus.postValue(ConnectionStatus.TIMEOUT)
                        _error.postValue("Connection timed out or failed: ${e.message}")
                    } finally {
                        socket?.close()
                    }
                }
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.FAILED
                _error.value = "SmartConfig failed: ${e.message}"
                Log.e("SmartConfigViewModel", "SmartConfig failed", e)
            } finally {
                _isConnecting.value = false
            }
        }
    }

    fun cancelSmartConfig() {
        viewModelScope.launch {
            try {
                socket?.close()
                _isConnecting.value = false
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            } catch (e: Exception) {
                Log.e("SmartConfigViewModel", "Error canceling SmartConfig", e)
            }
        }
    }

    private fun createSmartConfigPacket(ssid: String, password: String, deviceName: String): ByteArray {
        val data = "{\"ssid\":\"$ssid\",\"password\":\"$password\",\"deviceName\":\"$deviceName\"}"
        return data.toByteArray(StandardCharsets.UTF_8)
    }

    override fun onCleared() {
        super.onCleared()
        socket?.close()
    }

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        FAILED,
        TIMEOUT
    }
}