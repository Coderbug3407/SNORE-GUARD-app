package com.example.snoreguard.ui.setting

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.snoreguard.R
import com.example.snoreguard.databinding.FragmentSettingBinding
import android.text.TextWatcher
import android.text.Editable

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SmartConfigViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            scanWifiNetworks()
        } else {
            Toast.makeText(
                requireContext(),
                "Location permission is required to scan for WiFi networks",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SmartConfigViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // WiFi network dropdown
        binding.autoCompleteWifiNetwork.setOnItemClickListener { _, _, _, _ ->
            updateConnectButtonState()
        }

        // Password text change listener
        binding.etWifiPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateConnectButtonState()
            }
        })

        // Scan button
        binding.btnScanWifi.setOnClickListener {
            checkPermissionsAndScan()
        }

        // Connect button
        binding.btnConnect.setOnClickListener {
            connectToDevice()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            viewModel.cancelSmartConfig()
            binding.btnCancel.visibility = View.GONE
            binding.btnConnect.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }

        // Initially disable connect button
        binding.btnConnect.isEnabled = false
    }

    private fun observeViewModel() {
        viewModel.availableSSIDs.observe(viewLifecycleOwner) { ssids ->
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_item,
                ssids
            )
            adapter.setDropDownViewResource(R.layout.spinner_item)
            binding.autoCompleteWifiNetwork.setAdapter(adapter)

            // Enable dropdown after scan
            binding.autoCompleteWifiNetwork.isEnabled = true
        }

        viewModel.isConnecting.observe(viewLifecycleOwner) { isConnecting ->
            if (isConnecting) {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnConnect.visibility = View.GONE
                binding.btnCancel.visibility = View.VISIBLE
                binding.autoCompleteWifiNetwork.isEnabled = false
                binding.etWifiPassword.isEnabled = false
            } else {
                binding.progressBar.visibility = View.GONE
                binding.btnConnect.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.GONE
                binding.autoCompleteWifiNetwork.isEnabled = true
                binding.etWifiPassword.isEnabled = true
            }
        }

        viewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                SmartConfigViewModel.ConnectionStatus.DISCONNECTED -> {
                    binding.tvConnectionStatus.text = "Disconnected"
                    binding.tvConnectionStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_secondary)
                    )
                }
                SmartConfigViewModel.ConnectionStatus.CONNECTING -> {
                    binding.tvConnectionStatus.text = "Connecting..."
                    binding.tvConnectionStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.primary_purple)
                    )
                }
                SmartConfigViewModel.ConnectionStatus.CONNECTED -> {
                    binding.tvConnectionStatus.text = "Connected"
                    binding.tvConnectionStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.snoring_quiet)
                    )
                    Toast.makeText(
                        requireContext(),
                        "Successfully connected to device!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                SmartConfigViewModel.ConnectionStatus.FAILED -> {
                    binding.tvConnectionStatus.text = "Connection Failed"
                    binding.tvConnectionStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.snoring_epic)
                    )
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkPermissionsAndScan() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            scanWifiNetworks()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun scanWifiNetworks() {
        binding.progressBar.visibility = View.VISIBLE
        binding.autoCompleteWifiNetwork.isEnabled = false

        viewModel.scanWifiNetworks(requireContext())

        // After a delay, hide progress even if scan fails
        binding.root.postDelayed({
            binding.progressBar.visibility = View.GONE
        }, 5000)
    }

    private fun connectToDevice() {
        val ssid = binding.autoCompleteWifiNetwork.text.toString()
        val password = binding.etWifiPassword.text.toString()
        val wifiManager = requireContext().getSystemService(WifiManager::class.java)
        val wifiInfo = wifiManager.connectionInfo
        val bssid = wifiInfo.bssid

        if (ssid.isNotEmpty() && bssid != null) {
            viewModel.startSmartConfig(requireContext(), ssid, bssid, password)
        } else {
            Toast.makeText(
                requireContext(),
                "Please select a WiFi network",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateConnectButtonState() {
        val ssid = binding.autoCompleteWifiNetwork.text.toString()
        val password = binding.etWifiPassword.text.toString()

        binding.btnConnect.isEnabled = ssid.isNotEmpty() && password.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}