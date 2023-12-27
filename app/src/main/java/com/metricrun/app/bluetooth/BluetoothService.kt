package com.metricrun.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import java.util.*

class BluetoothService(private val context: Context) {
    companion object {
        private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val CHARACTERISTIC_UUID_A0 = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val CHARACTERISTIC_UUID_A1 = UUID.fromString("beb5483f-36e1-4688-b7f5-ea07361b26a9")
        private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    var devicesList = ArrayList<String>()
    var devices = mutableListOf<BluetoothDevice>()
    var arrayAdapter: ArrayAdapter<String> = ArrayAdapter(context, android.R.layout.simple_list_item_1, devicesList)
    private var bluetoothGatt: BluetoothGatt? = null

    // Interfaz para notificar cambios en las características
    interface CharacteristicChangeListener {
        fun onCharacteristicChanged(characteristicUuid: UUID, value: ByteArray)
    }

    // Variable para mantener una referencia al listener
    private var characteristicChangeListener: CharacteristicChangeListener? = null

    // Método para establecer el listener
    fun setCharacteristicChangeListener(listener: CharacteristicChangeListener) {
        characteristicChangeListener = listener
    }

    @SuppressLint("MissingPermission")
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action!!
            Log.d("BluetoothReceiver", "Action received: $action")
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    Log.d("BluetoothReceiver", "Device found: ${device.name} - ${device.address}")
                    if (device.name == "METRICRUN") {
                        devices.add(device)
                        devicesList.add("${device.name}\n${device.address}")
                        arrayAdapter.notifyDataSetChanged()
                        Log.d("BluetoothReceiver", "METRICRUN device added: ${device.name} - ${device.address}")
                    }
                }
            }
        }
    }

    init {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth is not supported on this device.", Toast.LENGTH_SHORT).show()
        } else {
            context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            if (!bluetoothAdapter!!.isEnabled) {
                // Prompt user to turn on Bluetooth
            } else {
                startBluetoothDiscovery()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startBluetoothDiscovery() {
        if (!bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter!!.startDiscovery()
            Log.d("BluetoothService", "Starting Bluetooth discovery")
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BluetoothGatt", "Connected to GATT server.")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("BluetoothGatt", "Disconnected from GATT server.")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                val characteristicA0 = service?.getCharacteristic(CHARACTERISTIC_UUID_A0)
                val characteristicA1 = service?.getCharacteristic(CHARACTERISTIC_UUID_A1)

                characteristicA0?.let { subscribeToCharacteristic(gatt, it) }
                characteristicA1?.let { subscribeToCharacteristic(gatt, it) }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Log.d("BluetoothGatt", "Characteristic changed: ${characteristic.uuid}")
            characteristicChangeListener?.onCharacteristicChanged(characteristic.uuid, characteristic.value)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            // Handle characteristic read
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeToCharacteristic(bluetoothGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
            bluetoothGatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    fun cleanup() {
        bluetoothGatt?.close()
        context.unregisterReceiver(receiver)
    }
}
