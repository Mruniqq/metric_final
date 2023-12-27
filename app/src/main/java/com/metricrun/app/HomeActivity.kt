package com.metricrun.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class HomeActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 101
    }
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var listView: ListView
    private val devicesList = ArrayList<String>()
    private val devices = mutableListOf<BluetoothDevice>()
    private val arrayAdapter: ArrayAdapter<String> by lazy {
        ArrayAdapter(this, android.R.layout.simple_list_item_1, devicesList)
    }
    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val CHARACTERISTIC_UUID_A0 = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val CHARACTERISTIC_UUID_A1 = UUID.fromString("beb5483f-36e1-4688-b7f5-ea07361b26a9")
    private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private lateinit var tvA0: TextView
    private lateinit var tvA1: TextView
    private var readingCountA0 = 0
    private var readingCountA1 = 0
    private val readingDataListA0 = mutableListOf<JSONObject>()
    private val readingDataListA1 = mutableListOf<JSONObject>()
    private var lastReadingA0: Long? = null
    private var lastReadingA1: Long? = null
    private val readingDataList = mutableListOf<JSONObject>()
    private var readingCount = 0
    private var connectedDeviceMacAddress: String? = null
    private lateinit var avgApintTextView: TextView
    private lateinit var avgApextTextView: TextView
    private lateinit var avgApintMin: TextView
    private lateinit var avgApextMin: TextView
    private lateinit var avgApintMax: TextView
    private lateinit var avgApextMax: TextView
    private lateinit var avgApintPbajo: TextView
    private lateinit var avgApextPbajo: TextView
    private lateinit var avgApintPalto: TextView
    private lateinit var avgApextPalto: TextView


    fun updateA0Value(hexValue: String) {
        val decimalValue = hexValue.toLongOrNull(16) ?: 0
        tvA0.text = "Zona1: $decimalValue"
    }

    fun updateA1Value(hexValue: String) {
        val decimalValue = hexValue.toLongOrNull(16) ?: 0
        tvA1.text = "Zona2: $decimalValue"
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action!!
            Log.d("BluetoothReceiver", "Acción recibida: $action")
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                Log.d("BluetoothReceiver", "Dispositivo encontrado: ${device.name} - ${device.address}")
                if (device.name == "METRICRUN") {
                    devices.add(device)
                    devicesList.add("${device.name}\n${device.address}")
                    arrayAdapter.notifyDataSetChanged()
                    Log.d("BluetoothReceiver", "Dispositivo METRICRUN añadido: ${device.name} - ${device.address}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        avgApintTextView = findViewById(R.id.avgApint) // Asegúrate de usar el ID correcto
        avgApextTextView = findViewById(R.id.avgApext)
        avgApintMin = findViewById(R.id.minApint)
        avgApextMin = findViewById(R.id.minApext)
        avgApintMax = findViewById(R.id.maxApint)
        avgApextMax = findViewById(R.id.maxApext)
        avgApintPbajo = findViewById(R.id.apintpromediobajo)
        avgApextPbajo = findViewById(R.id.apextpromediobajo)
        avgApintPalto = findViewById(R.id.apintpromedioalto)
        avgApextPalto = findViewById(R.id.apextpromedioalto)// Asegúrate de usar el ID correcto
        Log.d("MainActivity", "onCreate iniciado")

        // Inicializar bluetoothAdapter primero
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth.", Toast.LENGTH_SHORT).show()
            return  // Finalizar onCreate si el dispositivo no tiene Bluetooth
        }

        // Ahora es seguro llamar a checkPermissions, ya que bluetoothAdapter está inicializado
        checkPermissions()

        // Inicialización de los componentes de la UI
        tvA0 = findViewById(R.id.tvA0)
        tvA1 = findViewById(R.id.tvA1)
        listView = findViewById(R.id.listView)
        listView.adapter = arrayAdapter
        listView.setOnItemClickListener { _, _, position, _ ->
            Log.d("MainActivity", "Dispositivo seleccionado: ${devices[position]}")
            connectToDevice(devices[position])
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        startBluetoothDiscovery()

    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothDiscovery() {
        if (!bluetoothAdapter.isEnabled) {
            Log.d("MainActivity", "Bluetooth no está habilitado")
            // Solicitar al usuario que active el Bluetooth
        } else if (!bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.startDiscovery()
            Log.d("MainActivity", "Iniciando descubrimiento de Bluetooth")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        Log.d("MainActivity", "onDestroy: Receiver de Bluetooth desregistrado")
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        connectedDeviceMacAddress = device.address
        val gattCallback = object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("BluetoothGatt", "Conectado al GATT server.")
                    Log.d("BluetoothGatt", "Intentando iniciar el descubrimiento de servicios: " + gatt.discoverServices())
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BluetoothGatt", "Desconectado del GATT server.")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(SERVICE_UUID)
                    if (service != null) {
                        Log.d("BluetoothGattCallback", "Servicio encontrado: $SERVICE_UUID")

                        val characteristicA0 = service.getCharacteristic(CHARACTERISTIC_UUID_A0)
                        val characteristicA1 = service.getCharacteristic(CHARACTERISTIC_UUID_A1)

                        if (characteristicA0 != null) {
                            subscribeToCharacteristic(gatt, characteristicA0)
                            Log.d("BluetoothGattCallback", "Suscribiéndose a A0: ${CHARACTERISTIC_UUID_A0}")
                        } else {
                            Log.d("BluetoothGattCallback", "Característica A0 no encontrada: ${CHARACTERISTIC_UUID_A0}")
                        }

                        if (characteristicA1 != null) {
                            subscribeToCharacteristic(gatt, characteristicA1)
                            Log.d("BluetoothGattCallback", "Suscribiéndose a A1: ${CHARACTERISTIC_UUID_A1}")
                        } else {
                            Log.d("BluetoothGattCallback", "Característica A1 no encontrada: ${CHARACTERISTIC_UUID_A1}")
                        }
                    } else {
                        Log.d("BluetoothGattCallback", "Servicio no encontrado: $SERVICE_UUID")
                    }
                } else {
                    Log.w("BluetoothGattCallback", "onServicesDiscovered recibido: $status")
                }
            }
            @SuppressLint("MissingPermission")
            private fun readCharacteristicA1(bluetoothGatt: BluetoothGatt) {
                val service = bluetoothGatt.getService(SERVICE_UUID)
                service?.let {
                    val characteristicA1 = it.getCharacteristic(CHARACTERISTIC_UUID_A1)
                    characteristicA1?.let {
                        bluetoothGatt.readCharacteristic(characteristicA1)
                    }
                }
            }


            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val hexValue = characteristic.value.joinToString(separator = "") { byte -> "%02x".format(byte) }
                val decimalValue = hexValue.toLongOrNull(16) ?: 0

                if (characteristic.uuid == CHARACTERISTIC_UUID_A0) {
                    runOnUiThread { updateA0Value(hexValue) }
                    accumulateAndSendReadings("A0", decimalValue) // Acumula y prepara para enviar A0
                    readCharacteristicA1(gatt) // Leer A1 manualmente
                }
            }



            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (characteristic.uuid == CHARACTERISTIC_UUID_A1) {
                        val hexValue = characteristic.value.joinToString(separator = "") { byte -> "%02x".format(byte) }
                        val decimalValue = hexValue.toLongOrNull(16) ?: 0
                        runOnUiThread { updateA1Value(hexValue) }
                        accumulateAndSendReadings("A1", decimalValue) // Acumula y prepara para enviar A1
                    }
                }
            }



            override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                super.onDescriptorWrite(gatt, descriptor, status)

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIG) {
                        val characteristicUuid = descriptor.characteristic.uuid
                        Log.d("BluetoothGattCallback", "Notificación habilitada para $characteristicUuid")
                    }
                } else {
                    Log.e("BluetoothGattCallback", "Error al escribir el descriptor: $status")
                }
            }

            // Implementa otros métodos de callback necesarios como onCharacteristicRead, etc.
        }

        device.connectGatt(this, false, gattCallback)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_PERMISSIONS)
            Log.d("MainActivity", "Solicitando permisos de ubicación")
        } else {
            // Permiso ya otorgado, puedes continuar con la búsqueda de dispositivos Bluetooth
            Log.d("MainActivity", "Permisos de ubicación ya otorgados")
            startBluetoothDiscovery()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d("MainActivity", "Permiso de ubicación concedido")
                    startBluetoothDiscovery()
                } else {
                    Log.d("MainActivity", "Permiso de ubicación denegado")
                    // Permiso denegado. Debes manejar esta situación en tu aplicación
                }
                return
            }
            else -> {
                // Ignorar las otras solicitudes de permiso
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun subscribeToCharacteristic(bluetoothGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
            val notificationSet = bluetoothGatt.setCharacteristicNotification(characteristic, true)
            Log.d("BluetoothGattCallback", "Notificación establecida para ${characteristic.uuid}: $notificationSet")

            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val writeDescriptorSuccess = bluetoothGatt.writeDescriptor(descriptor)
                Log.d("BluetoothGattCallback", "Escribiendo descriptor para ${characteristic.uuid}: $writeDescriptorSuccess")
            } else {
                Log.d("BluetoothGattCallback", "Descriptor no encontrado para ${characteristic.uuid}")
            }
        } else {
            Log.d("BluetoothGattCallback", "La característica ${characteristic.uuid} no soporta notificaciones")
        }
    }
    private fun accumulateAndSendReadings(type: String, value: Long) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDateTime = sdf.format(Date())
        if (type == "A0") {
            lastReadingA0 = value
        } else if (type == "A1") {
            lastReadingA1 = value
        }

        // Comprobar si tenemos ambas lecturas
        if (lastReadingA0 != null && lastReadingA1 != null) {
            val readingData = JSONObject().apply {
                put("macAddress", connectedDeviceMacAddress!!)
                put("apint", lastReadingA0!!)
                put("apext", lastReadingA1!!)
                put("talon", 0) // Valor de ejemplo
                put("datetime", formattedDateTime)
                put("user", "usuario") // Valor de ejemplo
            }

            readingDataList.add(readingData)
            readingCount++

            // Resetear las lecturas para la próxima combinación
            lastReadingA0 = null
            lastReadingA1 = null

            if (readingCount >= 20) {
                val readingsToSend = ArrayList(readingDataList) // Crear una copia de la lista
                Log.d("AccumulateReadings", "Enviando lecturas combinadas: ${JSONArray(readingsToSend).toString()}")
                sendReadingsToServer(readingsToSend)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getAndDisplayAverages()
                }
                readingDataList.clear()
                readingCount = 0
            }

        }
    }


    fun sendReadingsToServer(readings: List<JSONObject>) {
        val thread = Thread {
            try {
                val url = URL("http://54.221.216.132/metricrun/update.php")
                val readingsArray = JSONArray(readings)
                Log.d("SendReadings", "Enviando lecturas: ${readingsArray.toString()}")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")

                    outputStream.use { os ->
                        val input = readingsArray.toString().toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val responseCode = responseCode
                    val responseMessage = responseMessage

                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        Log.e("SendReadings", "Error en la respuesta del servidor. Código: $responseCode, Mensaje: $responseMessage")
                        errorStream?.bufferedReader()?.use {
                            val errorResponse = it.readText()
                            Log.e("SendReadings", "Detalle del error del servidor: $errorResponse")
                        }
                    } else {
                        inputStream.bufferedReader().use {
                            val response = it.readText()
                            Log.d("SendReadings", "Respuesta del servidor: $response")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SendReadings", "Excepción al enviar lecturas: ${e.message}")
                e.printStackTrace()
            }
        }
        thread.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getAndDisplayAverages() {
        if (connectedDeviceMacAddress == null) return

        val thread = Thread {
            try {
                val url = URL("http://54.221.216.132/metricrun/get_data.php?macAddress=$connectedDeviceMacAddress")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"

                    inputStream.bufferedReader().use {
                        val response = it.readText()
                        val jsonResponse = JSONObject(response)

                        val averageApint = jsonResponse.getDouble("average_apint")
                        val averageApext = jsonResponse.getDouble("average_apext")
                        val minApint = jsonResponse.getDouble("min_apint")
                        val minApext = jsonResponse.getDouble("min_apext")
                        val maxApint = jsonResponse.getDouble("max_apint")
                        val maxApext = jsonResponse.getDouble("max_apext")
                        val PbajoApint = jsonResponse.getDouble("min_average_apint")
                        val PbajoApext = jsonResponse.getDouble("min_average_apext")
                        val PaltoApint = jsonResponse.getDouble("max_average_apint")
                        val PaltoApext = jsonResponse.getDouble("max_average_apext")

                        runOnUiThread {
                            avgApintTextView.text = "Avg Apint: $averageApint"
                            avgApextTextView.text = "Avg Apext: $averageApext"
                            avgApintMin.text = "APint Min: $minApint"
                            avgApextMin.text = "Apext Min: $minApext"
                            avgApintMax.text = "APint Max: $maxApint"
                            avgApextMax.text = "APext Max: $maxApext"
                            avgApintPbajo.text = "Promedio APint Bajo: $PbajoApint"
                            avgApextPbajo.text = "Promedio APext Bajo: $PbajoApext"
                            avgApintPalto.text = "Promedio APint Alto: $PaltoApint"
                            avgApextPalto.text = "Promedio APext Alto: $PaltoApext"

                            val progressBar: ProgressBar = findViewById(R.id.progressBarApint)
                            val progressText: TextView = findViewById(R.id.progressText)

                            progressBar.max = 5000 // Máximo valor de la barra de progreso
                            progressBar.min = 1000 // Mínimo valor de la barra de progreso (API nivel 26+)

                            // Establecer el progreso actual en función del valor promedio alto
                            progressBar.progress = PaltoApint.toInt()

                            // Calcular el porcentaje y actualizar el TextView
                            val percentage = ((PaltoApint - 1000) / (5000 - 1000) * 100).toInt()
                            progressText.text = "$percentage%"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
    }



}