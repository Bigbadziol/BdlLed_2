/*
//onCreate
       if (bluetoothAdapter.isEnabled){
            Log.d(TAG,"Bt is enabled.")
        }else{
            Log.d(TAG,"Bt is NOT enabled")
        }

        // na pale 11 i starszy
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R){
            requestBtPermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN))

        }
        //na pale android 12 i nowszy
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBtPermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT))
        }

        //na pale wlaczenie
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestBluetoothEnable.launch(enableBtIntent)

 */
//Zajebisty problem po aktualizacji z dnia 26.01.2022
//STANDARD_1 26_01_2022
//_TODO: Consider calling
//ActivityCompat#requestPermissions
//here to request the missing permissions, and then overriding
//public void onRequestPermissionsResult(int requestCode, String[] permissions,
//int[] grantResults)
//to handle the case where the user grants the permission. See the documentation
//for ActivityCompat#requestPermissions for more details.
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
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.size
import com.example.bdlled_02.BuildConfig
import com.example.bdlled_02.DeviceListAdapter
import com.example.bdlled_02.DeviceListModel
import com.example.bdlled_02.R
import com.example.bdlled_02.databinding.ActivityStartBinding
import com.google.android.material.snackbar.Snackbar

const val TAG = "DEBUG"
const val NO_DEVICE_NAME = "BRAK"
const val NO_DEVICE_ADDRESS = "urządzeń na liście"
//const val DEVICE_PREFIX_1 ="LEDS_"
//const val DEVICE_PREFIX_2 ="LEDP_"
//przetestowac prefixy raz jeszcze , wszystkie stałe do globalnego pliku ?? - przemyslec
//taka inna sygnatura
data class DeviceItem(
    val name: String?,
    val address: String?,
    var isConnected: Boolean

)
data class Conditions (var isBtEnabled : Boolean , var permissionsOk :Boolean){
    fun isReady():Boolean{
        if (isBtEnabled && permissionsOk) return true
        else return false
    }
    fun log(){
        Log.d(TAG,"Conditions-> bt enabled : $isBtEnabled , permissions : $permissionsOk")
    }
}

class StartActivity : AppCompatActivity() {

    companion object {
        var DEVICE_LIST : ArrayList<BluetoothDevice> = ArrayList()
        var PAIRED_DEVICES_SELECTED = 0
    }

    private lateinit var bind : ActivityStartBinding
    private lateinit var brNewDevices: BroadcastReceiver //broad cast reciver for new devices
    private lateinit var brBondDevice: BroadcastReceiver
    private lateinit var filterNewDevices : IntentFilter
    private lateinit var filterBondDevice : IntentFilter
    private lateinit var  bluetoothAdapter : BluetoothAdapter

    private lateinit var pairedDeviceList: ArrayList<DeviceListModel>// for listview
    private lateinit var pairedDevices: MutableSet<BluetoothDevice>  //from adapter

    private lateinit var newDeviceList: ArrayList<DeviceListModel>// for listview
    private lateinit var newDevices: ArrayList<BluetoothDevice>  //return from reciver
    private lateinit var  bluetoothManager : BluetoothManager

    private var conditions = Conditions(false, false)

    //pozwolenia dla api >=31 (android 12+)
    private val requestBluetoothPermNew =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d(TAG, "${it.key} = ${it.value}")
            }
        }

   /*
   //requestBluetoothEnable - wersja przed kombinowaniem
    private var requestBluetoothEnable =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d(TAG,"==================================")
            Log.d(TAG, "IT RESULT CODE: ${it.resultCode}")
            //kiedy bt jest wlaczone , result -1 , kiedy wylaczone i wlaczamy i akceptujemy tez -1
            //a jak odrzucamy to 0

            if (it.resultCode == -1) {
                conditions.log()
                conditions.isBtEnabled = true
            }

            if (conditions.isReady()) {
                buildInterfaceOk()
            } else buildInterfaceError()
        }

    */
    //pozwolenia dla api <=30 (android 11 max i ponizej)
    private var requestBluetoothEnable = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //jesli jest wlaczone z automatu bedzie granted dla api 12
            Log.d(TAG,"Bt turn on - Granted")
            conditions.log()
            conditions.isBtEnabled = true
        }else{
            //deny
            Log.d(TAG,"Bt turn on - Deny")
        }
       if (conditions.isReady()) {
           buildInterfaceOk()
       } else buildInterfaceError()
    }

    //use it when user denied first time
    private val requestPermissionLocationSecond =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Log.d(TAG, "Permission granted by contract 2")
                conditions.permissionsOk = checkPermissions()
                if (conditions.isReady()) {
                    buildInterfaceOk()
                } else buildInterfaceError()
            } else {
                val builder = AlertDialog.Builder(this@StartActivity)
                builder.setTitle("V2 - Hi!")
                builder.setMessage(
                    " Please go to the app settings and manually turn on " +
                            "\"location permission\". Without this permission, I do not work. "
                )
                builder.setPositiveButton("Ok") { _, _ -> }
                val dialog: AlertDialog = builder.create()
                dialog.show()
                Log.d(TAG, " V2-> Permission denied, - contract 2")
            }
        }

    // first try to get permission
    private var requestPermissionLocation =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Log.d(TAG, "Permission granted by contract 1")
                conditions.permissionsOk = checkPermissions()
                if (conditions.isReady()) {
                    buildInterfaceOk()
                } else buildInterfaceError()
            } else {
                Log.d(TAG, "Permission denied by contract 1")
                val builder = AlertDialog.Builder(this@StartActivity)
                builder.setTitle("V2 - Uprawnienie do lokalizacji")
                builder.setMessage("I need these permissions to work with Bt devices ")
                builder.setPositiveButton("YES") { _, _ ->
                    requestPermissionLocationSecond.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                builder.setNegativeButton("No") { _, _ -> }
                val dialog: AlertDialog = builder.create()
                dialog.show()
                conditions.permissionsOk = checkPermissions()
                if (conditions.isReady()) {
                    buildInterfaceOk()
                } else buildInterfaceError()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        bind = ActivityStartBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(bind.root)
        iamRunningOnInfo() //witch API , current build

        pairedDeviceList = ArrayList()
        newDeviceList = ArrayList()
        newDevices = ArrayList()
        //TESTS : try to use bluetoothManager instead of  get DefaultAdapter
        //bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter()
        bluetoothManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        conditions.isBtEnabled = bluetoothAdapter.isEnabled
        conditions.permissionsOk = checkPermissions()

        Log.d(TAG, "FIRST conditions check :")
        Log.d(TAG,"${conditions}")
        if (conditions.isReady()) {
            conditions.log()
            buildInterfaceOk()
        }else{
            buildInterfaceError()
            //wersja orginalna
            //if(!conditions.isBtEnabled)  requestBluetoothEnable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            //nowa
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestBluetoothPermNew.launch(arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT))
            }
            else{
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetoothEnable.launch(enableBtIntent)
            }

            if(!conditions.permissionsOk)  requestPermissionLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    //gotBtPermToScan is checking scan permission for different apis
    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
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

        if (this.bluetoothAdapter.isDiscovering){
            if (gotBtPermToScan(this,"[START ACTIVITY][BT] OnDestroy")){
                bluetoothAdapter.cancelDiscovery()
            }else{
                Log.d(TAG, "fun : OnDestroy - fatal error")
            }
        }
    }

    //gotBtPermToScan is checking scan permission for different apis
    @SuppressLint("MissingPermission")
    private fun buildInterfaceOk() {
        Log.d(TAG, "BUILDING INTERFACE : all is fine in theory")
        if (!gotBtPermToScan(this,"[START ACTIVITY][BT] buildInterfaceOk")){
            Log.d(TAG,"fun : buildInterfaceOk - fatal error")
            return
        }
        //1) Set header
        bind.tvWelcome.text = getString(R.string.tvWelcomeOk)

        //2) Start discover devices
        bluetoothAdapter.startDiscovery()
        if (bluetoothAdapter.isDiscovering) {
            Log.d(TAG, "[1] Is discovering...")
        } else {
            Log.d(TAG, "[1] Is NOT discovering...")
        }
        //3) List all paired devices with name LEDS_xxxx or LEDP_xxxx
        getPairedDevices() // from bt adapter to list view paired devices
        //4) handle action on paired devices
        bind.lvPairedDevices.isClickable = true
        bind.lvPairedDevices.setOnItemLongClickListener { _, view, position, _ ->
            doPopupMenuForPairedDevices(view, position)//TODO_
            PAIRED_DEVICES_SELECTED = position //put to companion object
            false
        }
        //5) handle new devices
        bind.tvNewDevices.visibility = View.VISIBLE
        //5.1) Broadcast receiver for new devices
        initBrNewDevices() // brNewDevices from new devices
        filterNewDevices = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(brNewDevices, filterNewDevices)
        //5.2) Broadcast receiver for bond state change
        initBrBoundDevice()
        filterBondDevice = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(brBondDevice, filterBondDevice)
        bind.lvNewDevices.setOnItemLongClickListener { _, view, position, _ ->
            doPopupMenuForNewDevices(view, position)
            false
        }

        //6) go to main application
        bind.btnNext.setOnClickListener { it ->
            Log.d(TAG,"Device list (empty) : ")
            DEVICE_LIST.clear()
            pairedDevices.forEach {
                if (it.name.contains("LEDS_") || it.name.contains("LEDP_")) {
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

    private fun buildInterfaceError() {
        Log.d(TAG, "BUILDING INTERFACE : errors")
        bind.tvWelcome.text = getString(R.string.tvWelcomeError)
    }

    /*
        Init broadcast receiver for new devices
     */
    private fun initBrNewDevices(){
        Log.d(TAG,"Init : broadcast receiver for new devices.")
        brNewDevices = object : BroadcastReceiver() {
            //gotBtPermToScan is checking scan permission for different apis
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        context?.let{
                            if (!gotBtPermToScan(context,"[START ACTIVITY][BT] initBrNewDevices")){
                                Log.d(TAG,"fun : initBrNewDevices - fatal error")
                                return
                            }
                        }

                        if (it.name.contains("LEDS_") || it.name.contains("LEDP_")){
                            //displayDeviceDetails(DeviceItem(it.name, it.address, false))
                            if (device.bondState== BluetoothDevice.BOND_NONE){
                                Log.d(TAG,"Not bonded -> ${it.name}")
                                if (!newDevices.contains(device)) {
                                    Log.d(TAG, "No device on new list")
                                    newDevices.add(device)
                                    val icon: Int
                                    if (it.name.contains("LEDS_") || it.name.contains("LEDP_")) {
                                        //old version
                                        //if (it.name.contains("LEDS_")) icon = R.drawable.icon_strip //ikonka listwy
                                        //else icon = R.drawable.icon_panel //ikonka ekranu
                                        icon =
                                            if (it.name.contains("LEDS_")) R.drawable.icon_strip //ikonka listwy
                                            else R.drawable.icon_panel //ikonka ekranu
                                        newDeviceList.add(DeviceListModel(it.name, it.address, icon))
                                        bind.lvNewDevices.adapter =  DeviceListAdapter(this@StartActivity,newDeviceList)
                                    }
                                }else{
                                    Log.d(TAG,"on new list -> ${it.name}")
                                }
                            }
                            //Poczytac o bond i remove w metodzie doPopup...
                        }
                    }
                }
            }
        }
    }

    /*
        Init broad cast receiver for bonding
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
                        if (!gotBtPermToScan(context,"[START ACTIVITY][BT] initBrBoundDevice")){
                            Log.d(TAG,"fun : initBrBoundDevice - fatal error")
                            return
                        }
                    }

                    Log.d(TAG,"Broadcast reciver (bond) is discavering  test : ${bluetoothAdapter.isDiscovering}")
                    when (thisDevice?.bondState){
                        BluetoothDevice.BOND_NONE ->{
                            Log.d(TAG,"Device : ${thisDevice.name}  state -> BOND_NONE")
                            //przeniesione z menu podrecznego tu bo interfejs odryswuje się często szybciej
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

    //from get list of paired devices form adapter ,put to paired device list view
    //gotBtPermToScan is checking scan permission for different apis
    @SuppressLint("MissingPermission")
    private  fun getPairedDevices(){
        Log.d(TAG,"entry : get paired device ")
        if (!gotBtPermToScan(this,"[START ACTIVITY][BT] getPairedDevices")){
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
                    NO_DEVICE_NAME,
                    NO_DEVICE_ADDRESS,
                    R.drawable.icon_no_devices
                )
            )
        }
        bind.lvPairedDevices.adapter =  DeviceListAdapter(this,pairedDeviceList)
        //bind.lvPairedDevices.deferNotifyDataSetChanged() nie uzywac , powoduje odroczenie dla zmian adaptera defakto
    }


    //try to remove bond between app and device
    //gotBtPermToScan is checking scan permission for different apis
    //gotBtPermToConnect is checking scan permission for different apis
    @SuppressLint("MissingPermission")
    private fun removeBond(device: BluetoothDevice) {
/*
        // testowany jest bluetooth_scan , moze to byc bluetooth_connect , tak sugeruje kotlin
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
                //Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //STANDARD_1 26_01_2022
            //a tu kontekst NIE wymaga uzupełnienia
            Log.d(TAG,"[START ACTIVITY][BT] removeBond - no connect permission")
            return
        }
*/

        // na ten moment profilaktycznie 2 testy
        if (!gotBtPermToScan(this,"[START ACTIVITY][BT] removeBond")){
            Log.d(TAG,"fun : removeBond - fatal error")
            return
        }
        if (!gotBtPermToConnect(this,"[START ACTIVITY][BT] removeBond")){
            Log.d(TAG,"fun : removeBond - fatal error")
            return
        }

        Log.d(TAG, "Try remove bond with : ${device.name}")
        try {
            device::class.java.getMethod("removeBond").invoke(device)
        } catch (e: Exception) {
            Log.e(TAG, "Removing bond has been failed. ${e.message}")
        }
    }

    //do popupmenu for paired devices
    //gotBtPermToScan is checking scan permission for different apis
    @SuppressLint("MissingPermission")
    private fun doPopupMenuForPairedDevices(thisView : View, pos : Int){
        val wrapper = ContextThemeWrapper(this, R.style.BasePopupMenu)
        val pairedPopup = PopupMenu(wrapper,thisView)
        //val pairedPopup= PopupMenu(this,thisView)
        val thisItem = bind.lvPairedDevices.getItemAtPosition(pos) as DeviceListModel
        var thisDevice : BluetoothDevice

        if (thisItem.deviceName == NO_DEVICE_NAME){
            pairedPopup.menu.add(Menu.NONE, 1, 0, "Skanuj w poszukiwaniu urządzeń")
        }else {
            pairedPopup.menu.add(Menu.NONE, 1, 0, "Skanuj w poszukiwaniu urządzeń")
            pairedPopup.menu.add(Menu.NONE, 2, 1, "Usun urządzenie")
        }
        pairedPopup.setOnMenuItemClickListener {
            when (it.itemId){
                1 -> {
                    //Toast.makeText(this, "skanuj : "+pos.toString(), Toast.LENGTH_SHORT).show()
                    if (gotBtPermToScan(this,"[START ACTIVITY][BT MENU] scan")){
                        if (this.bluetoothAdapter.isDiscovering) {
                            bluetoothAdapter.cancelDiscovery()
                            Log.d(TAG,"Canceling discovery.")
                        }
                        bluetoothAdapter.startDiscovery()
                        Log.d(TAG,"Starting discovery.")
                    }
                }
                2 ->{
                    //Toast.makeText(this, "usun : "+pos.toString(), Toast.LENGTH_SHORT).show()
                    thisDevice = bluetoothAdapter.getRemoteDevice(thisItem.deviceAdress)
                    removeBond(thisDevice)
                    //getPairedDevices()// przeniesine do initBrBoundDevice() bo często odrysowanie listview wywoływane
                    //jest wczesniej niż zmiana statusu urządzenia w systemie
                }
            }
            false
        }
        pairedPopup.show()
    }


    //gotBtPermToConnect is checking scan permission for different apis
    @SuppressLint("MissingPermission")
    private fun doPopupMenuForNewDevices(thisView : View, pos : Int){
        /*  Raz jeszcze przewalic dokumentacje
            //device.createBond()
            //device.setPairingConfirmation() //doczytac
            //device.setPin() //doczytac

         */
        if (bind.lvNewDevices.size > 0) {
            val wrapper = ContextThemeWrapper(this, R.style.BasePopupMenu)
            val newPopup = PopupMenu(wrapper,thisView)
            //val newPopup = android.widget.PopupMenu(this, thisView)
            val thisItem = bind.lvNewDevices.getItemAtPosition(pos) as DeviceListModel
            var thisDevice: BluetoothDevice
            newPopup.menu.add(Menu.NONE, 1, 0, "Paruj urządzenie")
            newPopup.setOnMenuItemClickListener {
                thisDevice = bluetoothAdapter.getRemoteDevice(thisItem.deviceAdress)
                //nowe
                if (!gotBtPermToConnect(this,"[START ACTIVITY][BT] doPopupMenuForNewDevices")){
                    Log.d(TAG,"fun : doPopupMenuForNewDevices - fatal error")
                }else{
                    thisDevice.createBond()
                }
                false
            }
            newPopup.show()
        }
    }

    //Only debug info , all paired or discoverd devices
    private fun displayDeviceDetails(device: DeviceItem) {
        val prefix = if (device.isConnected) "[C]" else "[D]"
        val info=  "$prefix Bluetooth device found: ${device.name}  address: ${device.address} ."
        Log.d(TAG, info)
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
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
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
        return gotAllPerms
    }

    private fun iamRunningOnInfo(){
        var gradleVersion  = "version code : "
        gradleVersion += BuildConfig.VERSION_CODE.toString()
        gradleVersion += " version name : " + BuildConfig.VERSION_NAME
        Log.d(TAG,"USING SDK: ${Build.VERSION.SDK_INT} -> ${Build.VERSION.CODENAME}")
        //val currentDebug = getString(R.string.app_name)
        Log.d(TAG,"CURRENT DEBUG -> $gradleVersion")
        Log.d(TAG,"----------------")
    }
/*
    private fun gotBtPermToScan(errorMessage : String) : Boolean{
        var gotPerm  = true
        if (Build.VERSION.SDK_INT <=30) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage (API) <=30 , BLUETOOTH")
            }

            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage (API) <=30 , BLUETOOTH_ADMIN")
            }
        }

        if (Build.VERSION.SDK_INT >=31) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage (API) >= 31")
            }
        }
        return gotPerm
    }

    private fun gotBtPermToConnect(errorMessage : String) : Boolean{
        var gotPerm  = true
        if (Build.VERSION.SDK_INT <=30) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage (API) <=30 , BLUETOOTH")
            }

            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage (API) <=30 , BLUETOOTH_ADMIN")
            }
        }

        if (Build.VERSION.SDK_INT >=31) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage (API) >= 31")
            }
        }
        return gotPerm
    }
*/
}