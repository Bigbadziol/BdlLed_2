

package com.badziol.bdlled_02

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bdlled_02.*
import com.example.bdlled_02.databinding.ActivityStartBinding
import com.google.android.material.snackbar.Snackbar

const val TAG = "DEBUG"
val MY_DEVICE_PREFIX = arrayOf("LEDS_","LEDP_")

data class DeviceItem(
    val name: String?,
    val address: String?,
    var isConnected: Boolean

)


class StartActivity : AppCompatActivity() {

    companion object {
        var DEVICE_LIST : ArrayList<BluetoothDevice> = ArrayList()
        var PAIRED_DEVICES_SELECTED = 0
    }

    private lateinit var bind : ActivityStartBinding
    private lateinit var brNewDevices: BroadcastReceiver
    private lateinit var filterNewDevices : IntentFilter

    private lateinit var brBondDevice: BroadcastReceiver
    private lateinit var filterBondDevice : IntentFilter

    private lateinit var  bluetoothManager : BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var pairedDevices: MutableSet<BluetoothDevice>  //from adapter
    private lateinit var pairedDeviceList: ArrayList<DeviceListModel>// for listview

    private lateinit var newDeviceList: ArrayList<DeviceListModel>// for listview

    private var requestBluetoothEnable = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "request Bluetooth enable, granted or is enabled")
        }else{
            Log.d(TAG, "request BlueTooth enable , danny")
        }
        buildInterface() //rebuild interface
    }

    //android 12+
    private val requestBluetoothPermissionsApiS =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d(TAG,"->Result for android 12+ permissions:")
            checkPermissions()
            buildInterface() //rebuild interface
        }
    //android 11 or less
    private var requestPermissionLocation =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            //less than android 12
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                if (granted) {
                    //INTERFEJS CZY GOTOWY
                    Log.d(TAG, "Permission : ACCESS_FINE_LOCATION  granted by contract 1")

                } else {
                    Log.d(TAG, "Permission : ACCESS_FINE_LOCATION denied by contract 1")
                    val builder = AlertDialog.Builder(this@StartActivity)
                    builder.setTitle("- ACCESS_FINE_LOCATION - ")
                    builder.setMessage("I need these permissions to work with Bt devices ")
                    builder.setPositiveButton("YES") { _, _ ->
                        requestPermissionLocationSecond.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    builder.setNegativeButton("No") { _, _ ->
                        Toast.makeText(this, "Sorry boss cant work!", Toast.LENGTH_SHORT).show()
                    }
                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
                buildInterface() //rebuild interface
            }
        }
    //android 11 or less
    private val requestPermissionLocationSecond =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                if (granted) {
                    Log.d(TAG, "Permission : ACCESS_FINE_LOCATION granted by contract 2")
                } else {
                    val builder = AlertDialog.Builder(this@StartActivity)
                    builder.setTitle("- ACCESS_FINE_LOCATION 2 -\n You blocked me twice :(")
                    builder.setMessage(
                        " Please go to the app settings and manually turn on " +
                                "\"location permission\". Without this permission," +
                                " I do not work."+
                                "Second solution is reinstall application and give this permission."
                    )
                    builder.setPositiveButton("Ok") { _, _ -> }
                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                    Log.d(TAG, " V2-> Permission : ACCESS_FINE_LOCATION denied, - contract 2")
                }
                buildInterface() //rebuild interface
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        bind = ActivityStartBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(bind.root)
        iamRunningOnInfo()

        bluetoothManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        pairedDeviceList = ArrayList()
        newDeviceList = ArrayList()

        //IF bt is enabled
        //Potencjalny problem , raz wlacza raz nie, coś tam na Stack
        Log.d(TAG,"Is bluetooth enabled : ${bluetoothAdapter.isEnabled}")
        if (bluetoothAdapter.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetoothEnable.launch(enableBtIntent)
        }

        if (!checkPermissions()) {
            //if got bt permissions for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestBluetoothPermissionsApiS.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            }

            // if got ACCESS_FINE_LOCATION permissions for Android 11 or less (required)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                requestPermissionLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        bind.lvPairedDevices.setOnItemClickListener { parent, view, position, id ->
            Log.d(TAG,"Paired devices clicked : $position")
        }
        bind.lvNewDevices.setOnItemClickListener { parent, view, position, id ->
            Log.d(TAG,"New devices clicked : $position")
        }

        bind.btnNext.setOnClickListener { it ->
            Log.d(TAG,"Action next... ")
            @SuppressLint("MissingPermission")
            if (gotBtPerms(this,"[NEXT]")){
                DEVICE_LIST.clear()
                pairedDevices.forEach {
                    if (itIsMyDevice(it.name)){
                        DEVICE_LIST.add(it)
                        Log.d(TAG, "Pushed device : ${it.name}")
                    }
                }

                if (DEVICE_LIST.count() > 0) {
                    Log.d(TAG, "Push : ${DEVICE_LIST.count()} devices to next acitivity ")
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("START_DEVICE_LIST", DEVICE_LIST)
                    intent.putExtra("START_CURRENT_SELECTED", PAIRED_DEVICES_SELECTED)
                    startActivity(intent)
                    finish()
                }else{
                    val info = Snackbar.make(it,getString(R.string.NO_PAIRED_DEVICES), Snackbar.LENGTH_SHORT)
                    info.show()
                }
            }
        }

        bind.btnFind.setOnClickListener { it->
            Log.d(TAG,"Action find... ")
            //newDevices.clear()
            newDeviceList.clear()
            @SuppressLint("MissingPermission")
            if (gotBtPerms(this,"FIND")) {
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                    bluetoothAdapter.startDiscovery()
                    Log.d(TAG,"Discovering stopped and started again.")
                    bind.btnFind.text = getString(R.string.btnFind2)
                } else {
                    bluetoothAdapter.startDiscovery()
                    Log.d(TAG,"Discovering started.")
                    bind.btnFind.text = getString(R.string.btnFind2)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG,"Discovering stopped.")
                    bluetoothAdapter.cancelDiscovery()
                    bind.btnFind.text = getString(R.string.btnFind)
                }, 20000L)
            }
        }

        buildInterface()


    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        if (gotBtPerms(this,"[START ACTIVITY][BT] OnDestroy")){
            if (bluetoothAdapter.isDiscovering){
                bluetoothAdapter.cancelDiscovery()
            }
            if (this::brNewDevices.isInitialized) {
                try {
                    this.unregisterReceiver(brNewDevices)
                    Log.d(TAG,"Try unregister brNewDevices - success!")
                } catch (e: IllegalArgumentException) {
                    Log.d(TAG,"Try unregister brNewDevices ERROR")
                }
            }
            if (this::brBondDevice.isInitialized) {
                try {
                    this.unregisterReceiver(brBondDevice)
                    Log.d(TAG,"Try unregister brBondDevice - success!")
                } catch (e: IllegalArgumentException) {
                    Log.d(TAG,"Try unregister brBondDevice ERROR")
                }
            }
        }
    }

    private fun iamRunningOnInfo(){
        var gradleVersion  = "version code : " +BuildConfig.VERSION_CODE
        gradleVersion += " version name : " + BuildConfig.VERSION_NAME
        Log.d(TAG,"USING SDK: ${Build.VERSION.SDK_INT} -> ${Build.VERSION.CODENAME}")
        //Log.d(TAG,"CURRENT DEBUG -> $gradleVersion")
        Log.d(TAG,"----------------")
    }

    /*
        Check if i got all needed permission
     */
    private fun checkPermissions(): Boolean {
        var gotAllPerms = true
        val permissionsRequired =
            if (Build.VERSION.SDK_INT <= 30) { //android 11 or less
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            }
        var permNum = 0
        Log.d(TAG,"Self test - permissions , testing ${permissionsRequired.size} entries")
        permissionsRequired.forEach { requiredPermission ->
            permNum++
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    requiredPermission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "$permNum ) $requiredPermission -> IS GRANTED")
            } else {
                Log.d(TAG, "$permNum ) $requiredPermission -> NOT GRANTED")
                gotAllPerms = false
            }
        }
        Log.d(TAG, "Result of self check permissions : $gotAllPerms")
        return gotAllPerms
    }
    /*
        Check required BT permissions for android 11 or less and Android 12+
     */
    private fun gotBtPerms(context: Context, errorMessage : String) : Boolean{
        var gotPerm  = true
        //Android 11 or less
        if (Build.VERSION.SDK_INT <=30) {

            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage - no  permission (API) <=30 , BLUETOOTH")
            }

            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage - no  permission (API) <=30 , BLUETOOTH_ADMIN")
            }
        }
        //Android 12+
        if (Build.VERSION.SDK_INT >=31) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage  - no  permission (API) >= 31 , BLUETOOTH_SCAN ")
            }

            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage  - no  permission (API) >= 31 , BLUETOOTH_CONNECT ")
            }
        }

        return gotPerm
    }

    /*
    Build interface...
 */
    private fun buildInterface(){
        Log.d(TAG,"Build interface test.")
        if (checkPermissions() && bluetoothAdapter.isEnabled ){
            Log.d(TAG, "Build interface , ok")
            bind.tvWelcome.text = getString(R.string.tvWelcomeOk)
            getPairedDevices()

            initBrNewDevices()
            filterNewDevices = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(brNewDevices, filterNewDevices)

            initBrBoundDevice()
            filterBondDevice = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            registerReceiver(brBondDevice, filterBondDevice)

            bind.btnFind.isEnabled = true
            bind.btnNext.isEnabled = btnNextStatus()
        }else{
            if (!bluetoothAdapter.isEnabled && !checkPermissions()){
                bind.tvWelcome.text = getString(R.string.tvWelcomeErr_BtPerms)
                Log.d(TAG, "Build interface , errors : bt disabled, no perms")
            }else if (!bluetoothAdapter.isEnabled){
                bind.tvWelcome.text= getString(R.string.tvWelcomeErr_Bt)
                Log.d(TAG, "Build interface , errors : bt disabled")
            }else{
                bind.tvWelcome.text=getString(R.string.tvWelcomeErr_Perms)
                Log.d(TAG, "Build interface , errors :  no perms")
            }
            bind.btnFind.isEnabled = false
            bind.btnNext.isEnabled = false
        }
    }


    /*
       Only debug info , all paired or discovered devices
     */
    private fun displayDeviceDetails(device: DeviceItem) {
        val prefix = if (device.isConnected) "[C]" else "[D]"
        val info=  "$prefix Bluetooth device found: ${device.name}  address: ${device.address} ."
        Log.d(TAG, info)
    }

    /*
        Get list currently bonded devices, check if name contains LEDS_ or LEDP_ prefix
        gotBtPerms() function check required permissions
     */
    @SuppressLint("MissingPermission")
    private  fun getPairedDevices(){
        Log.d(TAG,"Getting paired devices...")
        if (!gotBtPerms(this,"[START ACTIVITY][BT] getPairedDevices")){
            Log.d(TAG,"fun : getPairedDevices - fatal error")
            return
        }
        pairedDevices = bluetoothAdapter.bondedDevices
        var icon : Int
        var myDevices = 0
        pairedDeviceList.clear()

        pairedDevices.forEach {
            displayDeviceDetails(DeviceItem(it.name, it.address, true)) //DEBUG LOG
            if (it.name.contains("LEDS_") || it.name.contains("LEDP_")) {
                icon = if (it.name.contains("LEDS_")) R.drawable.icon_strip //ikonka listwy
                else R.drawable.icon_panel //ikonka ekranu
                pairedDeviceList.add(DeviceListModel(it.name, it.address, icon))
                myDevices++
            }
        }
        if (myDevices == 0){
            pairedDeviceList.add(
                DeviceListModel(
                    getString(R.string.NO_DEVICE_NAME),
                    getString(R.string.NO_DEVICE_NAME),
                    R.drawable.icon_no_devices
                )
            )
        }

        bind.lvPairedDevices.adapter =  DeviceListAdapter(this,
            pairedDeviceList,
            bluetoothAdapter,
            DEVICE_ACTION_TYPE.REMOVE)
    }

    /*
        Init broadcast receiver for new devices. If new device found with name prefix
        LEDS_ or LEDP_ add data to list :  newDeviceList
     */
    private fun initBrNewDevices(){
        Log.d(TAG,"Init : broadcast receiver for new devices.")
        brNewDevices = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (device == null) return
                        if (device.name.isNullOrEmpty()) return
                        if (device.bondState != BluetoothDevice.BOND_NONE) return
                        if (itIsMyDevice(device.name) == false) return //check prefixes LEDS_ ,LEDP_
                         newDeviceList.forEach { //already on list
                            if (it.deviceAdress.contentEquals(device.address))  return
                        }
                        Log.d(TAG, "Not bonded -> ${device.name}")
                        val icon: Int =
                            if (device.name.contains("LEDS_")) R.drawable.icon_strip //led strip icon
                            else R.drawable.icon_panel //led screen icon
                        newDeviceList.add(
                            DeviceListModel(
                                device.name,
                                device.address,
                                icon
                            )
                        )
                        bind.lvNewDevices.adapter = DeviceListAdapter(
                            this@StartActivity,
                            newDeviceList,
                            bluetoothAdapter,
                            DEVICE_ACTION_TYPE.ADD
                        )
                    } //ACTION_FOUND
                }//when intent
            }//on Receive
        }//broadcast receiver
    }
    /*
        Init broadcast receiver for bonding device
     */
    private fun initBrBoundDevice(){
        Log.d(TAG,"Init : broadcast reciver for bound devices.")
        brBondDevice = object : BroadcastReceiver(){
            //gotBtPermToScan is checking scan permission for different apis
            @SuppressLint("MissingPermission")
            override fun onReceive(context : Context?, intent: Intent?) {
                val action = intent?.action
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action){
                    val thisDevice: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    context?.let {
                        if (!gotBtPerms(context,"[START ACTIVITY][BT] initBrBoundDevice")){
                            Log.d(TAG,"fun : initBrBoundDevice - fatal error")
                            return
                        }
                    }

                    when (thisDevice?.bondState){
                        BluetoothDevice.BOND_NONE ->{
                            Log.d(TAG,"Device : ${thisDevice.name}  state -> BOND_NONE")
                            getPairedDevices()
                        }
                        BluetoothDevice.BOND_BONDING ->{
                            Log.d(TAG,"Device : ${thisDevice.name}  state -> BOND_BONDING")
                        }
                        BluetoothDevice.BOND_BONDED ->{
                            Log.d(TAG,"Device : ${thisDevice.name}  state -> BOND_BONDED")
                            getPairedDevices()//get paired devices again
                            //na okolo ale pieprzy mi sie mapowanie DeviceListModel-> Bluetooth device
                            // , zatem , robimy to troszkę na około
                            var tmpIcon = 0
                            if (thisDevice.name.contains("LEDS_")) tmpIcon = R.drawable.icon_strip
                            else if (thisDevice.name.contains("LEDP_")) tmpIcon =
                                R.drawable.icon_panel
                            val  dmDevice = DeviceListModel(thisDevice.name,thisDevice.address,tmpIcon)
                            val c = newDeviceList.contains(dmDevice)
                            val i = newDeviceList.indexOf(dmDevice)
                            Log.d(TAG,"Broadcast reciver (bond) looking for device, then remove it: $c -> $i")
                            newDeviceList.remove(dmDevice)
                        }
                    }
                }
            }
        }
    }
    /*
        Return true if on BT paired device list is at least one with name prefix LEDP_ or LEDS_
        else return false
     */
    @SuppressLint("MissingPermission")
    private fun btnNextStatus():Boolean{
        if (!gotBtPerms(this,"[START ACTIVITY][BT] btnNextStatus")){
            Log.d(TAG,"fun : initBrBoundDevice - fatal error")
            return false
        }
        pairedDevices.forEach {
            if (!it.name.isNullOrEmpty()){
                if (it.name.contains("LEDP_") || (it.name.contains("LEDS_"))){
                    return true
                }
            }
        }
        return false
    }
    /*
        Check if new device contain prefix defined in MY_DEVICE_PREFIX
     */
    private fun itIsMyDevice(name : String) : Boolean {
        if (name.isNullOrBlank()) return false
        for (elem in MY_DEVICE_PREFIX){
            if (name.contains(elem)) return true
        }
        return false
    }
}
