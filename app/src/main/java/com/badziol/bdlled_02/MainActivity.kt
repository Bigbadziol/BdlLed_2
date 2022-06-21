package com.badziol.bdlled_02
/*
//nowy commit
16.06.2022 - wydaje sie , ze REDMI Note 10 pro ma problem z implementacją BT,
wymaga perm BLUETOOTH , ktore powinno obowiazywac tylko do Androida 11

22.04.2022 - przy ustawianiu tla nagranego wcześniej(typ=10) zwracany jest teraz pusty obiekt
    "data":{}, w celu ujednolicenia podejscia
----
Wcześniej : BtHandler : Handler() -> BtHandler : Handler(Looper.getMainLooper())
myHandler = Handler() -> myHandler = Handler(Looper.getMainLooper())
 private inner class AcceptIncommingThread() : Thread() {
  -> private inner class AcceptIncommingThread : Thread() {

  //WAS : bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
  val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  bluetoothAdapter = bluetoothManager.adapter

  //--- aby byla aktualna wersja
  //--- w lukecinie text effect : word by word,
  //--- background effect : Liquid plasma
  //--- background effect : Big plasma
  //---28.11.2021
  //--jak zawsze jebane BT
  //
 */

// import android.Manifest
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
//import androidx.core.app.ActivityCompat
import com.badziol.bdlled_02.adapters.*
import com.example.bdlled_02.R
import com.example.bdlled_02.databinding.ActivityMainBinding
//import com.github.dhaval2404.colorpicker.ColorPickerDialog //old color picker
//import com.github.dhaval2404.colorpicker.model.ColorShape
import com.github.dhaval2404.colorpicker.util.setVisibility
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


const val SERVICE_NAME = "KrzysService"
//val uuid: UUID = UUID.fromString("06AE0A74-7BD4-43AA-AB5D-2511F3F6BAB1")
val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
lateinit var mySelectedBluetoothDevice: BluetoothDevice
lateinit var bluetoothAdapter: BluetoothAdapter
lateinit var appSocket: BluetoothSocket
lateinit var espSocket: BluetoothSocket
lateinit var myHandler: Handler
lateinit var dataHandler: Handler //only for data handling from ESP

var allStripData = Gson().fromJson(jsonStripDataTest_big, jStripData::class.java)
var allPanelData = Gson().fromJson(jsonPanelDataTest_small, jPanelData::class.java)



class MainActivity : AppCompatActivity(){
    private lateinit var bind : ActivityMainBinding
    private var myDevices : ArrayList<BluetoothDevice> = ArrayList() //list form start activity
    private var startPos : Int = 0 // position in list , current sellected device
    var espState : EspConnectionState = EspConnectionState.DISCONNECTED

    private var stripModeList : ArrayList<String> = ArrayList() //load data from resources in onCreate
    private var stripPaletteList : ArrayList<String> = ArrayList()  //load data from resources in onCreate
    private var stripCustomList :  ArrayList<String> = ArrayList()  //nad tym tez trzeba popracowac, znaczy sie wyjebac

    private var panelModeList : ArrayList<String> = ArrayList() //load data from resources in onCreate

    var sentenceList: ArrayList<jPanelSentence> = ArrayList()
    var fontList : ArrayList<jPanelFont> = ArrayList()
    private var fontSizeList : ArrayList<String> = ArrayList()
    private var fontDecorationList : ArrayList<String> = ArrayList()
    private var fontBorderTypeList : ArrayList<String> = ArrayList()
    var textPositionList : ArrayList<jPanelTextPosition> = ArrayList()
    var textEffectList : ArrayList<jPanelTextEffect> = ArrayList()
    var backgroundList : ArrayList<jPanelBackgrounds> = ArrayList()
    //----------------------------------------------------------------------------------------------
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
                    context, Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gotPerm = false
                Log.d(TAG, "$errorMessage  - no  permission (API) >= 31 , BLUETOOTH (FOR XIAOMI BUG) ")
            }

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
    //----------------------------------------------------------------------------------------------
     private  inner class BtHandler : Handler(Looper.getMainLooper()){
        var allMessage : String =""

        @SuppressLint("MissingPermission")
        override fun handleMessage(msg: Message) {
            val readBuf = msg.obj as String
            when (msg.what){
                1 ->{
                    if (readBuf.length == 330){ //330 - size of buffer set in ESP-IDF
                        allMessage += readBuf
                    }else{
                        allMessage += readBuf
                        if (allMessage.length-1 > 0) {
                            if (!gotBtPerms(this@MainActivity,"[MAIN ACTIVITY][BT] handleMessage")){
                                Log.d(TAG,"class : BtHandler , fun : handleMessage - fatal error")
                                return
                            }
                            when {
                                mySelectedBluetoothDevice.name.contains("LEDS_") -> {
                                    Log.d(TAG, "ALL DATA FROM : LED STRIP")
                                    jsonStripDataTest_big = allMessage.substring(0, allMessage.length - 1)
                                    allStripData = Gson().fromJson(
                                        jsonStripDataTest_big ,
                                        jStripData::class.java)
                                    allMessage = ""
                                    Log.d("DEBUG_INSIDE","Data loaded system alive")
                                    piStripMain()
                                }
                                mySelectedBluetoothDevice.name.contains("LEDP_") -> {
                                    Log.d(TAG, "ALL DATA FROM : LED PANEL (it could be cutted, to much data)")
                                    Log.d(TAG, "DATA : $allMessage")

                                    //allPanelData = Gson().fromJson(allMessage, jPanelData::class.java)

                                    Log.d("DEBUG_INSIDE","Clearing panel lists :sentences , font, position , effect , background")
                                    Log.d("DEBUG_INSIDE","Warning! ")
                                    sentenceList.clear()
                                    sentenceList.addAll(allPanelData.sentences)
                                    fontList.clear()
                                    fontList.addAll(allPanelData.fonts)
                                    textPositionList.clear()
                                    textPositionList.addAll(allPanelData.textPositions)
                                    textEffectList.clear()
                                    textEffectList.addAll(allPanelData.textEffects)
                                    backgroundList.clear()
                                    backgroundList.addAll(allPanelData.backgrounds)

                                    bind.lvPanelSentences.adapter = SentenceListAdapter(this@MainActivity,sentenceList)
                                    allMessage = ""
                                    //show panels
                                    showPanelConfig()
                                    showPanelSenteces()
                                    Log.d("DEBUG_INSIDE","----")
                                }
                                else -> {
                                    Log.d(TAG, "Unknown type of selected device")
                                    allMessage = ""
                                    }
                            } // test if name contains LEDS_ or LEDP_ prefix
                        } //readBuf > 0 but != 330
                    } //redBuf == 330
                } //msg idetifier
            }//there is a message
            super.handleMessage(msg)
        }
    }
    //----------------------------------------------------------------------------------------------
/*
    @SuppressLint("MissingPermission")
    private inner class AcceptIncommingThread : Thread() {
        val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            //ZDECYDOWANIE OBADAC
            //if (gotBtPerms(this@MainActivity,"class : AcceptIncommingThread - fatal error ")){
                bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, uuid)
            //}
        }

        override fun run() {
            var shouldLoop = true
            Log.d("DEBUG_ESP_INCOMMING" , "RUN processs...")
            while (shouldLoop) {
                val newSocket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.d("DEBUG_ESP", "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                newSocket?.also {
                    Log.d("DEBUG_ESP","Before ESP handling")
                    espSocket = newSocket
                    ConnectedThread(espSocket).start()
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }
        //------------------------------------------------------------------------------------------
        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.d("DEBUG_ESP", "Could not close the connect socket", e)
            }
        }
    }
  */
    //----------------------------------------------------------------------------------------------
    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        override fun run() {
            val inputStream = socket.inputStream
            val buffer = ByteArray(20240)
            var bytes: Int
            var thisMessage: String
            Log.d("DEBUG_ESP", "Waiting for data, ...")
            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    //Log.d("DEBUG_DATA_SIZE", bytes.toString())
                    thisMessage  = String(buffer, 0, bytes)
                    dataHandler.obtainMessage(1,thisMessage).sendToTarget()
                } catch (e :IOException) {
                    e.printStackTrace()
                    Log.d("DEBUG_ESP", "Data error")
                    break
                }
            }
        }
    }
    //----------------------------------------------------------------------------------------------
    // Uwaga
    // wczesniejsza inicjacja : private var newSocket = device.createInsecureRfcommSocketToServiceRecord(uuid)
    // aby abejsc : pojawilo sie  :  var myDevice = device
    // samo utworzenie serwisu : poszlo do try PO sprawdzeniu uprawnien
    //--------------------------------------------------------------------------------------
    private inner class ConnectThread(device: BluetoothDevice): Thread() {
        @SuppressLint("MissingPermission")
        private var newSocket = device.createInsecureRfcommSocketToServiceRecord(uuid)
        @SuppressLint("MissingPermission")
        override fun run() {
            bluetoothAdapter.cancelDiscovery()
            try {
                Log.d("DEBUG_APP", "Connecting socket")
                myHandler.post {
                    espState = EspConnectionState.CONNECTING
                    handlePanelsVisibility()
                }
                appSocket = newSocket

                if (!gotBtPerms(this@MainActivity,
                        "[MAIN ACTIVITY][BT] ConnectThread")) {
                    return
                }

                appSocket.connect()

                Log.d("DEBUG_APP", "Socket connected")
                myHandler.post {
                    espState = EspConnectionState.CONNECTED
                    handlePanelsVisibility()
                }
                ConnectedThread(appSocket).start()
                val cmdWelcome= JsonObject()
                cmdWelcome.addProperty("cmd","DATA_PLEASE")
                cmdWelcome.addProperty("cmdId",0)
                //ConnectThread(mySelectedBluetoothDevice).writeMessage("""{"cmd" : "DATA_PLEASE" }""")
                ConnectThread(mySelectedBluetoothDevice).writeMessage(cmdWelcome.toString())
            }catch (e1: Exception){
                Log.d("DEBUG_APP", "Error connecting socket, $e1")
                myHandler.post {
                    //bind.connectedOrNotTextView.text = "Connection failed" //OLD
                    espState = EspConnectionState.CONNECTION_ERROR
                    handlePanelsVisibility()
                }
            }
        }
        fun writeMessage(newMessage: String){
            Log.d("DEBUG_APP", "Sending")
            val outputStream = appSocket.outputStream
            try {
                outputStream.write(newMessage.toByteArray())
                outputStream.flush()
                Log.d("DEBUG_APP", "Sent " + newMessage)

            } catch (e: Exception) {
                Log.d("DEBUG_APP", "Cannot send, " + e)
                return
            }
        }
        fun cancel(){
            try {
                appSocket.close()
            } catch (e: IOException) {
                Log.d("DEBUG_APP", "Cant close socket", e)
            }
        }
    }

    //this is a quick fix because some problems with description translaction in enum class
    private fun espStateDescription(): String{
        when (espState){
            EspConnectionState.DISCONNECTED -> return getString(R.string.espDisconnected)
            EspConnectionState.CONNECTED -> return getString(R.string.espConnected)
            EspConnectionState.CONNECTING -> return getString(R.string.espConnecting)
            EspConnectionState.CONNECTION_ERROR -> return getString(R.string.espConnectionError)
        }
    }


    /*
        Prepare main settings interface : turn on visibility of components,
        get data from json
     */
    private fun handlePanelsVisibility(){
        when (espState){
            EspConnectionState.DISCONNECTED -> {
                hideStripInterface()
                defaultStripEffectInterface() //hide all elements and set default values
                hidePanelInterfaces()
                bind.btnConnect.isEnabled = true
                bind.spDevices.isEnabled = true
                bind.btnConnect.text = getString(R.string.iConnect)
            }

            EspConnectionState.CONNECTING ->{
                hideStripInterface()
                defaultStripEffectInterface() //hide all elements and set default values
                hidePanelInterfaces()
                bind.btnConnect.isEnabled = false
                bind.spDevices.isEnabled = false
                bind.btnConnect.text =getString(R.string.iConnect)

            }
            EspConnectionState.CONNECTED -> {
                //The relevant panels are turned on by BtHandler after downloading the necessary data
                bind.btnConnect.isEnabled = true
                bind.spDevices.isEnabled = false
                bind.btnConnect.text =getString(R.string.iDisconnect)
            }
            EspConnectionState.CONNECTION_ERROR ->{
                //all panels at this point are invisible becouse preious state "CONNECTING"
                //hide all panels
                bind.btnConnect.isEnabled = true
                bind.spDevices.isEnabled = true
                bind.btnConnect.text =getString(R.string.iConnect)
            }
        }
        // old version
        //bind.tvStatus.text = espState.description
        bind.tvStatus.text = espStateDescription()
    }

    @SuppressLint("MissingPermission")
    private fun piConnection(){
        //CONNECTION PART
        //now from Bluetooth device list  to string list to device adapter....
        //var myDevicesNames = arrayOf<String>() // old version with no custom draw
        val myDevicesNames : ArrayList<String> = ArrayList()

        if (!gotBtPerms(this,"[MAIN ACTIVITY][BT] piConnection")){
            Log.d(TAG,"fun : piConnection - fatal error")
            return
        }else{
            for (d in myDevices) {
                myDevicesNames += d.name
            }
        }

        // this is a old version with no custom draw
        //val adapterDevices = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,myDevicesNames)
        val adapterDevices = StringListAdapter(this@MainActivity, myDevicesNames)
        bind.spDevices.adapter = adapterDevices
        bind.spDevices.setSelection(startPos)
    }

    private fun piStripCore(){
        with(bind,){
            cvStripInterface.setVisibility(true)
            panelStrip.setVisibility(true)
            panelStripConfig.setVisibility(true)
            rowStripMode.setVisibility(true)
            rowStripConfirm.setVisibility(true)
        }
    }
    private fun piStripModeSelectedEffect(){
        piStripCore()
        with(bind,){
            rowStripEffectSelect.setVisibility(true)
            rowStripTime.setVisibility(false)
            rowStripColor.setVisibility(false)
            panelStripEffect.setVisibility(true)
        }

    }
    //for :auto next and random next effect modes
    private fun piStripModeRandom(){
        piStripCore()
        with(bind) {
            rowStripEffectSelect.setVisibility(false)
            rowStripTime.setVisibility(true)
            rowStripColor.setVisibility(false)
            panelStripEffect.setVisibility(false)
        }
    }

    private fun piStripModeColor(){
        piStripCore()
        with(bind){
            rowStripEffectSelect.setVisibility(false)
            rowStripTime.setVisibility(false)
            rowStripColor.setVisibility(true)
            panelStripEffect.setVisibility(false)
        }
    }

    private fun piStripMain(){
        val stripEffectList   : ArrayList<jStripEffect> = ArrayList()
        stripEffectList.addAll(allStripData.effects)
        with(
            bind,
        ) {
            //here is new visibility version
            //specific interface is called by spStripMode listener
            piStripCore() // set minimum visibility
            spStripMode.setSelection(allStripData.config.mode, false)

            if (stripEffectList.size > 0) {
                spStripEffect.adapter = StripEffectListAdapter(this@MainActivity,stripEffectList)
                spStripEffect.setSelection(allStripData.config.selected)
            } else {
                rowStripEffectSelect.setVisibility(false)
            }
            sbStripTime.setProgress(allStripData.config.time, false)
            lbStripTimeVal.text = allStripData.config.time.toString()
            tvStripColorMain.setBackgroundColor(
                Color.rgb(
                    allStripData.config.color.r,
                    allStripData.config.color.g,
                    allStripData.config.color.b,
                ),
            )
        }
    }

    private fun hideStripInterface(){
        bind.cvStripInterface.setVisibility(false)
    }

    private fun hidePanelConfig(){
        with(bind) {
            rowPanelBrightess.setVisibility(false)
            rowPanelWorkMode.setVisibility(false)
            rowPanelConfirm.setVisibility(false)
            panelPanelConfig.setVisibility(false)
        }
    }

    private fun showPanelConfig(){
        with(bind) {
            rowPanelBrightess.setVisibility(true)
            rowPanelWorkMode.setVisibility(false) //DISABLED FOR NOW
            rowPanelConfirm.setVisibility(true)
            panelPanelConfig.setVisibility(true)
        }
    }

    private fun hidePanelSentences(){
        with(bind) {
            tvSentenceListHeader.setVisibility(false)
            lvPanelSentences.setVisibility(false)
            panelPanelSentences.setVisibility(false)
        }
    }

    private fun showPanelSenteces(){
        with(bind) {
            tvSentenceListHeader.setVisibility(true)
            lvPanelSentences.setVisibility(true)
            panelPanelSentences.setVisibility(true)
        }
    }

    private fun hidePanelInterfaces(){
        hidePanelConfig()
        hidePanelSentences()
    }

    //All set "Metods" simply turn on visibility and set params
    //This methods do only UI stuff, DONT set any datas
    /*
    Clear Seek Bar parameter , little help only for : clearAndHideEffectInterface()
    */
    private fun clearStripParamVal(p : SeekBar){
        p.min = 0
        p.max = 255
        p.setProgress(0,false)
    }

    //hide everything , set all values to default
    private fun defaultStripEffectInterface(){
        //set default values
        bind.tvStripEffectName.text = resources.getString(R.string.tvStripEffectName)
        bind.edStripColor1.setBackgroundColor(Color.parseColor("#000000"))
        bind.btnStripColor1.text = getString(R.string.btnCommonColor1)
        bind.edStripColor2.setBackgroundColor(Color.parseColor("#000000"))
        bind.btnStripColor2.text = getString(R.string.btnCommonColor2)
        bind.lbStripPalette.text = resources.getString(R.string.lbStripPalette)
        bind.spStripPalette.setSelection(0)
        bind.lbStripCustom.text = resources.getString(R.string.lbCommonCustom)
        bind.spStripCustom.setSelection(0)
        bind.lbStripParam1.text = resources.getString(R.string.lbCommonParam1)
        clearStripParamVal(bind.sbStripParam1)
        bind.lbStripParam2.text = resources.getString(R.string.lbCommonParam2)
        clearStripParamVal(bind.sbStripParam2)
        bind.lbStripParam3.text = resources.getString(R.string.lbCommonParam3)
        clearStripParamVal(bind.sbStripParam3)
        bind.lbStripParam4.text = resources.getString(R.string.lbCommonParam4)
        clearStripParamVal(bind.sbStripParam4)
        bind.lbStripBool1.text = resources.getString(R.string.lbCommonBool1)
        bind.swStripBool1.isChecked = false
        bind.lbStripBool2.text = resources.getString(R.string.lbCommonBool2)
        bind.swStripBool2.isChecked = false
        //hide everything
        bind.rowStripEffectName.setVisibility(false)
        bind.rowStripEffectColor1.setVisibility(false)
        bind.rowStripEffectColor2.setVisibility(false)
        bind.rowStripEffectPalette.setVisibility(false)
        bind.rowStripEffectCustom.setVisibility(false)
        bind.rowStripEffectParam1.setVisibility(false)
        bind.rowStripEffectParam2.setVisibility(false)
        bind.rowStripEffectParam3.setVisibility(false)
        bind.rowStripEffectParam4.setVisibility(false)
        bind.rowStripEffectBool1.setVisibility(false)
        bind.rowStripEffectBool2.setVisibility(false)
    }

    //Strip name also turn on strip effect panel !!
    private fun setStripEffectName(name : String ){
        val prefixAndName = getString(R.string.lbStripEffectNamePrefix) + name
        bind.panelStripEffect.setVisibility(true)
        bind.rowStripEffectName.setVisibility(true)
        bind.tvStripEffectName.text = prefixAndName
    }
    private fun setStripParamColor(pColorNum : Int, r :Int, g : Int, b : Int){
        when (pColorNum){
            1 -> {
                bind.rowStripEffectColor1.setVisibility(true)
                bind.edStripColor1.setBackgroundColor(Color.rgb(r,g,b))
            }
            2 -> {
                bind.rowStripEffectColor2.setVisibility(true)
                bind.edStripColor2.setBackgroundColor(Color.rgb(r,g,b))
            }
        }
    }
    private fun setStripPalette(index : Int){
        bind.rowStripEffectPalette.setVisibility(true)
        if (index > bind.spStripPalette.count - 1) bind.spStripPalette.setSelection(0,false)
        else bind.spStripPalette.setSelection(index,false)
        //   Toast.makeText(this,"Count P:" +bind.spPalette.count,Toast.LENGTH_SHORT).show()
    }

    private fun setStripCustom(desc : String, elem : Array<String>, index : Int){
        if (elem.isNotEmpty()){
            bind.rowStripEffectCustom.setVisibility(true)
            bind.lbStripCustom.text = desc
            val aC = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,elem)
            bind.spStripCustom.adapter = aC
            if (index > elem.size -1) bind.spStripCustom.setSelection(0)
            else bind.spStripCustom.setSelection(index)
        }
    }

    private fun setStripParamVal(pNum : Int, desc : String, pVal : Int, pMin : Int, pMax : Int){
        when (pNum){
            1 -> {
                bind.rowStripEffectParam1.setVisibility(true)
                bind.lbStripParam1.text = desc
                bind.sbStripParam1.min = pMin
                bind.sbStripParam1.max = pMax
                bind.sbStripParam1.progress = pVal
            }
            2->{
                bind.rowStripEffectParam2.setVisibility(true)
                bind.lbStripParam2.text = desc
                bind.sbStripParam2.min = pMin
                bind.sbStripParam2.max = pMax
                bind.sbStripParam2.progress = pVal
            }
            3->{
                bind.rowStripEffectParam3.setVisibility(true)
                bind.lbStripParam3.text = desc
                bind.sbStripParam3.min = pMin
                bind.sbStripParam3.max = pMax
                bind.sbStripParam3.progress = pVal
            }
            4->{
                bind.rowStripEffectParam4.setVisibility(true)
                bind.lbStripParam4.text = desc
                bind.sbStripParam4.min = pMin
                bind.sbStripParam4.max = pMax
                bind.sbStripParam4.progress = pVal
            }
        }
    }
    /*  Obejscie problemu , dla Kotlina bool to true/false , dla ESP32 jeden ch....
        czy true/false czy 1/0 , domyslnie zwraca jednak 1/0
        aby zachowac zgodność bool bedzie intem, mniej roboty i przerobek
        Podobnie w updateParamBool
     */
    private fun setStripParamBool(pNum : Int, desc : String, state : Int){
        var tmpBool  = false
        if (state == 1) tmpBool = true
        when (pNum){
            1-> {
                bind.rowStripEffectBool1.setVisibility(true)
                bind.lbStripBool1.text = desc
                bind.swStripBool1.isChecked = tmpBool
            }
            2-> {
                bind.rowStripEffectBool2.setVisibility(true)
                bind.lbStripBool2.text = desc
                bind.swStripBool2.isChecked = tmpBool
            }
        }
    }

    //THIS metods are triggered by uiXXXX methods , update data structures
    private fun updateStripParamCol(parmName: String, tvCol : TextView){
        val thisEffect = allStripData.effects[bind.spStripEffect.selectedItemPosition]
        if (thisEffect.data.has(parmName)){
            val col = tvCol.background as ColorDrawable
            val col2 = col.color
            val r = Color.red(col2)
            val g = Color.green(col2)
            val b = Color.blue(col2)
            val oCol = JsonObject()
            oCol.addProperty("r",r)
            oCol.addProperty("g",g)
            oCol.addProperty("b",b)
            thisEffect.data.add(parmName,oCol)
            //val rgbStr = " ->"+ r +" "+g+" "+b+" <-"
            //Toast.makeText(this , "RGB : "+rgbStr, Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, "Effect dont have $parmName parameter.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateStripPalette(parmName : String){
        val thisEffect = allStripData.effects[bind.spStripEffect.selectedItemPosition]
        if(thisEffect.data.has(parmName)){
            thisEffect.data.addProperty(parmName,bind.spStripPalette.selectedItemPosition)
        } else{
            Toast.makeText(this, "Effect dont have $parmName parameter", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateStripCustom(parmName : String){
        val thisCustom = allStripData.effects[bind.spStripEffect.selectedItemPosition]
        if (thisCustom.data.has(parmName)){
            thisCustom.data.addProperty(parmName,bind.spStripCustom.selectedItemPosition)
        }else{
            Toast.makeText(this, "Effect dont have $parmName parameter", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateStripParamVal(parmName : String, sb :SeekBar ){
        val thisEffect = allStripData.effects[bind.spStripEffect.selectedItemPosition]
        if(thisEffect.data.has(parmName)){
            thisEffect.data.addProperty(parmName,sb.progress)
        } else{
            Toast.makeText(this, "Effect dont have $parmName parameter", Toast.LENGTH_SHORT).show()
        }
    }
    /*  Wazne, pewna niezgodność czym jest bool dla Kotlina i ESP. Kotlin wymaga "true/false"
        ESP jest obojetnie czy "true/false" czy 1/0 , domyslnie jednak zwraca 1/0
        Zatem bool to int tak naprawde

     */
    private fun updateStripParamBool(parmName: String, sw : SwitchCompat){
        val thisEffect = allStripData.effects[bind.spStripEffect.selectedItemPosition]
        if(thisEffect.data.has(parmName)){
            var tmpBoolAsInt = 0
            if (sw.isChecked) tmpBoolAsInt = 1
            thisEffect.data.addProperty(parmName, tmpBoolAsInt)
        } else{
            Toast.makeText(this, "Effect dont have $parmName parameter", Toast.LENGTH_SHORT).show()
        }
    }

    //==========================================================================
    // "Beat wave" parm1 , parm2 , parm3 , parm4
    private fun piBeatWave(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pulse1")) {
                setStripParamVal(1, getString(R.string.stBeatWaveP1_pulse1), thisEffectData.get("pulse1").asInt, 1, 30)
            }
            if (thisEffectData.has("pulse2")) {
                setStripParamVal(2, getString(R.string.stBeatWaveP2_pulse2), thisEffectData.get("pulse2").asInt, 1, 30)
            }
            if (thisEffectData.has("pulse3")) {
                setStripParamVal(3,getString(R.string.stBeatWaveP3_pulse3), thisEffectData.get("pulse3").asInt, 1, 30)
            }
            if (thisEffectData.has("pulse4")) {
                setStripParamVal(4, getString(R.string.stBeatWaveP4_pulse4), thisEffectData.get("pulse4").asInt, 1, 30)
            }
        }else{
            Toast.makeText(this,"This "+ allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upBeatWave(){
        updateStripParamVal("pulse1",bind.sbStripParam1)
        updateStripParamVal("pulse2",bind.sbStripParam2)
        updateStripParamVal("pulse3",bind.sbStripParam3)
        updateStripParamVal("pulse4",bind.sbStripParam4)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Blend wave" parm1 , parm2 , parm3
    private fun piBlendWave(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("speed")) {
                setStripParamVal(1, getString(R.string.stBlendWaveP1_speed), thisEffectData.get("speed").asInt, 1, 12)
            }
            if (thisEffectData.has("mH1")) {
                setStripParamVal(2, getString(R.string.stBlendWaveP2_mH1), thisEffectData.get("mH1").asInt, 1, 24)
            }
            if (thisEffectData.has("mH2")) {
                setStripParamVal(3, getString(R.string.stBlendWaveP3_mH2), thisEffectData.get("mH2").asInt, 1, 24)
            }
        }else{
            Toast.makeText(this,"This "+ allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upBlendWave() {
        updateStripParamVal("speed", bind.sbStripParam1)
        updateStripParamVal("mH1", bind.sbStripParam2)
        updateStripParamVal("mH2", bind.sbStripParam3)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Blur" parm1 , parm2 , parm3 , parm4
    private fun piBlur(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("speed")) {
                setStripParamVal(1, getString(R.string.stBlurP1_speed), thisEffectData.get("speed").asInt, 1, 10)
            }
            if (thisEffectData.has("o1")) {
                setStripParamVal(2, getString(R.string.stBlurP2_o1), thisEffectData.get("o1").asInt, 1, 20)
            }
            if (thisEffectData.has("o2")) {
                setStripParamVal(3, getString(R.string.stBlurP3_o2), thisEffectData.get("o2").asInt, 1, 20)
            }
            if (thisEffectData.has("o3")) {
                setStripParamVal(4, getString(R.string.stBlurP4_o3), thisEffectData.get("o3").asInt, 1, 20)
            }
        }else{
            Toast.makeText(this,"This "+ allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upBlur(){
        updateStripParamVal("speed",bind.sbStripParam1)
        updateStripParamVal("o1",bind.sbStripParam2)
        updateStripParamVal("o2",bind.sbStripParam3)
        updateStripParamVal("o3",bind.sbStripParam4)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Confeti" palette , parm1 , parm2
    private fun piConfeti(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("fade")) {
                setStripParamVal(1, getString(R.string.stConfetiP1_fade), thisEffectData.get("fade").asInt, 1, 16)
            }
            if (thisEffectData.has("mDiff")) {
                setStripParamVal(2, getString(R.string.stConfetiP2_mdiff), thisEffectData.get("mDiff").asInt, 1, 15)
            }
        }
    }
    private  fun upConfeti(){
        updateStripPalette("pIndex")
        updateStripParamVal("fade",bind.sbStripParam1)
        updateStripParamVal("mDiff",bind.sbStripParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Sinelon" , parm1 , parm2
    private fun piSinelon(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("bpm")) {
                setStripParamVal(1, getString(R.string.stSinelonP1_bpm), thisEffectData.get("bpm").asInt, 1, 25)
            }
            if (thisEffectData.has("fade")) {
                setStripParamVal(2, getString(R.string.stSinelonP2_fade), thisEffectData.get("fade").asInt, 1, 20)
            }
        }
    }
    private fun upSinelon() {
        updateStripParamVal("bpm",bind.sbStripParam1)
        updateStripParamVal("fade",bind.sbStripParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Bpm" palette , parm1
    private fun piBpm(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("bpm")) {
                setStripParamVal(1, getString(R.string.stBpmP1_bpm), thisEffectData.get("bpm").asInt, 1, 12)
            }
        }
    }
    private fun upBpm(){
        updateStripPalette("pIndex")
        updateStripParamVal("bpm",bind.sbStripParam1)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Juggle" parm1 , parm2
    private fun piJuggle(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("stepHue")) {
                setStripParamVal(1, getString(R.string.stJuggleP1_stepHue), thisEffectData.get("stepHue").asInt, 1, 8)
            }
            if (thisEffectData.has("fade")) {
                setStripParamVal(2, getString(R.string.stJuggleP2_fade), thisEffectData.get("fade").asInt, 1, 25)
            }
        }
    }
    private fun upJuggle() {
        updateStripParamVal("stepHue",bind.sbStripParam1)
        updateStripParamVal("fade",bind.sbStripParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Dot beat" color1 ,color2 , parm1 , parm2
    private fun piDotBeat(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            var col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color1")){
                col = thisEffectData.get("color1").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setStripParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("color2")){
                col = thisEffectData.get("color2").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt }
                if (col.has("g")) { g = col.get("g").asInt }
                if (col.has("b")) { b = col.get("b").asInt }
                setStripParamColor(2 , r , g , b)
            }
            if (thisEffectData.has("bpm")) {
                setStripParamVal(1, getString(R.string.stDotBeatP1_bpm), thisEffectData.get("bpm").asInt, 1, 7)
            }
            if (thisEffectData.has("fadeMod")) {
                setStripParamVal(2, getString(R.string.stDotBeatP2_fadeMod), thisEffectData.get("fadeMod").asInt, 1, 10)
            }
        }
    }
    private fun upDotBeat(){
        updateStripParamCol("color1",bind.edStripColor1)
        updateStripParamCol("color2",bind.edStripColor2)
        updateStripParamVal("bpm",bind.sbStripParam1)
        updateStripParamVal("fadeMod",bind.sbStripParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Easing" color1 , parm1
    private fun piEasing(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            val col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color")){
                col = thisEffectData.get("color").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setStripParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("multiplier")) {
                setStripParamVal(1, getString(R.string.stEasingP1_multiplier), thisEffectData.get("multiplier").asInt, 1, 8)
            }
        }
    }
    private fun upEasing(){
        updateStripParamCol("color",bind.edStripColor1)
        updateStripParamVal("multiplier",bind.sbStripParam1)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Hyper dot" color1 ,  parm1 , parm2 , parm3
    private fun piHyperDot(){ //color 1 parm 1 parm2 parm 3
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            val col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color")){
                col = thisEffectData.get("color").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setStripParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("bpm")) {
                setStripParamVal(1, getString(R.string.stHyperDotP1_bpm), thisEffectData.get("bpm").asInt, 1, 20)
            }
            if (thisEffectData.has("low")) {
                setStripParamVal(2, getString(R.string.stHyperDotP2_low), thisEffectData.get("low").asInt, 1, 5)
            }
            if (thisEffectData.has("high")) {
                setStripParamVal(3, getString(R.string.stHyperDotP3_high), thisEffectData.get("high").asInt, 1, 10)
            }
        }
    }
    private fun upHyperDot(){
        updateStripParamCol("color",bind.edStripColor1)
        updateStripParamVal("bpm",bind.sbStripParam1)
        updateStripParamVal("low",bind.sbStripParam2)
        updateStripParamVal("high",bind.sbStripParam3)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Beat sin gradient" parm1 , parm2
    private fun piBeatSinGradient(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("start")) {
                setStripParamVal(1, getString(R.string.stBeatSinGradientP1_speed1), thisEffectData.get("start").asInt, 1, 16)
            }
            if (thisEffectData.has("end")) {
                setStripParamVal(2, getString(R.string.stBeatSinGradientP2_speed2), thisEffectData.get("end").asInt, 1, 16)
            }
        }
    }
    private fun upBeatSinGradient() {
        updateStripParamVal("start",bind.sbStripParam1)
        updateStripParamVal("end",bind.sbStripParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fire 1" parm1 , parm2
    private fun piFire1(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("cooling")) {
                setStripParamVal(1, getString(R.string.stFire1P1_cooling), thisEffectData.get("cooling").asInt, 1, 9)
            }
            if (thisEffectData.has("sparking")) {
                setStripParamVal(2, getString(R.string.stFire1P2_sparking), thisEffectData.get("sparking").asInt, 1, 16)
            }
        }
    }
    private fun upFire1() {
        updateStripParamVal("cooling",bind.sbStripParam1)
        updateStripParamVal("sparking",bind.sbStripParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fire 1 two flames" parm1 , parm2
    private fun piFire1TwoFlames(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("cooling")) {
                setStripParamVal(1, getString(R.string.stFire1TwoFlamesP1_cooling), thisEffectData.get("cooling").asInt, 1, 9)
            }
            if (thisEffectData.has("sparking")) {
                setStripParamVal(2, getString(R.string.stFire1TwoFlamesP2_sparking), thisEffectData.get("sparking").asInt, 1, 16)
            }
        }
    }
    private fun upFire1TwoFlames() {
        updateStripParamVal("cooling",bind.sbStripParam1)
        updateStripParamVal("sparking",bind.sbStripParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Worm" param1 , param2
    private fun piWorm(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("adjust")) {
                setStripParamVal(1, getString(R.string.stWormP1_adjust), thisEffectData.get("adjust").asInt, 1, 24)
            }
            if (thisEffectData.has("nextBlend")) {
                setStripParamVal(2, getString(R.string.stWormP2_nextblend), thisEffectData.get("nextBlend").asInt, 1, 6)
            }
        }
    }
    private fun upWorm() {
        updateStripParamVal("adjust",bind.sbStripParam1)
        updateStripParamVal("nextBlend",bind.sbStripParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fire 2" custom , parm1 , parm2
    private fun piFire2(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data

            if (thisEffectData.has("dir")) {
                val customParams = arrayOf(
                    getString(R.string.stFire2Custom_rightToLeft),
                    getString(R.string.stFire2Custom_leftToRight),
                    getString(R.string.stFire2Custom_bothSites)
                )
                setStripCustom(getString(R.string.stFire2CustomDescription), customParams,thisEffectData.get("dir").asInt)
            }
            if (thisEffectData.has("intensity")) {
                setStripParamVal(1, getString(R.string.stFire2P1_intensity), thisEffectData.get("intensity").asInt, 1, 5)
            }
            if (thisEffectData.has("speed")) {
                setStripParamVal(2, getString(R.string.stFire2P2_speed), thisEffectData.get("speed").asInt, 1, 5)
            }
        }else{
            Toast.makeText(this,"This "+ allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upFire2(){
        updateStripCustom("dir") //data gets form spCustom
        updateStripParamVal("intensity",bind.sbStripParam1)
        updateStripParamVal("speed",bind.sbStripParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Noise 1" , palette , parm1 , parm2
    private fun piNoise1(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("low")) {
                setStripParamVal(1, getString(R.string.stNoise1P1_low), thisEffectData.get("low").asInt, 1, 10)
            }
            if (thisEffectData.has("high")) {
                setStripParamVal(2, getString(R.string.stNoiseP2_high), thisEffectData.get("high").asInt, 1, 10)
            }
        }
    }
    private fun upNoise1(){
        updateStripPalette("pIndex")
        updateStripParamVal("low",bind.sbStripParam1)
        updateStripParamVal("high",bind.sbStripParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Juggle 2" parm1 , parm2 , parm3
    private fun piJuggle2(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("dots")) {
                setStripParamVal(1, getString(R.string.stJuggle2P1_dots), thisEffectData.get("dots").asInt, 1, 10)
            }
            if (thisEffectData.has("beat")) {
                setStripParamVal(2, getString(R.string.stJuggle2P2_beat), thisEffectData.get("beat").asInt, 1, 20)
            }
            if (thisEffectData.has("fade")) {
                setStripParamVal(3, getString(R.string.stJuggle2P3_fade), thisEffectData.get("fade").asInt, 1, 10)
            }
        }else{
            Toast.makeText(this,"This "+ allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upJuggle2() {
        updateStripParamVal("dots", bind.sbStripParam1)
        updateStripParamVal("beat", bind.sbStripParam2)
        updateStripParamVal("fade", bind.sbStripParam3)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Running color dots"  palette , custom
    private fun piRunningColorDots(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("dir")) {
                val customParams = arrayOf(
                    getString(R.string.stRunningColorDotsCustom_leftToRight),
                    getString(R.string.stRunningColorDotsCustom_rightToLeft)
                )
                setStripCustom(getString(R.string.stRunningColorDotsCustomDescription), customParams,thisEffectData.get("dir").asInt)
            }
        }
    }
    private fun upRunningColorDots(){
        updateStripPalette("pIndex")
        updateStripCustom("dir") //data gets form spCustom
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Disco 1" palette , parm1
    private fun piDisco1(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("flash")) {
                setStripParamVal(1, getString(R.string.stDisco1P1_flash), thisEffectData.get("flash").asInt, 1, 10)
            }
        }
    }
    private fun upDisco1(){
        updateStripPalette("pIndex")
        updateStripParamVal("flash",bind.sbStripParam1)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Running color dots 2" palette , color1 , param1 , param2
    private fun piRunningColorDots2(){ //color 1 parm 1 bool1
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            val col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("bgColor")){
                col = thisEffectData.get("bgColor").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setStripParamColor(1 , r , g , b)
                bind.btnStripColor1.text =getString(R.string.stRunningColorDots2C1_background)
            }
            if (thisEffectData.has("bgBright")) {
                setStripParamVal(1, getString(R.string.stRunningColorDots2P1_bgbright), thisEffectData.get("bgBright").asInt, 1, 10)
            }
            if (thisEffectData.has("bgStatic")){
                setStripParamBool(1,getString(R.string.stRunningColorDots2B1_bgstatic),thisEffectData.get("bgStatic").asInt)
            }
        }
    }
    private fun upRunningColorDots2(){
        updateStripPalette("pIndex")
        updateStripParamCol("bgColor",bind.edStripColor1)
        updateStripParamVal("bgBright",bind.sbStripParam1)
        updateStripParamBool("bgStatic",bind.swStripBool1)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Disco dots" parm1
    private fun piDiscoDots(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("phaseTime")) {
                setStripParamVal(1, getString(R.string.stDiscoDotsP1_phasetime), thisEffectData.get("phaseTime").asInt, 5, 30)
            }
        }
    }
    private fun upDiscoDots() {
        updateStripParamVal("phaseTime",bind.sbStripParam1)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    // "Plasma" palette , parm1 , parm2 , parm 3
    private fun piPlasma(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("low")) {
                setStripParamVal(1, getString(R.string.stPlasmaP1_low), thisEffectData.get("low").asInt, 1, 13)
            }
            if (thisEffectData.has("high")) {
                setStripParamVal(2, getString(R.string.stPlasmaP2_high), thisEffectData.get("high").asInt, 1, 13)
            }
            if (thisEffectData.has("speed")) {
                setStripParamVal(3, getString(R.string.stPlasmaP3_speed), thisEffectData.get("speed").asInt, 1, 5)
            }
        }
    }
    private fun upPlasma(){
        updateStripPalette("pIndex")
        updateStripParamVal("low",bind.sbStripParam1)
        updateStripParamVal("high",bind.sbStripParam2)
        updateStripParamVal("speed",bind.sbStripParam3)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Rainbow sine" , parm1 , parm2
    private fun piRainbowSine(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("speed")) {
                setStripParamVal(1, getString(R.string.stRainbowSineP1_speed), thisEffectData.get("speed").asInt, 1, 6)
            }
            if (thisEffectData.has("hueStep")) {
                setStripParamVal(2, getString(R.string.stRainbowSineP2_huestep), thisEffectData.get("hueStep").asInt, 1, 8)
            }
        }
    }
    private fun upRainbowSine() {
        updateStripParamVal("speed",bind.sbStripParam1)
        updateStripParamVal("hueStep",bind.sbStripParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fast rainbow" , parm1 , parm2
    private fun piFastRainbow(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("speed")) {
                setStripParamVal(1, getString(R.string.stFastRainbowP1_speed), thisEffectData.get("speed").asInt, 1, 9)
            }
            if (thisEffectData.has("delta")) {
                setStripParamVal(2, getString(R.string.stFastRainbowP2_delta), thisEffectData.get("delta").asInt, 1, 9)
            }
        }
    }
    private fun upFastRainbow() {
        updateStripParamVal("speed",bind.sbStripParam1)
        updateStripParamVal("delta",bind.sbStripParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Pulse rainbow" , custom ,parm1 , parm2 , parm3
    private fun piPulseRainbow(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data

            if (thisEffectData.has("dir")) {
                val customParams = arrayOf(
                    getString(R.string.stPulseRainbowCustom_forward),
                    getString(R.string.stPulseRainbowCustom_backward)
                )
                setStripCustom(getString(R.string.stPulseRainbowCustomDescription), customParams,thisEffectData.get("dir").asInt)
            }
            if (thisEffectData.has("rot")) {
                setStripParamVal(1, getString(R.string.stPulseRainbowP1_rot), thisEffectData.get("rot").asInt, 1, 5)
            }
            if (thisEffectData.has("hue")) {
                setStripParamVal(2, getString(R.string.stPulseRainbowP2_hue), thisEffectData.get("hue").asInt, 1, 10)
            }
            if (thisEffectData.has("delay")) {
                setStripParamVal(3, getString(R.string.stPulseRainbowP3_delay), thisEffectData.get("delay").asInt, 1, 5)
            }
        }else{
            Toast.makeText(this,"This "+ allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upPulseRainbow(){
        updateStripCustom("dir") //data gets form spCustom
        updateStripParamVal("rot",bind.sbStripParam1)
        updateStripParamVal("hue",bind.sbStripParam2)
        updateStripParamVal("delay",bind.sbStripParam3)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fireworks" , parm1 , parm2
    private fun piFireworks(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("size")) {
                setStripParamVal(1, getString(R.string.stFireworksP1_size), thisEffectData.get("size").asInt, 1, 9)
            }
            if (thisEffectData.has("speed")) {
                setStripParamVal(2, getString(R.string.stFireworksP2_speed), thisEffectData.get("speed").asInt, 1, 4)
            }
        }
    }
    private fun upFireworks() {
        updateStripParamVal("size",bind.sbStripParam1)
        updateStripParamVal("speed",bind.sbStripParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fireworks 2" , parm1 , parm2
    private fun piFireworks2(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("life")) {
                setStripParamVal(1, getString(R.string.stFireworks2P1_life), thisEffectData.get("life").asInt, 1, 6)
            }
            if (thisEffectData.has("fade")) {
                setStripParamVal(2, getString(R.string.stFireworks2P2_fade), thisEffectData.get("fade").asInt, 1, 6)
            }
        }
    }
    private fun upFireworks2(){
        updateStripParamVal("life",bind.sbStripParam1)
        updateStripParamVal("fade",bind.sbStripParam2)
    }
    //"Sin-neon" custom , parm1
    private fun piSinNeon(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("mode")) {
                val customParams = resources.getStringArray(R.array.stSinNeonCustom)
                setStripCustom(getString(R.string.stSinNeonCustomDescription), customParams,thisEffectData.get("mode").asInt)
            }
            if (thisEffectData.has("phaseTime")) {
                setStripParamVal(1, getString(R.string.stSinNeonP1_phaseTime), thisEffectData.get("phaseTime").asInt, 10, 30)
            }
        }
    }
    private fun upSinNeon(){
        updateStripCustom("mode") //data gets form spCustom
        updateStripParamVal("phaseTime",bind.sbStripParam1)
    }
    //"Carusel" , custom, parm1 , parm1
    private fun piCarusel(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("mode")) {
                val customParams = resources.getStringArray(R.array.stCaruselCustom)
                setStripCustom(getString(R.string.stCaruselCustomDescription), customParams,thisEffectData.get("mode").asInt)
            }
            if (thisEffectData.has("phaseTime")) {
                setStripParamVal(1, getString(R.string.stCaruselP1_phaseTime), thisEffectData.get("phaseTime").asInt, 5, 30)
            }
            if (thisEffectData.has("freq")) {
                setStripParamVal(2, getString(R.string.stCaruselP2_freq), thisEffectData.get("freq").asInt, 1, 3)
            }
        }
    }
    private fun upCarusel(){
        updateStripCustom("mode") //data gets form spCustom
        updateStripParamVal("phaseTime",bind.sbStripParam1)
        updateStripParamVal("freq",bind.sbStripParam2)
    }
    //"Color Wipe" , color1 , color2 , parm1 , parm2 , bool
    private fun piColorWipe(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            var col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color1")){
                col = thisEffectData.get("color1").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setStripParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("color2")){
                col = thisEffectData.get("color2").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt }
                if (col.has("g")) { g = col.get("g").asInt }
                if (col.has("b")) { b = col.get("b").asInt }
                setStripParamColor(2 , r , g , b)
            }
            if (thisEffectData.has("delay1")) {
                setStripParamVal(1, getString(R.string.stColorWipeP1_delay1), thisEffectData.get("delay1").asInt, 1, 10)
            }
            if (thisEffectData.has("delay2")) {
                setStripParamVal(2, getString(R.string.stColorWipeP2_delay2), thisEffectData.get("delay2").asInt, 1, 10)
            }
            if (thisEffectData.has("clear")){
                setStripParamBool(1,getString(R.string.stColorWipeB1_clear),thisEffectData.get("clear").asInt)
            }
        }
    }
    private fun upColorWipe(){
        updateStripParamCol("color1",bind.edStripColor1)
        updateStripParamCol("color2",bind.edStripColor2)
        updateStripParamVal("delay1",bind.sbStripParam1)
        updateStripParamVal("delay2",bind.sbStripParam2)
        updateStripParamBool("clear",bind.swStripBool1)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Bounce bar" , color1 , parm1 , parm1
    private fun piBounceBar(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            val col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color")){
                col = thisEffectData.get("color").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setStripParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("size")) {
                setStripParamVal(1, getString(R.string.stBounceBarP1_size), thisEffectData.get("size").asInt, 1, 8)
            }
            if (thisEffectData.has("delay")) {
                setStripParamVal(2, getString(R.string.stBounceBarP2_delay), thisEffectData.get("delay").asInt, 1, 6)
            }
        }
    }
    private fun upBounceBar(){
        updateStripParamCol("color",bind.edStripColor1)
        updateStripParamVal("size",bind.sbStripParam1)
        updateStripParamVal("delay",bind.sbStripParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Chillout" , parm1 , parm2
    private fun piChillout(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("heat")) {
                setStripParamVal(1, getString(R.string.stChilloutP1_heat), thisEffectData.get("heat").asInt, 1, 15)
            }
            if (thisEffectData.has("delay")) {
                setStripParamVal(2, getString(R.string.stChilloutP2_delay), thisEffectData.get("delay").asInt, 1, 6)
            }
        }
    }
    private fun upChillout() {
        updateStripParamVal("heat",bind.sbStripParam1)
        updateStripParamVal("delay",bind.sbStripParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Comet", color , parm1 , parm2 , bool
    private fun piComet(){
        val index = bind.spStripEffect.selectedItemPosition
        defaultStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            val col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color")){
                col = thisEffectData.get("color").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setStripParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("size")) {
                setStripParamVal(1, getString(R.string.stCometP1_size), thisEffectData.get("size").asInt, 1, 7)
            }
            if (thisEffectData.has("delay")) {
                setStripParamVal(2, getString(R.string.stCometP2_delay), thisEffectData.get("delay").asInt, 1, 10)
            }
            if (thisEffectData.has("solid")){
                setStripParamBool(1,getString(R.string.stCometB1_solid),thisEffectData.get("solid").asInt)
            }
        }
    }
    private fun upComet(){
        updateStripParamCol("color",bind.edStripColor1)
        updateStripParamVal("size",bind.sbStripParam1)
        updateStripParamVal("delay",bind.sbStripParam2)
        updateStripParamBool("solid",bind.swStripBool1)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }

    private fun doPopupMenuSentences(thisView : View, pos : Int) {
        val wrapper = ContextThemeWrapper(this, R.style.BasePopupMenu)
        val sentencePopup = PopupMenu(wrapper,thisView)
        //val sentencePopup = PopupMenu(this,thisView)
        sentencePopup.menuInflater.inflate(R.menu.sentences_popup, sentencePopup.menu)
        val thisItem = bind.lvPanelSentences.getItemAtPosition(pos) as jPanelSentence
        Log.d(TAG,"PopupMenu Sentence id : ${thisItem.id} , sentence :  ${thisItem.sentence}")

        sentencePopup.setOnMenuItemClickListener {
            when (it.itemId){
                R.id.sentenceSet -> {
                    val set = JsonObject()
                    set.addProperty("cmd","SET")
                    set.addProperty("cmdId",thisItem.id)
                    Log.d(TAG,"SET sentence : $set")
                    ConnectThread(mySelectedBluetoothDevice).writeMessage(set.toString())
                }
                R.id.sentenceNew -> {
                    Log.d(TAG,"ADD sentence")
                    dialogSentenceAction(thisItem,getString(R.string.sentenceHeaderAdd))
                }
                R.id.sentenceEdit ->{
                    Log.d(TAG,"EDIT sentence")
                    dialogSentenceAction(thisItem,getString(R.string.sentenceHeaderEdit))
                }
                R.id.sentenceDelete -> {
                    Log.d(TAG,"DELETE sentence")
                    dialogSentenceAction(thisItem,getString(R.string.sentenceHeaderDelete))
                }
            }
            false
        }
        sentencePopup.show()
    }

    private  fun dialogSentenceAction(sentence : jPanelSentence, mode : String) {
        val mDialog = Dialog(this)
        Log.d(TAG, "Passed sentence ID : ${sentence.id} and mode $mode")
        Log.d(TAG, "Sentence txt: ${sentence.sentence} ")
        val sentenceIndex = sentenceList.indexOf(sentence)
        Log.d(TAG, "(HEADER) Sentence index : $sentenceIndex")
        Log.d(TAG, "text position : ${sentence.textPosition}")
        Log.d(TAG, "text effect : ${sentence.textEffect}")
        Log.d(TAG, "background : ${sentence.background}")

        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mDialog.setContentView(R.layout.ledp_dialog)//WARNING was ledp_dialog
        mDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //FONT PARAMETERS
        //header
        val tvHeader = mDialog.findViewById<View>(R.id.tvLedpHeader) as TextView
        val etSentence = mDialog.findViewById<View>(R.id.etLedpSentence) as EditText
        //font name size and decoration
        val spFontName = mDialog.findViewById<View>(R.id.spPanelFontName) as Spinner
        val spFontSize = mDialog.findViewById<View>(R.id.spPanelFontFontSize) as Spinner
        val spFontDecoration = mDialog.findViewById<View>(R.id.spPanelFontFontDecoration) as Spinner
        // font color
        val btnColor = mDialog.findViewById<View>(R.id.btnPanelFontFontColor) as Button
        val tvColor = mDialog.findViewById<View>(R.id.tvPanelFontFontColor) as TextView
        // font border type
        //val tvBorderType = mDialog.findViewById<View>(R.id.tvPanelFontBorderType) as TextView //as label now
        val spBorderType = mDialog.findViewById<View>(R.id.spPanelFontBorderType) as Spinner
        // font border color
        val btnBorderColor = mDialog.findViewById<View>(R.id.btnPanelFontBorderColor) as Button
        val tvBorderColor = mDialog.findViewById<View>(R.id.tvPanelFontBorderColor) as TextView
        // render delays
        val sbTextDelay = mDialog.findViewById<View>(R.id.sbPanelTextDelay) as SeekBar
        val sbBgDelay = mDialog.findViewById<View>(R.id.sbPanelBgDelay) as SeekBar
        // action panel buttons
        val btnConfirm = mDialog.findViewById<View>(R.id.btnPanelConfirm) as Button
        val btnCancel = mDialog.findViewById<View>(R.id.btnPanelCancel) as Button


        //TEXT POSITION PARAMETERS
        val panelTextPosition =
            mDialog.findViewById<View>(R.id.panelPanelTextPosition) as LinearLayout
        //position "name/type" Static, scroll, etc...
        //val tvTpTextPosition = mDialog.findViewById<View>(R.id.tvPanelTextPosition) as TextView //as label now
        val spTpTextPosition = mDialog.findViewById<View>(R.id.spPanelTextPosition) as Spinner
        //text position custom param
        //val rowTeCustom = mDialog.findViewById<View>(R.id.rowPanelTextCustom) as LinearLayout //Before changes
        val tvTpCustom = mDialog.findViewById<View>(R.id.tvPanelTextPositionCustom) as TextView
        val spTpCustom = mDialog.findViewById<View>(R.id.spPanelTextPositionCustom) as Spinner

        //text position param 1
        //val rowTeParam1 = mDialog.findViewById<View>(R.id.rowPanelTextParam1) as LinearLayout //Before changes
        val tvTpParam1 = mDialog.findViewById<View>(R.id.tvPanelTextPositionParam1) as TextView
        val tvTpParam1Val =
            mDialog.findViewById<View>(R.id.tvPanelTextPositionParam1Val) as TextView
        val sbTpParam1 = mDialog.findViewById<View>(R.id.sbPanelTextPositionParam1) as SeekBar
        //text position param 2
        //val rowTeParam2 = mDialog.findViewById<View>(R.id.rowPanelTextParam2) as LinearLayout //Before changes
        val tvTpParam2 = mDialog.findViewById<View>(R.id.tvPanelTextPositionParam2) as TextView
        val tvTpParam2Val =
            mDialog.findViewById<View>(R.id.tvPanelTextPositionParam2Val) as TextView
        val sbTpParam2 = mDialog.findViewById<View>(R.id.sbPanelTextPositionParam2) as SeekBar

        //TEXT EFFECT PARAMETERS
        val panelTextEffect = mDialog.findViewById<View>(R.id.panelPanelTextEffect) as LinearLayout
        // spTeTextEffect - main selector deciding which parameters should be visible
        val spTeTextEffect = mDialog.findViewById<View>(R.id.spPanelTextEffect) as Spinner
        // text effect color 1
        val btnTeColor1 = mDialog.findViewById<View>(R.id.btnPanelTextEffectColor1) as Button
        val tvTeColor1 = mDialog.findViewById<View>(R.id.tvPanelTextEffectColor1) as TextView
        // text effect param custom
        val tvTeCustom = mDialog.findViewById<View>(R.id.tvPanelTextEffectCustom) as TextView
        val spTeCustom = mDialog.findViewById<View>(R.id.spPanelTextEffectCustom) as Spinner
        //text position param 1
        val tvTeParam1 = mDialog.findViewById<View>(R.id.tvPanelTextEffectParam1) as TextView
        val tvTeParam1Val = mDialog.findViewById<View>(R.id.tvPanelTextEffectParam1Val) as TextView
        val sbTeParam1 = mDialog.findViewById<View>(R.id.sbPanelTextEffectParam1) as SeekBar
        //text position param 2
        val tvTeParam2 = mDialog.findViewById<View>(R.id.tvPanelTextEffectParam2) as TextView
        val tvTeParam2Val = mDialog.findViewById<View>(R.id.tvPanelTextEffectParam2Val) as TextView
        val sbTeParam2 = mDialog.findViewById<View>(R.id.sbPanelTextEffectParam2) as SeekBar

        //BACKGROUND Elements
        val panelBg = mDialog.findViewById<View>(R.id.panelPanelBackgroud) as LinearLayout
        // spBgEffect - main selector deciding which parameters should be visible
        val spBgEffect = mDialog.findViewById<View>(R.id.spPanelBackgrounds) as Spinner
        // color 1
        //val rowBgColor1 = mDialog.findViewById<View>(R.id.rowPanelBgColor1) as LinearLayout
        val btnBgColor1 = mDialog.findViewById<View>(R.id.btnPanelBgColor1) as Button
        val tvBgColor1 = mDialog.findViewById<View>(R.id.tvPanelBgColor1) as TextView
        // color 2
        //val rowBgColor2 = mDialog.findViewById<View>(R.id.rowPanelBgColor2) as LinearLayout
        val btnBgColor2 = mDialog.findViewById<View>(R.id.btnPanelBgColor2) as Button
        val tvBgColor2 = mDialog.findViewById<View>(R.id.tvPanelBgColor2) as TextView
        // color 3
        //val rowBgColor3 = mDialog.findViewById<View>(R.id.rowPanelBgColor3) as LinearLayout
        val btnBgColor3 = mDialog.findViewById<View>(R.id.btnPanelBgColor3) as Button
        val tvBgColor3 = mDialog.findViewById<View>(R.id.tvPanelBgColor3) as TextView
        // color 4
        //val rowBgColor4 = mDialog.findViewById<View>(R.id.rowPanelBgColor4) as LinearLayout
        val btnBgColor4 = mDialog.findViewById<View>(R.id.btnPanelBgColor4) as Button
        val tvBgColor4 = mDialog.findViewById<View>(R.id.tvPanelBgColor4) as TextView

        // custom param
        //val rowBgCustomParam = mDialog.findViewById<View>(R.id.rowPanelBgCustomParam) as LinearLayout
        val tvBgCustomParam = mDialog.findViewById<View>(R.id.tvPanelBgCustom) as TextView
        val spBgCustomParam = mDialog.findViewById<View>(R.id.spPanelBgCustom) as Spinner

        //param1
        //val rowBgParam1 = mDialog.findViewById<View>(R.id.rowPanelBgParam1) as LinearLayout
        val tvBgParam1 = mDialog.findViewById<View>(R.id.tvPanelBgParam1) as TextView
        val tvBgParam1val = mDialog.findViewById<View>(R.id.tvPanelBgParam1Val) as TextView
        val sbBgParam1 = mDialog.findViewById<View>(R.id.sbPanelBgParam1) as SeekBar
        //param2
        //val rowBgParam2 = mDialog.findViewById<View>(R.id.rowPanelBgParam2) as LinearLayout
        val tvBgParam2 = mDialog.findViewById<View>(R.id.tvPanelBgParam2) as TextView
        val tvBgParam2val = mDialog.findViewById<View>(R.id.tvPanelBgParam2Val) as TextView
        val sbBgParam2 = mDialog.findViewById<View>(R.id.sbPanelBgParam2) as SeekBar
        //param3
        //val rowBgParam3 = mDialog.findViewById<View>(R.id.rowPanelBgParam3) as LinearLayout
        val tvBgParam3 = mDialog.findViewById<View>(R.id.tvPanelBgParam3) as TextView
        val tvBgParam3val = mDialog.findViewById<View>(R.id.tvPanelBgParam3Val) as TextView
        val sbBgParam3 = mDialog.findViewById<View>(R.id.sbPanelBgParam3) as SeekBar
        // bool param1
        val tvBgParamBool1 = mDialog.findViewById<View>(R.id.tvPanelBgBool1) as TextView
        //WARNING : SwitchCompat i Switch nie to samo, to drugie wywala program, zerka w XML
        val swBgParamBool1 = mDialog.findViewById<View>(R.id.swPanelBgBool1) as SwitchCompat

        //test  buttons
        val panelTests = mDialog.findViewById<View>(R.id.panelPanelTestButtons) as LinearLayout
        val btnTest1 = mDialog.findViewById<View>(R.id.btnLedpTest1) as Button
        val btnTest2 = mDialog.findViewById<View>(R.id.btnLedpTest2) as Button
        val btnTest3 = mDialog.findViewById<View>(R.id.btnLedpTest3) as Button

        val newSentence = jPanelSentence()
        fontSizeList.clear()
        fontSizeList.addAll(resources.getStringArray(R.array.FontSize))
        fontDecorationList.clear()
        fontDecorationList.addAll(resources.getStringArray(R.array.FontDecoration))
        fontBorderTypeList.clear()
        fontBorderTypeList.addAll(resources.getStringArray(R.array.BorderType))

        spFontName.adapter = FontListAdapter(this@MainActivity, fontList)
        spFontSize.adapter = StringListAdapter(this@MainActivity, fontSizeList)
        spFontDecoration.adapter = StringListAdapter(this@MainActivity, fontDecorationList)
        spBorderType.adapter = StringListAdapter(this@MainActivity, fontBorderTypeList)
        spTpTextPosition.adapter = TextPositionListAdapter(this@MainActivity, textPositionList)
        spTeTextEffect.adapter = TextEffectListAdapter(this@MainActivity, textEffectList)
        spBgEffect.adapter = BgCalcAdapter(this@MainActivity, backgroundList)

        tvHeader.text = mode
        //------------------------------------------------------------------------------------------
        //Common functions used to build a specific interface
        fun setParamCustom(
            descriptionTarget: TextView, valuesTarget: Spinner, description: String,
            values: ArrayList<String>, index: Int
        ) {
            if (values.isNotEmpty()) {
                descriptionTarget.setVisibility(true)
                valuesTarget.setVisibility(true)
                descriptionTarget.text = description
                val dataAdapter = StringListAdapter(this, values)
                valuesTarget.adapter = dataAdapter
                if (index > values.size - 1 || index < 0) valuesTarget.setSelection(0)
                else valuesTarget.setSelection(index)
            }
        }

        fun setParamVal(
            descriptionTarget: TextView, valueInfoTarget: TextView, valueTarget: SeekBar,
            description: String, pVal: Int, pMin: Int, pMax: Int
        ) {
            descriptionTarget.setVisibility(true)
            valueTarget.setVisibility(true)
            valueInfoTarget.setVisibility(true)
            //for example seekbar param1 now onChangeListener change tvParam1Val.text
            descriptionTarget.text = description
            valueTarget.min = pMin
            valueTarget.max = pMax
            valueTarget.progress = pVal
        }

        fun setParamBool(
            descriptionTarget: TextView, boolTarget: SwitchCompat, description: String,
            value: Int
        ) {
            descriptionTarget.setVisibility(true)
            boolTarget.setVisibility(true)
            descriptionTarget.text = description
            boolTarget.isChecked = value > 0
        }

        fun getColorFromTextView(o: TextView): JsonObject {
            val ret = JsonObject()
            val colDraw = o.background as ColorDrawable
            val colInt = colDraw.color
            ret.addProperty("r", Color.red(colInt))
            ret.addProperty("g", Color.green(colInt))
            ret.addProperty("b", Color.blue(colInt))
            return ret
        }

        fun getColorFromRgb(r: Int, g: Int, b: Int): JsonObject {
            val ret = JsonObject()
            ret.addProperty("r", r)
            ret.addProperty("g", g)
            ret.addProperty("b", b)
            return ret
        }

        fun setParamColorFromColorObj(
            c: JsonObject, tvTarget: TextView,
            buttonTarget: Button, description: String
        ) {
            buttonTarget.setVisibility(true)
            tvTarget.setVisibility(true)
            buttonTarget.text = description
            var r = 128
            var g = 128
            var b = 128
            if (c.has("r")) r = c.get("r").asInt
            if (c.has("g")) g = c.get("g").asInt
            if (c.has("b")) b = c.get("b").asInt
            tvTarget.setBackgroundColor(Color.rgb(r, g, b))
        }

        //--- font and delays part -----------------------------------------------------------------
        fun getFontType(): String {
            var res = ""
            when (spFontSize.selectedItemPosition) {
                0 -> res = "s"
                1 -> res = "m"
                2 -> res = "l"
            }
            when (spFontDecoration.selectedItemPosition) {
                0 -> res += "n"
                1 -> res += "b"
                2 -> res += "i"
                3 -> res += "bi"
            }
            return res
        }

        fun getRescaledDelay(o: SeekBar): Int {
            return 60 - (o.progress * 10)
        }

        fun setRescaledDelay(target: SeekBar, baseValue: Int) {
            var res = (60 - baseValue) / 10
            if (res < 1) res = 1
            if (res > 5) res = 5
            target.progress = res
        }

        fun getBorderType(): Int {
            var borderTypeIndex = spBorderType.selectedItemPosition
            if (borderTypeIndex < 0 || borderTypeIndex > 11) borderTypeIndex = 0
            return borderTypeIndex
        }

        fun setFontIndexes() {
            var fontId = 0
            var fontType = "sn"
            if (sentence.font.has("fontId")) fontId = sentence.font.get("fontId").asInt
            if (sentence.font.has("fontType")) fontType = sentence.font.get("fontType").asString
            val fontIndex: Int = fontList.indexOfFirst { it.id == fontId }
            //first font name
            if (fontIndex > -1) spFontName.setSelection(fontIndex)
            else spFontName.setSelection(0)

            //size
            when (fontType[0]) {
                's' -> {
                    spFontSize.setSelection(0)
                }
                'm' -> {
                    spFontSize.setSelection(1)
                }
                'l' -> {
                    spFontSize.setSelection(2)
                }
                else -> {
                    spFontSize.setSelection(0)
                }
            }
            //decoration
            fontType = fontType.drop(1) //remove first
            when (fontType) {
                "n" -> {
                    spFontDecoration.setSelection(0)
                }
                "b" -> {
                    spFontDecoration.setSelection(1)
                }
                "i" -> {
                    spFontDecoration.setSelection(2)
                }
                "bi" -> {
                    spFontDecoration.setSelection(3)
                }
                else -> {
                    spFontDecoration.setSelection(0)
                }
            }
        }

        fun setFontColor() {
            if (sentence.font.has("color")) {
                val colorObj = sentence.font.getAsJsonObject("color")
                setParamColorFromColorObj(
                    colorObj,
                    tvColor,
                    btnColor,
                    getString(R.string.btnPanelFontColor)
                )
            }
        }

        fun setBorderIndexes() {
            if (sentence.font.has("borderType")) {
                val borderTypeNum = sentence.font.get("borderType").asInt
                spBorderType.setSelection(borderTypeNum)
            }
        }

        fun setBorderColor() {
            if (sentence.font.has("borderColor")) {
                val colorObj = sentence.font.getAsJsonObject("borderColor")
                setParamColorFromColorObj(
                    colorObj,
                    tvBorderColor,
                    btnBorderColor,
                    getString(R.string.btnPanelFontBorderColor)
                )
            }
        }

        //------------------------------------------------------------------------------------------
        //Interface functions , show , hide , do specific for effect
        //----Main part , where is the place for : sentence, font attributes , render speed
        fun enableInterface() {
            etSentence.isEnabled = true
            spFontName.isEnabled = true
            spFontSize.isEnabled = true
            spFontDecoration.isEnabled = true
            btnColor.isEnabled = true
            btnBorderColor.isEnabled = true
            spBorderType.isEnabled = true

            sbTextDelay.isEnabled = true
            sbBgDelay.isEnabled = true
            spTpTextPosition.isEnabled = true
            spBgEffect.isEnabled = true
        }

        fun disableInterface() {
            etSentence.isEnabled = false
            spFontName.isEnabled = false
            spFontSize.isEnabled = false
            spFontDecoration.isEnabled = false
            btnColor.isEnabled = false
            btnBorderColor.isEnabled = false
            spBorderType.isEnabled = false
            sbTextDelay.isEnabled = false
            sbBgDelay.isEnabled = false
            spTpTextPosition.isEnabled = false
            spBgEffect.isEnabled = false
        }

        //---text position part --------------------------------------------------------------------
        fun setTpPositionFromSentence() {
            var tpName = ""
            //set text position index
            if (sentence.textPosition.has("name")) tpName =
                sentence.textPosition.get("name").asString
            Log.d(TAG, "name from text position : $tpName")
            val index: Int = textPositionList.indexOfFirst { it.name == tpName }
            if (index > -1) spTpTextPosition.setSelection(index)
            else spTpTextPosition.setSelection(0)
        }

        fun hideTextPositionInterface() {
            //Ukrywa poprawnie , jednak przy ponownm włączeniu panelu pokazuje się wszystko
/*
            val childCnt : Int = panelTextEffect.childCount
            if (childCnt > 0){
                for (i in 0..childCnt-1){
                    val element : View = panelTextEffect.getChildAt(i)
                    if (element !is LinearLayout){
                        element.setVisibility(false)
                    }
                }
            }else{
                Log.d(TAG,"ERROR - > panel text effect dont have properties to set.")
            }
*/
            panelTextPosition.setVisibility(false)
            //depends on value of spTpTextPosition some parameters will be turn on
            //by default should not be visible
            //tvTpTextPosition.setVisibility(false)
            //spTpTextPosition.setVisibility(false)

            tvTpCustom.setVisibility(false)
            spTpCustom.setVisibility(false)

            tvTpParam1.setVisibility(false)
            tvTpParam1Val.setVisibility(false)
            sbTpParam1.setVisibility(false)

            tvTpParam2.setVisibility(false)
            tvTpParam2Val.setVisibility(false)
            sbTpParam2.setVisibility(false)
        }

        fun piTpStatic() {
            var pPosition = 2 //position
            val tp = sentence.textPosition
            val data: JsonObject
            if (tp.has("data")) {
                data = tp.getAsJsonObject("data").asJsonObject
                if (data.has("position")) pPosition = data.get("position").asInt
            }
            Log.d(TAG, "[TP] Static -> values : pPosition : $pPosition")
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.tpStaticCustom))
            setParamCustom(
                tvTpCustom,
                spTpCustom,
                getString(R.string.tpStaticCustomDescription),
                values,
                pPosition
            )
            panelTextPosition.setVisibility(true)
        }

        fun upTpStatic(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("position", spTpCustom.selectedItemPosition)
            return dataObj
        }

        fun piTpScroll() {
            var pScrollType = 2 //default scrollType
            val te = sentence.textPosition
            val data: JsonObject
            if (te.has("data")) {
                data = te.getAsJsonObject("data").asJsonObject
                if (data.has("scrollType")) pScrollType = data.get("scrollType").asInt
            }
            Log.d(TAG, "[TE] Scroll -> values : pScrollType : $pScrollType")
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.tpScrollCustom))
            setParamCustom(
                tvTpCustom,
                spTpCustom,
                getString(R.string.tpScrollCustomDescription),
                values,
                pScrollType
            )
            panelTextPosition.setVisibility(true)
        }

        fun upTpScroll(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("scrollType", spTpCustom.selectedItemPosition)
            return dataObj
        }

        fun piTpWordByWord() {
            var pTime = 3 //defoult time in seconds
            val te = sentence.textPosition
            val data: JsonObject
            if (te.has("data")) {
                data = te.getAsJsonObject("data").asJsonObject
                //Log.d(TAG, "Data: $data")
                if (data.has("time")) pTime = data.get("time").asInt
            }
            Log.d(TAG, "[TE] WordByWord -> values : time : $pTime")
            setParamVal(
                tvTpParam1,
                tvTpParam1Val,
                sbTpParam1,
                getString(R.string.tpWordByWordP1_timeInSec),
                pTime,
                1,
                10
            )
            panelTextPosition.setVisibility(true)
        }

        fun upTpWordByWord(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("time", sbTpParam1.progress)
            return dataObj
        }

        fun updateTextPositionData(): JsonObject {
            val thisTextPosition = spTpTextPosition.selectedItem as jPanelTextPosition
            var textPositionData = JsonObject()
            when (thisTextPosition.name) {
                "Static" -> textPositionData = upTpStatic()
                "Scroll" -> textPositionData = upTpScroll()
                "Word by word" -> textPositionData = upTpWordByWord()
            }
            return textPositionData
        }

        fun setupTextPositionInterface() {
            Log.d(TAG, "--Setup text position interface--")
            hideTextPositionInterface()
            val thisTextPosition = spTpTextPosition.selectedItem as jPanelTextPosition
            when (thisTextPosition.name) {
                "Static" -> piTpStatic()
                "Scroll" -> piTpScroll()
                "Word by word" -> piTpWordByWord()
            }
        }

        //--text effect part -----------------------------------------------------------------------
        fun setTePositionFromSentence() {
            var teName = ""
            if (sentence.textEffect.has("name")) teName = sentence.textEffect.get("name").asString
            Log.d(TAG, "name from text effect : $teName")
            val index: Int = textEffectList.indexOfFirst { it.name == teName }
            if (index > -1) spTeTextEffect.setSelection(index)
            else spTeTextEffect.setSelection(0)
        }

        fun hideTextEffectInterface() {
            panelTextEffect.setVisibility(false)

            btnTeColor1.setVisibility(false)
            tvTeColor1.setVisibility(false)

            tvTeCustom.setVisibility(false)
            spTeCustom.setVisibility(false)

            tvTeParam1.setVisibility(false)
            tvTeParam1Val.setVisibility(false)
            sbTeParam1.setVisibility(false)

            tvTeParam2.setVisibility(false)
            tvTeParam2Val.setVisibility(false)
            sbTeParam2.setVisibility(false)
        }

        fun piTeSimple() {
            var pMode = 0// default controller val , range 0..5 , at this point do nothing
            val te = sentence.textEffect
            val data: JsonObject
            if (te.has("data")) {
                data = te.getAsJsonObject("data").asJsonObject
                if (data.has("mode")) pMode = data.get("mode").asInt
            }
            Log.d(TAG, "[TE] Simple -> values : pMode : $pMode")
            setParamVal(
                tvTeParam1,
                tvTeParam1Val,
                sbTeParam1,
                getString(R.string.teSimpleP1_mode),
                pMode,
                0,
                5
            )
            panelTextEffect.setVisibility(true)
        }

        fun upTeSimple(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("mode", sbTeParam1.progress)
            return dataObj
        }

        fun piTeFireText() {
            var pCooling = 6 //default controller val, range 1..11
            var pSparking = 6//default controller val, range 1..16
            val te = sentence.textEffect
            val data: JsonObject
            if (te.has("data")) {
                data = te.getAsJsonObject("data").asJsonObject
                if (data.has("cooling")) pCooling = data.get("cooling").asInt
                if (data.has("sparking")) pSparking = data.get("sparking").asInt
            }
            Log.d(TAG, "[TE] Fire text -> values : pCooling : $pCooling , pSparking : $pSparking")
            setParamVal(
                tvTeParam1,
                tvTeParam1Val,
                sbTeParam1,
                getString(R.string.teFireTextP1_cooling),
                pCooling,
                1,
                11
            )
            setParamVal(
                tvTeParam2,
                tvTeParam2Val,
                sbTeParam2,
                getString(R.string.teFireTextP2_sparking),
                pSparking,
                1,
                16
            )
            panelTextEffect.setVisibility(true)
        }

        fun upTeFireText(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("cooling", sbTeParam1.progress)
            dataObj.addProperty("sparking", sbTeParam2.progress)
            return dataObj
        }

        fun piTeRollingBorder() {
            var pDir = 0 //default controller val , 0-roll right, 1-roll left
            val te = sentence.textEffect
            val data: JsonObject
            if (te.has("data")) {
                data = te.getAsJsonObject("data").asJsonObject
                if (data.has("dir")) pDir = data.get("dir").asInt
            }
            Log.d(TAG, "[TE] Rolling border -> values : pDir : $pDir")
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.teRollingBorderCustom))
            setParamCustom(
                tvTeCustom,
                spTeCustom,
                getString(R.string.teRollingBorderCustom_desc),
                values,
                pDir
            )
            panelTextEffect.setVisibility(true)
        }

        fun upTeRollingBorder(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("dir", spTeCustom.selectedItemPosition)
            return dataObj
        }

        fun piTeColors() {
            var pDelta = 0// default controller val , range 1..10
            val te = sentence.textEffect
            val data: JsonObject
            if (te.has("data")) {
                data = te.getAsJsonObject("data").asJsonObject
                if (data.has("delta")) pDelta = data.get("delta").asInt
            }
            Log.d(TAG, "[TE] Colors -> values : pDelta : $pDelta")
            setParamVal(
                tvTeParam1,
                tvTeParam1Val,
                sbTeParam1,
                getString(R.string.teColorsP1_delta),
                pDelta,
                1,
                10
            )
            panelTextEffect.setVisibility(true)
        }

        fun upTeColors(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("delta", sbTeParam1.progress)
            return dataObj
        }

        fun piTeNoise(){
            var pIndex = 0  // default controller val , range 0..6
            var pLow = 1    // default controller val , 1..10
            var pHigh = 1   // default controller val , 1..10
            val te = sentence.textEffect
            val data: JsonObject
            if (te.has("data")) {
                data = te.getAsJsonObject("data").asJsonObject
                if (data.has("pIndex")) pIndex = data.get("pIndex").asInt
                if (data.has("low")) pLow = data.get("low") .asInt
                if (data.has("high")) pHigh = data.get("high").asInt
            }
            Log.d(TAG, "[TE] Noise -> values : pIndex: $pIndex , low: $pLow , high: $pHigh")
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.commonPaleteList))
            setParamCustom(
                tvTeCustom,
                spTeCustom,
                getString(R.string.commonPaletteName),
                values,
                pIndex
            )
            setParamVal(
                tvTeParam1,
                tvTeParam1Val,
                sbTeParam1,
                getString(R.string.teNoiseP1_low),
                pLow,
                1,
                10
            )
            setParamVal(
                tvTeParam2,
                tvTeParam2Val,
                sbTeParam2,
                getString(R.string.teNoiseP2_high),
                pHigh,
                1,
                10
            )
            panelTextEffect.setVisibility(true)
        }
        fun upTeNoise(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("pIndex", spTeCustom.selectedItemPosition)
            dataObj.addProperty("low", sbTeParam1.progress)
            dataObj.addProperty("high", sbTeParam2.progress)
            return dataObj
        }

        fun updateTextEffectData(): JsonObject {
            val thisTextEffect = spTeTextEffect.selectedItem as jPanelTextEffect
            var textEffectData = JsonObject()
            if (thisTextEffect.type == 200) {
                when (thisTextEffect.name) {
                    "Simple" -> textEffectData = upTeSimple()
                    "Fire text" -> textEffectData = upTeFireText()
                    "Rolling border" -> textEffectData = upTeRollingBorder()
                    "Colors" -> textEffectData = upTeColors()
                    "Noise" -> textEffectData = upTeNoise()
                }
            }
            return textEffectData
        }

        fun setupTextEffectInterface() {
            Log.d(TAG, "--Setup text effect interface--")
            hideTextEffectInterface()
            val thisTextEffect = spTeTextEffect.selectedItem as jPanelTextEffect
            if (thisTextEffect.type == 200) {
                when (thisTextEffect.name) {
                    "Simple" -> piTeSimple()
                    "Fire text" -> piTeFireText()
                    "Rolling border" -> piTeRollingBorder()
                    "Colors" -> piTeColors()
                    "Noise" ->piTeNoise()
                }
            }
        }

        //--background part ------------------------------------------------------------------------
        fun setBgPositionFromSentence() {
            var bgName = ""
            //set bg index
            if (sentence.background.has("name")) bgName = sentence.background.get("name").asString
            Log.d(TAG, "name from bg : $bgName")
            val index: Int = backgroundList.indexOfFirst { it.name == bgName }
            if (index > -1) spBgEffect.setSelection(index)
            else spBgEffect.setSelection(0)
        }

        fun hideBackgroundInterface() {
            //Log.d(TAG,"Background hiding")
            panelBg.setVisibility(false)
            btnBgColor1.setVisibility(false)
            tvBgColor1.setVisibility(false)
            btnBgColor2.setVisibility(false)
            tvBgColor2.setVisibility(false)
            btnBgColor3.setVisibility(false)
            tvBgColor3.setVisibility(false)
            btnBgColor4.setVisibility(false)
            tvBgColor4.setVisibility(false)
            tvBgCustomParam.setVisibility(false)
            spBgCustomParam.setVisibility(false)
            tvBgParam1.setVisibility(false)
            tvBgParam1val.setVisibility(false)
            sbBgParam1.setVisibility(false)
            tvBgParam2.setVisibility(false)
            tvBgParam2val.setVisibility(false)
            sbBgParam2.setVisibility(false)
            tvBgParam3.setVisibility(false)
            tvBgParam3val.setVisibility(false)
            sbBgParam3.setVisibility(false)
            tvBgParamBool1.setVisibility(false)
            swBgParamBool1.setVisibility(false)
        }
        //common for all non editable mostly recorded backgrounds
        fun piBgRecordedBackgrounds(){
            panelBg.setVisibility(true) // show only main background selector
        }
        //common for all non editable mostly recorded backgrounds
        //At this point prepareDataToUpdate() check bg type , if type = 30 add parameters to data, if type = 10
        // is empty object "data":{}
        /*
        fun upBgRecordedBackgrounds():JsonObject{
            val dataObj = JsonObject()
            return dataObj
        }
        */
        fun piBgSimpleColor() {
            val bg = sentence.background
            val data: JsonObject
            if (bg.has("data")) {
                data = bg.get("data").asJsonObject
                if (data.has("color")) {
                    setParamColorFromColorObj(
                        data.getAsJsonObject("color"),
                        tvBgColor1, btnBgColor1,  getString(R.string.bgSimpleColorColor1)
                    )
                } else {
                    //esp32 def values
                    setParamColorFromColorObj(
                        getColorFromRgb(0, 0, 0),
                        tvBgColor1, btnBgColor1, getString(R.string.bgSimpleColorColor1)
                    )
                }
            }
            panelBg.setVisibility(true)
        }

        fun upBgSimpleColor(): JsonObject {
            val dataObj = JsonObject()
            val color = getColorFromTextView(tvBgColor1)
            dataObj.add("color", color)
            return dataObj
        }

        fun piBgFire1() {
            var pRows = 2   // default , flareRows
            var pChance = 3 // flareChance
            var pDecay = 6  // flareDecay
            var pDir = 2    // dir
            val bg = sentence.background
            val data: JsonObject
            if (bg.has("data")) {
                data = bg.get("data").asJsonObject
                if (data.has("flareRows")) pRows = data.get("flareRows").asInt
                if (data.has("flareChance")) pChance = data.get("flareChance").asInt
                if (data.has("flareDecay")) pDecay = data.get("flareDecay").asInt
                if (data.has("dir")) pDir = data.get("dir").asInt
            }
            Log.d(
                TAG,
                "[BG] Fire 1 -> Values :pPows : $pRows , pChance : $pChance , pDecay : $pDecay , pDir : $pDir"
            )
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.bgFire1Custom))
            setParamCustom(tvBgCustomParam, spBgCustomParam, getString(R.string.bgFire1CustomDescription), values, pDir)
            setParamVal(tvBgParam1, tvBgParam1val, sbBgParam1, getString(R.string.bgFire1P1_rows), pRows, 1, 4)
            setParamVal(tvBgParam2, tvBgParam2val, sbBgParam2, getString(R.string.bgFire1P2_chance), pChance, 1, 5)
            setParamVal(tvBgParam3, tvBgParam3val, sbBgParam3,getString(R.string.bgFire1P3_decay), pDecay, 1, 10)
            panelBg.setVisibility(true)
        }

        fun upBgFire1(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("dir", spBgCustomParam.selectedItemPosition)
            dataObj.addProperty("flareRows", sbBgParam1.progress)
            dataObj.addProperty("flareChance", sbBgParam2.progress)
            dataObj.addProperty("flareDecay", sbBgParam3.progress)
            return dataObj
        }

        fun piBgFire2() {
            var pPalette = 2    // palette
            var pHeat = 2       //heat
            val bg = sentence.background
            val data: JsonObject
            if (bg.has("data")) {
                data = bg.get("data").asJsonObject
                if (data.has("palette")) pPalette = data.get("palette").asInt
                if (data.has("heat")) pHeat = data.get("heat").asInt
            }
            Log.d(TAG, "[BG] Fire 2 -> Values : pPalette : $pPalette , pHeat : $pHeat ")
            Log.d(TAG, "WARNING : pHeat not used for now")
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.bgFire2Custom))
            setParamCustom(tvBgCustomParam, spBgCustomParam, getString(R.string.bgFire2CustomDescription), values, pPalette)
            //heat to fix
            //setParamVal(tvBgParam1, tvBgParam1val,sbBgParam1,"Heat :",pHeat ,1,5)
            panelBg.setVisibility(true)
        }

        fun upBgFire2(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("palette", spBgCustomParam.selectedItemPosition)
            //heat to fix
            //dataObj.addProperty("heat" , sbBgParam1.progress)
            return dataObj
        }

        fun piBgFire3() {
            var pPalette = 0 //palette
            var pCooling = 6 //cooling
            var pSparking = 11 //sparking
            val bg = sentence.background
            val data: JsonObject
            if (bg.has("data")) {
                data = bg.get("data").asJsonObject
                if (data.has("palette")) pPalette = data.get("palette").asInt
                if (data.has("cooling")) pCooling = data.get("cooling").asInt
                if (data.has("sparking")) pSparking = data.get("sparking").asInt
            }
            Log.d(
                TAG,
                "[BG] Fire 3 -> Values : pPalette : $pPalette , pCooling: $pCooling , pSparking : $pSparking "
            )
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.bgCustomFire3))
            setParamCustom(tvBgCustomParam, spBgCustomParam, getString(R.string.bgFire3CustomDescription), values, pPalette)
            setParamVal(tvBgParam1, tvBgParam1val, sbBgParam1,getString(R.string.bgFire3P1_cooling), pCooling, 1, 11)
            setParamVal(tvBgParam2, tvBgParam2val, sbBgParam2, getString(R.string.bgFire3P2_sparking), pSparking, 1, 16)
            panelBg.setVisibility(true)
        }

        fun upBgFire3(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("palette", spBgCustomParam.selectedItemPosition)
            dataObj.addProperty("cooling", sbBgParam1.progress)
            dataObj.addProperty("sparking", sbBgParam2.progress)
            return dataObj
        }

        fun piBgRain() {
            /*
            Esp data
            CRGB def_color1Start = CRGB(255, 255, 255); //white
            CRGB def_color1Stop = CRGB(255, 0, 0); //red;
            CRGB def_color2Start = CRGB(255, 0, 0);//red;
            CRGB def_color2Stop = CRGB(32, 0, 0);//little red;
	        uint8_t def_size = 1; //0..2 //0-small , 1-medium , 2-large
	        uint8_t def_fillBg = 0; //0..1 0-bg black , 1-last pixel color from palette
            */
            var pSize = 2             //WARNING : in Esp 1..3 , here 0..2 so remember +1 -1
            var pFlow = 0
            val bg = sentence.background
            val data: JsonObject
            if (bg.has("data")) {
                data = bg.get("data").asJsonObject
                if (data.has("color1")) {
                    setParamColorFromColorObj(
                        data.getAsJsonObject("color1"),
                        tvBgColor1, btnBgColor1, getString(R.string.bgRainColor1)
                    )
                } else {
                    //esp32 def values
                    setParamColorFromColorObj(
                        getColorFromRgb(128, 128, 128),
                        tvBgColor1, btnBgColor1, getString(R.string.bgRainColor1)
                    )
                }

                if (data.has("color2")) {
                    setParamColorFromColorObj(
                        data.getAsJsonObject("color2"),
                        tvBgColor2, btnBgColor2, getString(R.string.bgRainColor2)
                    )
                } else {
                    //esp32 def values
                    setParamColorFromColorObj(
                        getColorFromRgb(255, 0, 0),
                        tvBgColor2, btnBgColor2, getString(R.string.bgRainColor2)
                    )
                }

                if (data.has("color3")) {
                    setParamColorFromColorObj(
                        data.getAsJsonObject("color3"),
                        tvBgColor3, btnBgColor3, getString(R.string.bgRainColor3)
                    )
                } else {
                    //esp32 def values
                    setParamColorFromColorObj(
                        getColorFromRgb(255, 255, 0),
                        tvBgColor3, btnBgColor3, getString(R.string.bgRainColor3)
                    )
                }

                if (data.has("size")) pSize = data.get("size").asInt
                if (data.has("flow")) pFlow = data.get("flow").asInt
            }
            Log.d(TAG, "[BG] Rain - > Values : colors....")
            Log.d(TAG, "[BG] Rain -> Values : pSize : $pSize , pFillBg: $pFlow")
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.bgRainCustom))
            //Look pSize description
            setParamCustom(tvBgCustomParam, spBgCustomParam, getString(R.string.bgRainCustomDescription), values, pSize - 1)
            setParamBool(tvBgParamBool1, swBgParamBool1, getString(R.string.bgRainB1_blur), pFlow)
            panelBg.setVisibility(true)
        }

        fun upBgRain(): JsonObject {
            val dataObj = JsonObject()
            val color1 = getColorFromTextView(tvBgColor1)
            val color2 = getColorFromTextView(tvBgColor2)
            val color3 = getColorFromTextView(tvBgColor3)

            dataObj.add("color1", color1)
            dataObj.add("color2", color2)
            dataObj.add("color3", color3)
            //Look pSize description
            dataObj.addProperty("size", spBgCustomParam.selectedItemPosition + 1)
            var flow = 0
            if (swBgParamBool1.isChecked) flow = 1
            dataObj.addProperty("flow", flow)
            return dataObj
        }

        fun piBgStreak() {
            var pPalette = 0
            var pLength = 1
            val bg = sentence.background
            val data: JsonObject
            if (bg.has("data")) {
                data = bg.get("data").asJsonObject
                if (data.has("palette")) pPalette = data.get("palette").asInt
                if (data.has("length")) pLength = data.get("length").asInt
            }
            Log.d(TAG, "[BG] Streak -> Values : pPalette : $pPalette , pHeat : $pLength")
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.commonPaleteList))
            setParamCustom(tvBgCustomParam, spBgCustomParam, getString(R.string.bgStreakCustomDescription), values, pPalette)
            setParamVal(tvBgParam1, tvBgParam1val, sbBgParam1, getString(R.string.bgStreakP1_length), pLength, 1, 5)
            panelBg.setVisibility(true)
        }

        fun upBgStreak(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("palette", spBgCustomParam.selectedItemPosition)
            dataObj.addProperty("length", sbBgParam1.progress)
            return dataObj
        }

        fun piBgLiquidPlasma() {
            var pPalette = 2 //0..6
            var pSmooth = 3 //1..5
            var pSpeed = 3 //1..5
            val bg = sentence.background
            val data: JsonObject
            if (bg.has("data")) {
                data = bg.get("data").asJsonObject
                if (data.has("palette")) pPalette = data.get("palette").asInt
                if (data.has("smooth")) pSmooth = data.get("smooth").asInt
                if (data.has("speed")) pSpeed = data.get("speed").asInt
            }
            Log.d(
                TAG,
                "[BG] Liquid Plasma -> Values : palette: $pPalette  , smooth : $pSmooth , speed :  $pSpeed "
            )
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.commonPaleteList))
            setParamCustom(tvBgCustomParam, spBgCustomParam, getString(R.string.bgLiquidPlasmaCustomDescription), values, pPalette)
            setParamVal(tvBgParam1, tvBgParam1val, sbBgParam1, getString(R.string.bgLiquidPlasmaP1_smooth), pSmooth, 1, 5)
            setParamVal(tvBgParam2, tvBgParam2val, sbBgParam2, getString(R.string.bgLiquidPlasmaP2_speed), pSpeed, 1, 5)
            panelBg.setVisibility(true)
        }

        fun upBgLiquidPlasma(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("palette", spBgCustomParam.selectedItemPosition)
            dataObj.addProperty("smooth", sbBgParam1.progress)
            dataObj.addProperty("speed", sbBgParam2.progress)
            return dataObj
        }

        fun piBgBigPlasma() {
            var pPalette = 2 //0..6
            var pNextMove = 3 //1..5
            val bg = sentence.background
            val data: JsonObject
            if (bg.has("data")) {
                data = bg.get("data").asJsonObject
                if (data.has("palette")) pPalette = data.get("palette").asInt
                if (data.has("nextMove")) pNextMove = data.get("nextMove").asInt
            }
            Log.d(TAG, "[BG] Big plasma -> Values : palette: $pPalette  , nextMove : $pNextMove")
            val values: ArrayList<String> = ArrayList()
            values.addAll(resources.getStringArray(R.array.commonPaleteList))
            setParamCustom(tvBgCustomParam, spBgCustomParam, getString(R.string.bgBigPlasmaCustomDescription), values, pPalette)
            setParamVal(tvBgParam1, tvBgParam1val, sbBgParam1, getString(R.string.bgBigPlasmaP1_nextmove), pNextMove, 1, 5)
            panelBg.setVisibility(true)
        }

        fun upBgBigPlasma(): JsonObject {
            val dataObj = JsonObject()
            dataObj.addProperty("palette", spBgCustomParam.selectedItemPosition)
            dataObj.addProperty("nextMove", sbBgParam1.progress)
            return dataObj
        }

        fun updateBackgroundData(): JsonObject {
            val thisBg = spBgEffect.selectedItem as jPanelBackgrounds
            var bgData = JsonObject()
            if (thisBg.type == 30) {
                when (thisBg.name) {
                    "Selected color" -> bgData = upBgSimpleColor()
                    "Fire 1" -> bgData = upBgFire1()
                    "Fire 2" -> bgData = upBgFire2()
                    "Fire 3" -> bgData = upBgFire3()
                    "Rain" -> bgData = upBgRain()
                    "Streak" -> bgData = upBgStreak()
                    "Liquid plasma" -> bgData = upBgLiquidPlasma()
                    "Big plasma" -> bgData = upBgBigPlasma()
                }
            }
            //if this bg type == 10 (recorded background) , return empty object
            return bgData
        }

        fun setupBackgroundInterface() {
            Log.d(TAG, "--Setup background interface--")
            val thisBackground = spBgEffect.selectedItem as jPanelBackgrounds
            hideBackgroundInterface()
            //type 30 - live generated with params
            if (thisBackground.type == 30) {
                when (thisBackground.name) {
                    "Selected color" -> piBgSimpleColor()
                    "Fire 1" -> piBgFire1()
                    "Fire 2" -> piBgFire2()
                    "Fire 3" -> piBgFire3()
                    "Rain" -> piBgRain()
                    "Streak" -> piBgStreak()
                    "Liquid plasma" -> piBgLiquidPlasma()
                    "Big plasma" -> piBgBigPlasma()
                }
            }
            // type 10 - recorded
            if (thisBackground.type == 10) piBgRecordedBackgrounds()
        }
        //-------------------------------------------------------------------------------

        fun prepareDataToUpdate() {
            newSentence.sentence = etSentence.text.toString()
            newSentence.bgDelay = getRescaledDelay(sbBgDelay)
            newSentence.scrollDelay = getRescaledDelay(sbTextDelay)
            // font
            val fontObj = JsonObject()
            val thisFont = spFontName.selectedItem as jPanelFont //data from spinner
            fontObj.addProperty("fontId", thisFont.id)
            fontObj.addProperty("fontType", getFontType())
            fontObj.add("color", getColorFromTextView(tvColor))
            fontObj.addProperty("borderType", getBorderType())
            fontObj.add("borderColor", getColorFromTextView(tvBorderColor))
            newSentence.font = fontObj

            // text position
            val textPositionObj = JsonObject()
            val thisTextPosition = spTpTextPosition.selectedItem as jPanelTextPosition
            textPositionObj.addProperty("name", thisTextPosition.name)
            textPositionObj.addProperty("editable", thisTextPosition.editable)
            textPositionObj.addProperty("type", thisTextPosition.type)
            textPositionObj.add("data", updateTextPositionData())
            newSentence.textPosition = textPositionObj

            // text effect.....
            val textEffectObj = JsonObject()
            val thisTextEffect = spTeTextEffect.selectedItem as jPanelTextEffect
            textEffectObj.addProperty("name", thisTextEffect.name)
            textEffectObj.addProperty("editable", thisTextEffect.editable)
            textEffectObj.addProperty("type", thisTextEffect.type)
            textEffectObj.add("data", updateTextEffectData())
            newSentence.textEffect = textEffectObj

            //background
            val backgroundObj = JsonObject()
            val thisBackground = spBgEffect.selectedItem as jPanelBackgrounds
            backgroundObj.addProperty("name", thisBackground.name)
            backgroundObj.addProperty("editable", thisBackground.editable)
            backgroundObj.addProperty("type", thisBackground.type)
            // if..type == 10 , empty "data" should be returned
            // if..type == 30 , parameters is "data" should be returned
            if (thisBackground.type == 30 || thisBackground.type == 10)
                backgroundObj.add("data", updateBackgroundData())
            newSentence.background = backgroundObj
        }

        fun newSentenceId(): Int {
            var temp = 0
            sentenceList.forEach {
                if (it.id > temp) temp = it.id
            }
            temp++
            return temp
        }

        fun hideTestPanel() {
            panelTests.setVisibility(false)
            btnTest1.setVisibility(false)
            btnTest2.setVisibility(false)
            btnTest3.setVisibility(false)
        }

        fun addSentence(): Boolean {
            Log.d(TAG, "TESTING ADD : ")
            if (etSentence.text.isEmpty()) {
                Log.d(TAG, "New sentence is empty, no action")
                return false
            }
            //if (etSentence.text.isNotEmpty()) {
            val newId = newSentenceId()
            Log.d(TAG, "New sentence id : $newId")
            //PART HEADER
            newSentence.id = newId
            prepareDataToUpdate() //same step a update new sentence
            //===Handle list part and send prepared object

            sentenceList.add(newSentence)

            bind.lvPanelSentences.adapter = SentenceListAdapter(this@MainActivity, sentenceList)
            //troszke na okolo obiekt klasy jPanelSentence do stringa , string do obiektu json
            val sentenceJson = Gson().toJson(newSentence)
            val sentenceObj = Gson().fromJson(sentenceJson, JsonObject::class.java)
            //From ESP32
            //for ADD and UPDATE cmdId and id must me the same
            sentenceObj.addProperty("cmd", "ADD_SET")
            sentenceObj.addProperty("cmdId", newSentence.id)
            Log.d(TAG, "Json ADD sentence : $sentenceObj")
            ConnectThread(mySelectedBluetoothDevice).writeMessage(sentenceObj.toString())
            //}else{
            //    Log.d(TAG,"New sentence -> text no set")
            //}
            return true
        }

        fun updateSentence(): Boolean {
            if (etSentence.text.isEmpty()) {
                Log.d(TAG, "Edited sentence is empty, no action.")
                return false
            }
            Log.d(TAG, "Update sentence ")
            //if (etSentence.text.isNotEmpty()) {
            newSentence.id = sentence.id
            //Log.d(TAG,"-->before update")
            prepareDataToUpdate() //same step a add new sentence
            sentenceList[sentenceIndex] = newSentence // sentence index from header
            bind.lvPanelSentences.adapter = SentenceListAdapter(this@MainActivity, sentenceList)
            //troszke na okolo obiekt klasy jPanelSentence do stringa , string do obiektu json
            //Log.d(TAG,"-->before json")
            val sentenceJson = Gson().toJson(newSentence)
            val sentenceObj = Gson().fromJson(sentenceJson, JsonObject::class.java)
            //From ESP32
            //for ADD and UPDATE cmdId and id must me the same
            sentenceObj.addProperty("cmd", "UPDATE_SET")
            sentenceObj.addProperty("cmdId", sentence.id)
            Log.d(TAG, "Json Update sentence : $sentenceObj")
            ConnectThread(mySelectedBluetoothDevice).writeMessage(sentenceObj.toString())
            // }else{
            //    Log.d(TAG,"Update sentence -> text no set")
            // }
            return true
        }

        fun deleteSentence(): Boolean {
            val index = sentenceList.indexOf(sentence)//old data
            sentenceList.removeAt(index)
            bind.lvPanelSentences.adapter = SentenceListAdapter(this@MainActivity, sentenceList)
            val del = JsonObject()
            del.addProperty("cmd", "DELETE")
            del.addProperty("cmdId", sentence.id)
            Log.d(TAG, "Json DELETE command : $del")
            ConnectThread(mySelectedBluetoothDevice).writeMessage(del.toString())
            return true
        }

        // FONT listeners
        spFontName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                //val thisFont = spFontName.getItemAtPosition(position) as jPanelFont
                //Log.d(TAG,"Led panel font name :$thisFont")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d(TAG, "Led panel font name NOTHING selected")
            }
        }
        spFontSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                //val thisSize = spFontSize.getItemAtPosition(position)
                //Log.d(TAG,"Led panel font size :${thisSize}")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d(TAG, "Led panel font size NOTHING selected")
            }
        }
        spFontDecoration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                //val thisDecoration = spFontDecoration.getItemAtPosition(position)
                //Log.d(TAG,"Led panel font decoration :${thisDecoration}")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d(TAG, "Led panel font decoration NOTHING selected")
            }
        }
        btnColor.setOnClickListener {
            Log.d(TAG, "Lede sentence color button clicked.")
/*
            //Old color picker
            ColorPickerDialog
                .Builder(this)
                .setTitle(R.string.dialog_title_pick_color)
                .setColorShape(ColorShape.CIRCLE)
                .setDefaultColor("#ff0000")
                .setColorListener { _, colorHex ->
                    tvColor.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()

 */
            com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
                .setTitle(R.string.dlColorTitle)
                .setPreferenceName("prefPanelColorFont")
                .setPositiveButton(getString(R.string.dlColorBtnOk), ColorEnvelopeListener { envelope, _ ->
                    val color = envelope.color
                    tvColor.setBackgroundColor(color)
                })
                .attachAlphaSlideBar(false) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                //.setBottomSpace(12) // avible in : 2.2.4
                .show()
        }
        btnBorderColor.setOnClickListener {
            Log.d(TAG, "Lede border color button clicked.")
/*
            //Old color picker
            ColorPickerDialog
                .Builder(this)
                .setTitle(getString(R.string.dlColorTitle))
                .setColorShape(ColorShape.CIRCLE)
                .setDefaultColor("#ff0000")
                .setColorListener { _, colorHex ->
                    tvBorderColor.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()
 */
            com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
                .setTitle(R.string.dlColorTitle)
                .setPreferenceName("prefPanelColorBorder")
                .setPositiveButton(getString(R.string.dlColorBtnOk), ColorEnvelopeListener { envelope, _ ->
                    val color = envelope.color
                    tvBorderColor.setBackgroundColor(color)
                    //tvColor.setBackgroundColor(color)
                })
                .attachAlphaSlideBar(false) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                //.setBottomSpace(12) // avible in : 2.2.4
                .show()
        }

        // TEXT Position listeners
        spTpTextPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                when (mode) {
                    getString(R.string.sentenceHeaderAdd) -> { //ADD
                        setupTextPositionInterface()
                    }
                    getString(R.string.sentenceHeaderEdit) -> { //EDIT
                        setupTextPositionInterface()
                    }
                    getString(R.string.sentenceHeaderDelete) -> { //DELETE
                        hideTextPositionInterface()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d(TAG, "Led panel text effect name NOTHING selected")
            }
        }
        sbTpParam1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTpParam1Val.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        sbTpParam2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTpParam2Val.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        //TEXT Effect listeners
        spTeTextEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                when (mode) {
                    getString(R.string.sentenceHeaderAdd) -> { //ADD
                        setupTextEffectInterface()
                    }
                    getString(R.string.sentenceHeaderEdit) -> { //EDIT
                        setupTextEffectInterface()
                    }
                    getString(R.string.sentenceHeaderDelete) -> { //DELETE
                        hideTextEffectInterface()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d(TAG, "Led panel text effect name NOTHING selected")
            }
        }
        sbTeParam1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTeParam1Val.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        sbTeParam2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTeParam2Val.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // BACKGROUND listeners
        spBgEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                //val thisBackground = spBgEffect.getItemAtPosition(position) as jPanelBackgrounds
                //Log.d(TAG,"Background : $thisBackground")
                //setupBackgroudInterface()
                when (mode) {
                    getString(R.string.sentenceHeaderAdd) -> { //ADD
                        setupBackgroundInterface()
                    }
                    getString(R.string.sentenceHeaderEdit) -> { //EDIT
                        setupBackgroundInterface()
                    }
                    getString(R.string.sentenceHeaderDelete) -> { //DELETE
                        hideBackgroundInterface()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d(TAG, "Led panel background  NOTHING selected")
            }
        }

        sbBgParam1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBgParam1val.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        sbBgParam2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBgParam2val.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        sbBgParam3.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBgParam3val.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnBgColor1.setOnClickListener {
/*
            // Old color picker
            ColorPickerDialog
                .Builder(this)
                .setTitle(getString(R.string.chooseColor))
                .setColorShape(ColorShape.CIRCLE)
                .setDefaultColor("#ff0000")
                .setColorListener { _, colorHex ->
                    tvBgColor1.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()
 */
            com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
                .setTitle(R.string.dlColorTitle)
                .setPreferenceName("prefPanelBgColor1")
                .setPositiveButton(getString(R.string.dlColorBtnOk), ColorEnvelopeListener { envelope, _ ->
                    val color = envelope.color
                    tvBgColor1.setBackgroundColor(color)
                })
                .attachAlphaSlideBar(false) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                //.setBottomSpace(12) // avible in : 2.2.4
                .show()

        }
        btnBgColor2.setOnClickListener {
/*
            // Old color picker
            ColorPickerDialog
                .Builder(this)
                .setTitle(getString(R.string.chooseColor))
                .setColorShape(ColorShape.CIRCLE)
                .setDefaultColor("#ff0000")
                .setColorListener { _, colorHex ->
                    tvBgColor2.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()

 */
            com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
                .setTitle(R.string.dlColorTitle)
                .setPreferenceName("prefPanelBgColor2")
                .setPositiveButton(getString(R.string.dlColorBtnOk), ColorEnvelopeListener { envelope, _ ->
                    val color = envelope.color
                    tvBgColor2.setBackgroundColor(color)
                })
                .attachAlphaSlideBar(false) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                //.setBottomSpace(12) // avible in : 2.2.4
                .show()

        }
        btnBgColor3.setOnClickListener {
/*
            //Old color picker
            ColorPickerDialog
                .Builder(this)
                .setTitle(getString(R.string.chooseColor))
                .setColorShape(ColorShape.CIRCLE)
                .setDefaultColor("#ff0000")
                .setColorListener { _, colorHex ->
                    tvBgColor3.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()

 */

            com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
                .setTitle(R.string.dlColorTitle)
                .setPreferenceName("prefPanelBgColor3")
                .setPositiveButton(getString(R.string.dlColorBtnOk), ColorEnvelopeListener { envelope, _ ->
                    val color = envelope.color
                    tvBgColor3.setBackgroundColor(color)
                })
                .attachAlphaSlideBar(false) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                //.setBottomSpace(12) // avible in : 2.2.4
                .show()
        }
        btnBgColor4.setOnClickListener {
/*
            //Old color picker
            ColorPickerDialog
                .Builder(this)
                .setTitle(getString(R.string.chooseColor))
                .setColorShape(ColorShape.CIRCLE)
                .setDefaultColor("#ff0000")
                .setColorListener { _, colorHex ->
                    tvBgColor4.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()
 */
            com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
                .setTitle(R.string.dlColorTitle)
                .setPreferenceName("prefPanelBgColor4")
                .setPositiveButton(getString(R.string.dlColorBtnOk), ColorEnvelopeListener { envelope, _ ->
                    val color = envelope.color
                    tvBgColor4.setBackgroundColor(color)
                })
                .attachAlphaSlideBar(false) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                //.setBottomSpace(12) // avible in : 2.2.4
                .show()
        }

        // CONFIRM
        btnConfirm.setOnClickListener {
            Log.d(TAG, "Lede sentence confirm.")
            var result = false
            when (mode) {
                getString(R.string.sentenceHeaderAdd) -> {
                    result = addSentence()
                }
                getString(R.string.sentenceHeaderEdit) -> {
                    result = updateSentence()
                }
                getString(R.string.sentenceHeaderDelete) -> {
                    result = deleteSentence()
                }
            }
            if (result) mDialog.dismiss()
            else {
                val info =
                    Snackbar.make(it, getString(R.string.SENTENCE_REQUIRED), Snackbar.LENGTH_SHORT)
                info.show()
            }
        }
        // CANCEL
        btnCancel.setOnClickListener {
            mDialog.dismiss()
        }
        //TESTS
        btnTest1.setOnClickListener {

        }
        btnTest2.setOnClickListener {

        }
        btnTest3.setOnClickListener {

        }

        // Handle mode
        when (mode) {
            getString(R.string.sentenceHeaderAdd) -> { //ADD
                enableInterface()
                etSentence.hint = getString(R.string.SENTENCE_HINT)
                etSentence.text.clear()
                //hm...
                //przetestowac , tu raczej ustawienie domyslnych indexow
                //font,size,decoration , bordertype , itp;
                spFontName.setSelection(0) // first definied font
                spFontSize.setSelection(0)
                spFontDecoration.setSelection(0)
                spTpTextPosition.setSelection(0)
                spTeTextEffect.setSelection(0)
                spBgEffect.setSelection(0)
            }
            getString(R.string.sentenceHeaderEdit) -> { //EDIT
                enableInterface()
                etSentence.hint = getString(R.string.SENTENCE_HINT)
                etSentence.text.clear()
                etSentence.text.append(sentence.sentence)
                setFontIndexes()
                setFontColor()
                setBorderIndexes()
                setBorderColor()
                setRescaledDelay(sbBgDelay, sentence.bgDelay)
                setRescaledDelay(sbTextDelay, sentence.scrollDelay)
                setTpPositionFromSentence()
                setTePositionFromSentence()
                setBgPositionFromSentence()
            }
            getString(R.string.sentenceHeaderDelete) -> { //DELETE
                disableInterface()
                etSentence.text.clear()
                etSentence.text.append(sentence.sentence)
                setFontIndexes()
                setFontColor()
                setBorderIndexes()
                setBorderColor()
                setRescaledDelay(sbBgDelay, sentence.bgDelay)
                setRescaledDelay(sbTextDelay, sentence.scrollDelay)

                setTpPositionFromSentence()
                setTePositionFromSentence()
                setBgPositionFromSentence()
                //tu nie ma sensu : hideTextEffectInterface()  , hideBackgroundInterface()
                //bo listenery zadzialaja pozniej i tak przestawia
            }
        }

        //wonderfull .....
        hideTestPanel()
        mDialog.setCancelable(true)
        mDialog.show()
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        mDialog.window!!.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT)
    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        bind = ActivityMainBinding.inflate(layoutInflater)

        stripModeList.addAll(resources.getStringArray(R.array.StripModeList))
        stripPaletteList.addAll(resources.getStringArray(R.array.commonPaleteList))
        stripCustomList.addAll(resources.getStringArray(R.array.TestCustomParameterList))

        panelModeList.addAll(resources.getStringArray(R.array.PanelModeList))

        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        //AT Androi12 this generate unfixable for me error
        //AcceptIncommingThread().start() // listen for controlers , trying to connect to app (backward flow)
        myHandler = Handler(Looper.getMainLooper())    //handle data from  threds : AcceptIncommingThread() ,
        dataHandler = BtHandler()

        //data from StartActivity
        myDevices = intent.getParcelableArrayListExtra<BluetoothDevice>("START_DEVICE_LIST") as ArrayList<BluetoothDevice>
        Log.d(TAG, "Pushed array of bt device size : ${myDevices.size}")
        startPos = intent.getIntExtra("START_CURRENT_SELECTED",0)

        var adr: String
        var name :String

        if(!gotBtPerms(this,"[MAIN ACTIVITY][BT] onCreate")){
            Log.d(TAG,"fun : onCreate - fatal error")
            return
        }else{
            for (d in myDevices) {
                adr = d.address
                name = d.name
                Log.d(TAG, "$adr -> $name")
            }
        }

        piConnection() //prepare interface connection
        //piMain() //prepare interface main
        hideStripInterface()
        defaultStripEffectInterface()

        //WARNING FOR TEST ONLY : at this point "test data" are loaded
        sentenceList.addAll(allPanelData.sentences)
        fontList.addAll(allPanelData.fonts)
        textPositionList.addAll(allPanelData.textPositions)
        backgroundList.addAll(allPanelData.backgrounds)

        //showPanelMainSettings()
        //showPanelSenteces()
        hidePanelInterfaces()

        //--------------------------connection panel------------------------------------------------
        bind.spDevices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,  view: View, position: Int, id: Long) {
                //Toast.makeText(this@MainActivity, "DEVICE : " + parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        //----btn connect
        bind.btnConnect.setOnClickListener {
            val selectedNum = bind.spDevices.selectedItemPosition
            //mySelectedBluetoothDevice = bluetoothAdapter.getRemoteDevice("AC:67:B2:2C:D2:B2")
            mySelectedBluetoothDevice = myDevices[selectedNum]

            when {
                mySelectedBluetoothDevice.name.contains("LEDS_") -> {
                    Log.d(TAG,"Selected device is Led-strip ")
                }
                mySelectedBluetoothDevice.name.contains("LEDP_") -> {
                    Log.d(TAG,"Selected device is Led-panel")
                }
                else -> {
                    Log.d(TAG, "Unknown type of selected device.")
                }
            }


            when (espState){
                EspConnectionState.DISCONNECTED ->{
                    ConnectThread(mySelectedBluetoothDevice).start()
                }
                EspConnectionState.CONNECTION_ERROR ->{
                    ConnectThread(mySelectedBluetoothDevice).start()
                }
                EspConnectionState.CONNECTED ->{
                    ConnectThread(mySelectedBluetoothDevice).cancel()
                    espState = EspConnectionState.DISCONNECTED
                    handlePanelsVisibility()
                }
                else -> {
                    Log.d(TAG,"Unknown ESP State")
                }
            }
        }
        //------------------------main  strip settings----------------------------------------------
        //-----mode
        //bind.spStripMode.adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,stripModeList)
        bind.spStripMode.adapter = StringListAdapter(this@MainActivity,stripModeList)

        bind.spStripMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                when (position){
                    0 -> piStripModeSelectedEffect()
                    1,2 -> piStripModeRandom()
                    3 -> piStripModeColor()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        //----effects
        //Uwaga ten adapter jest z zasobow , nowy ustawiany jest w uiMain()
        //val adapterEffects = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,effectList)
        //bind.spEffect.adapter = adapterEffects
        //bind.spStripEffect = StringListAdapter
        bind.spStripEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                allStripData.config.selected = position

                val thisEffect = bind.spStripEffect.getItemAtPosition(position) as jStripEffect
                when (thisEffect.name){
                    "Beat wave" -> piBeatWave()
                    "Blend wave" -> piBlendWave()
                    "Blur" -> piBlur()
                    "Confeti" -> piConfeti()
                    "Sinelon" -> piSinelon()
                    "Bpm" -> piBpm()
                    "Juggle" -> piJuggle()
                    "Dot beat" -> piDotBeat()
                    "Easing" -> piEasing()
                    "Hyper dot" -> piHyperDot()
                    "Beat sin gradient" -> piBeatSinGradient()
                    "Fire 1" ->piFire1()
                    "Fire 1 two flames" -> piFire1TwoFlames()
                    "Worm" -> piWorm()
                    "Fire 2" -> piFire2()
                    "Noise 1" -> piNoise1()
                    "Juggle 2" -> piJuggle2()
                    "Running color dots" -> piRunningColorDots()
                    "Disco 1" -> piDisco1()
                    "Running color dots 2" -> piRunningColorDots2()
                    "Disco dots" -> piDiscoDots()
                    "Plasma" -> piPlasma()
                    "Rainbow sine" -> piRainbowSine()
                    "Fast rainbow" -> piFastRainbow()
                    "Pulse rainbow" -> piPulseRainbow()
                    "Fireworks" -> piFireworks()
                    "Fireworks 2" -> piFireworks2()
                    "Sin-neon" -> piSinNeon()
                    "Carusel" -> piCarusel()
                    "Color Wipe" -> piColorWipe()
                    "Bounce bar" -> piBounceBar()
                    "Chillout" -> piChillout()
                    "Comet" -> piComet()
                    else -> defaultStripEffectInterface()
                }
//                Toast.makeText(this@MainActivity, "E: " + parent.getItemAtPosition(position) + " : " + position, Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        //----time
        bind.sbStripTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbStripTimeVal.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        //----main color pick
        bind.btnStripColorMain.setOnClickListener {
/*
            //old colorpicker
            ColorPickerDialog
                .Builder(this)        				// Pass Activity Instance
                .setTitle(R.string.dialog_title_pick_color)           	// Default "Choose Color"
                .setColorShape(ColorShape.CIRCLE)   // Default ColorShape.CIRCLE
                .setDefaultColor("#ff0000")     // Pass Default Color
                .setColorListener { _, colorHex ->
                    val thisColorInt = Color.parseColor(colorHex)
                    bind.tvStripColorMain.setBackgroundColor(thisColorInt)
//                  Toast.makeText(this, "Main Color HEX:"+colorHex+"  ", Toast.LENGTH_SHORT).show()
                }.show()
*/

            com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
                .setTitle(R.string.dlColorTitle)
                .setPreferenceName("prefStripColorSolid")
                .setPositiveButton(getString(R.string.dlColorBtnOk), ColorEnvelopeListener { envelope, _ ->
                    val color = envelope.color
                    bind.tvStripColorMain.setBackgroundColor(color)
                })
                .attachAlphaSlideBar(false) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                //.setBottomSpace(12) // avible in : 2.2.4
                .show()
        }
        //----confirm
        bind.btnStripMainConfirm.setOnClickListener {
            val updatedColor = JsonObject()
            val objResponse = JsonObject()
            val objConfig = JsonObject()
            val objEffect = JsonObject()
            allStripData.config.mode = bind.spStripMode.selectedItemPosition
            allStripData.config.selected = bind.spStripEffect.selectedItemPosition
            allStripData.config.time = bind.sbStripTime.progress
            //now color part...
            val colDraw = bind.tvStripColorMain.background as ColorDrawable
            val colInt = colDraw.color
            allStripData.config.color.r = Color.red(colInt)
            allStripData.config.color.g = Color.green(colInt)
            allStripData.config.color.b = Color.blue(colInt)

            updatedColor.addProperty("r",Color.red(colInt))
            updatedColor.addProperty("g",Color.green(colInt))
            updatedColor.addProperty("b",Color.blue(colInt))

            val  effect =  bind.spStripEffect.selectedItem as jStripEffect
            when (effect.name){
                "Beat wave" -> upBeatWave()
                "Blend wave" -> upBlendWave()
                "Blur" -> upBlur()
                "Confeti" -> upConfeti()
                "Sinelon" -> upSinelon()
                "Bpm" -> upBpm()
                "Juggle" -> upJuggle()
                "Dot beat" -> upDotBeat()
                "Easing" -> upEasing()
                "Hyper dot" -> upHyperDot()
                "Beat sin gradient" -> upBeatSinGradient()
                "Fire 1" -> upFire1()
                "Fire 1 two flames" -> upFire1TwoFlames()
                "Worm" -> upWorm()
                "Fire 2" -> upFire2()
                "Noise 1" -> upNoise1()
                "Juggle 2" -> upJuggle2()
                "Running color dots" -> upRunningColorDots()
                "Disco 1" -> upDisco1()
                "Running color dots 2" -> upRunningColorDots2()
                "Disco dots" -> upDiscoDots()
                "Plasma" -> upPlasma()
                "Rainbow sine" -> upRainbowSine()
                "Fast rainbow" -> upFastRainbow()
                "Pulse rainbow" -> upPulseRainbow()
                "Fireworks" -> upFireworks()
                "Fireworks 2" -> upFireworks2()
                "Sin-neon" -> upSinNeon()
                "Carusel" -> upCarusel()
                "Color Wipe" -> upColorWipe()
                "Bounce bar" -> upBounceBar()
                "Chillout" -> upChillout()
                "Comet" -> upComet()
            }

            objConfig.addProperty("mode", bind.spStripMode.selectedItemPosition)
            objConfig.addProperty("selected",bind.spStripEffect.selectedItemPosition)
            objConfig.addProperty("time",bind.sbStripTime.progress)
            objConfig.add("color",updatedColor)

            val thisEffect = allStripData.effects[bind.spStripEffect.selectedItemPosition]

            objEffect.addProperty("name", thisEffect.name)
            objEffect.addProperty("editable" , thisEffect.editable)
            objEffect.add("data",thisEffect.data)

            objResponse.addProperty("cmd","UPDATE")
            objResponse.add("config",objConfig)
            objResponse.add("effect",objEffect)
            Log.d(TAG,"$objResponse")

            ConnectThread(mySelectedBluetoothDevice).writeMessage(objResponse.toString())
//            Toast.makeText(this, updatedConfig.toString(), Toast.LENGTH_LONG).show()
        }
        //------------------------Strip effect settings---------------------------------------------
        //----pick color 1
        bind.btnStripColor1.setOnClickListener {
/*
            //Old color picker
            ColorPickerDialog
                .Builder(this)        				// Pass Activity Instance
                .setTitle(R.string.dialog_title_pick_color) // Default "Choose Color"
                .setColorShape(ColorShape.CIRCLE)   // Default ColorShape.CIRCLE
                .setDefaultColor("#ff0000")     // Pass Default Color
                .setColorListener { _, colorHex ->
                    //Toast.makeText(this, "Test1 Color"+colorHex+" ", Toast.LENGTH_SHORT).show()
                    bind.edStripColor1.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()

 */
            com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
                .setTitle(R.string.dlColorTitle)
                .setPreferenceName("prefStripColor1")
                .setPositiveButton(getString(R.string.dlColorBtnOk), ColorEnvelopeListener { envelope, _ ->
                    val color = envelope.color
                    bind.edStripColor1.setBackgroundColor(color)

                })
                .attachAlphaSlideBar(false) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                //.setBottomSpace(12) // avaible in : 2.2.4
                .show()
        }
        //----pick color 2
        bind.btnStripColor2.setOnClickListener {
/*
            //Old color picker
            ColorPickerDialog
                .Builder(this)        				// Pass Activity Instance
                .setTitle(R.string.dialog_title_pick_color) // Default "Choose Color"
                .setColorShape(ColorShape.CIRCLE)   // Default ColorShape.CIRCLE
                .setDefaultColor("#ff0000")     // Pass Default Color
                .setColorListener { _, colorHex ->
                    //Toast.makeText(this, "Test1 Color"+colorHex+" ", Toast.LENGTH_SHORT).show()
                    bind.edStripColor2.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()

 */
            com.skydoves.colorpickerview.ColorPickerDialog.Builder(this)
                .setTitle(R.string.dlColorTitle)
                .setPreferenceName("prefStripColorColor2")
                .setPositiveButton(getString(R.string.dlColorBtnOk), ColorEnvelopeListener { envelope, _ ->
                    val color = envelope.color
                    bind.edStripColor2.setBackgroundColor(color)
                })
                .attachAlphaSlideBar(false) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                //.setBottomSpace(12) // avible in : 2.2.4
                .show()
        }
        //----palette pick
        bind.spStripPalette.adapter =  StringListAdapter(this@MainActivity, stripPaletteList)

        bind.spStripPalette.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        //----custom pick
        val adapterCustom = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,stripCustomList)

        bind.spStripCustom.adapter = adapterCustom
        bind.spStripCustom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        //----param 1
        bind.sbStripParam1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbStripParam1Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        //----param 2
        bind.sbStripParam2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbStripParam2Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        //----param 3
        bind.sbStripParam3.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbStripParam3Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        //----param 4
        bind.sbStripParam4.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbStripParam4Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        //----bool 1
        bind.swStripBool1.setOnCheckedChangeListener { _, b ->
            Log.d(TAG,"bool 1 state : $b")
        }
        //----bool 2
        bind.swStripBool2.setOnCheckedChangeListener { _, b ->
            Log.d(TAG,"bool 2 state : $b")
        }

        //------------------------Panel main settings-----------------------------------------------
        //-----mode
        bind.spPanelMode.adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,panelModeList)
        bind.spPanelMode.setSelection(0)
        bind.spPanelMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        //-----brightness
        bind.sbPanelBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.tvPanelBrightnessVal.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        //-----confirm new settings
        bind.btnPanelMainConfirm.setOnClickListener {
           /*
            //Some dummy test
            val setConfig = JsonObject()
            setConfig.addProperty("cmd","SET_DATA")
            setConfig.addProperty("cmdId",666)
            setConfig.addProperty("mode",bind.spPanelMode.selectedItemPosition)
            setConfig.addProperty("brightness",bind.sbPanelBrightness.progress)
            */
            val setBrightness = JsonObject()
            setBrightness.addProperty("cmd","SET_BRIGHTNESS")
            setBrightness.addProperty("newBrightnessParam",bind.sbPanelBrightness.progress)
            Log.d(TAG,"SET brightness : $setBrightness")
            ConnectThread(mySelectedBluetoothDevice).writeMessage(setBrightness.toString())
        }
        //-----header sentence list
        bind.tvSentenceListHeader.isClickable = true
        bind.tvSentenceListHeader.setOnLongClickListener {
            val wrapper = ContextThemeWrapper(this, R.style.BasePopupMenu)
            val newPopup = PopupMenu(wrapper, it)
            newPopup.menu.add(Menu.NONE, 1, 0, getString(R.string.popupSentenceNew))
            newPopup.setOnMenuItemClickListener {
                Log.d(TAG,"ADD sentence from header click")
                val thisItem = jPanelSentence()
                dialogSentenceAction(thisItem,getString(R.string.sentenceHeaderAdd))
                false
            }
            newPopup.show()
            false
        }
        //----Panel sentence list
        bind.lvPanelSentences.isClickable = true
        bind.lvPanelSentences.adapter = SentenceListAdapter(this@MainActivity,sentenceList)
        bind.lvPanelSentences.setOnItemLongClickListener { _, view, position, _ ->
            doPopupMenuSentences(view,position)
            false
        }
        //------------------------test panel--------------------------------------------------------
        // TEST PANEL///
        bind.btnTest1.setOnClickListener {

        }
        bind.btnTest2.setOnClickListener {

        }
        bind.btnTest3.setOnClickListener {

        }
        bind.btnTest4.setOnClickListener {

        }
    }
}