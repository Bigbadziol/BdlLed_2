package com.example.bdlled_02


import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SwitchCompat
import com.example.bdlled_02.adapters.FontListAdapter
import com.example.bdlled_02.adapters.SentenceListAdapter
import com.example.bdlled_02.databinding.ActivityMainBinding
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.github.dhaval2404.colorpicker.util.setVisibility
import com.google.gson.Gson
import com.google.gson.JsonObject
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

var allStripData = Gson().fromJson(jsonStripDataTest_big,jStripData::class.java)
var allPanelData = Gson().fromJson(jsonPanelDataTest_small,jPanelData::class.java)


class MainActivity : AppCompatActivity(){
    private lateinit var bind : ActivityMainBinding
    var myDevices : ArrayList<BluetoothDevice> = ArrayList() //list form start activity
    var startPos : Int = 0 // position in list , current sellected device
    var espState : EspConnectionState = EspConnectionState.DISCONNECTED

    var stripModeList : ArrayList<String> = ArrayList() //load data from resources in onCreate
    var stripPaletteList : ArrayList<String> = ArrayList()  //load data from resources in onCreate
    var stripCustomList :  ArrayList<String> = ArrayList()  //nad tym tez trzeba popracowac, znaczy sie wyjebac

    var panelModeList : ArrayList<String> = ArrayList() //load data from resources in onCreate
    var sentenceList: ArrayList<jPanelSentence> = ArrayList()
    var fontList : ArrayList<jPanelFont> = ArrayList()


    //----------------------------------------------------------------------------------------------
    private inner class BtHandler : Handler(){
        var allMessage : String =""
        var tmp: String=""
        override fun handleMessage(msg: Message) {
            var readBuf = msg.obj as String
            //Log.d("DEBUG_INSIDE",readBuf.length.toString())
            when (msg.what){
                1 ->{
                    //Log.d("INSIDE_BUF",readBuf.length.toString())
                    if (readBuf.length == 330){
                        allMessage += readBuf
                    }else{
                        allMessage += readBuf
                        if (allMessage.length-1 > 0) {
                            jsonStripDataTest_big = allMessage.substring(0, allMessage.length - 1)
                            allStripData = Gson().fromJson(jsonStripDataTest_big ,jStripData::class.java)
                            allMessage = ""
                            Log.d("DEBUG_INSIDE","Data loaded system alive")
                            piStripMain()
                        }
                    }
                }
            }
            super.handleMessage(msg)
        }
    }
    //----------------------------------------------------------------------------------------------
    private inner class AcceptIncommingThread() : Thread() {
        // val serverSocket: BluetoothServerSocket?
        val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            //bluetoothAdapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME,uuid)
            bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME,uuid)
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
                    //manageMyConnectedSocket(it)
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
    //----------------------------------------------------------------------------------------------
    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        override fun run() {
            var inputStream = socket.inputStream
            var buffer = ByteArray(10240)
            var bytes = 0
            var thisMessage: String = "" //      this var is init inside old version
            Log.d("DEBUG_ESP", "Waiting for data, ...")
            while (true) {
                try {
                    /*
                    //OLD VERSION
                    bytes = inputStream.read(buffer, bytes, 10240 - bytes)
                    val thisMessage= String(buffer).substring(0, bytes-1) //linuxy/windowsy , znak konca
                    bytes = 0
                     */
                    bytes = inputStream.read(buffer)
                    //Log.d("DEBUG_DATA_SIZE", bytes.toString())
                    thisMessage  = String(buffer, 0, bytes)
                    //if (bytes == -1 || bytes == 0 || bytes == null){ Log.d("DBT_S", "WHY ?????") }
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
    private inner class ConnectThread(device: BluetoothDevice): Thread() {
        //        private var newSocket = device.createRfcommSocketToServiceRecord(uuid)
        private var newSocket = device.createInsecureRfcommSocketToServiceRecord(uuid)
        var buffer = ByteArray(10240)
        var bytes: Int = 0
        override fun run() {
            try {
                Log.d("DEBUG_APP", "Connecting socket")
                myHandler.post {
                    //bind.connectedOrNotTextView.text = "Connecting..." //OLD
                    espState = EspConnectionState.CONNECTING
                    handlePanelsVisibility()
                }
                appSocket = newSocket
                appSocket.connect()

                Log.d("DEBUG_APP", "Socket connected")
                myHandler.post {
                    //bind.connectedOrNotTextView.text = "Connected" //OLD
                    espState = EspConnectionState.CONNECTED
                    handlePanelsVisibility()
                }
                ConnectedThread(appSocket).start()
                //ConnectThread(mySelectedBluetoothDevice).writeMessage("""{"cmd" : "DATA_TEST" }""")
                ConnectThread(mySelectedBluetoothDevice).writeMessage("""{"cmd" : "DATA_PLEASE" }""")
            }catch (e1: Exception){
                Log.d("DEBUG_APP", "Error connecting socket, $e1")
                myHandler.post {
                    //bind.connectedOrNotTextView.text = "Connection failed" //OLD
                    espState = EspConnectionState.CONNECTION_ERROR
                    handlePanelsVisibility()
                }
            }
            // ConnectedThread w teorii, ten kod w tym miejscu zwyczajnie nie jest odpalany
            // Na pale osobny wątek w pętli głownej

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
    /*
        Prepare main settings interface : turn on visibility of components,
        get data from json
     */
    private fun handlePanelsVisibility(){
        when (espState){
            EspConnectionState.DISCONNECTED -> {
                hideMainInterface()
                hideStripEffectInterface()
                bind.btnConnect.isEnabled = true
                bind.spDevices.isEnabled = true
                bind.btnConnect.text = getString(R.string.iConnect)
            }

            EspConnectionState.CONNECTING ->{
                hideMainInterface()
                hideStripEffectInterface()
                bind.btnConnect.isEnabled = false
                bind.spDevices.isEnabled = false
                bind.btnConnect.text =getString(R.string.iConnect)

            }
            EspConnectionState.CONNECTED -> {
                bind.btnConnect.isEnabled = true
                bind.spDevices.isEnabled = false
                bind.btnConnect.text =getString(R.string.iDisconnect)
            }
            EspConnectionState.CONNECTION_ERROR ->{
                bind.btnConnect.isEnabled = true
                bind.spDevices.isEnabled = true
                bind.btnConnect.text =getString(R.string.iConnect)
            }
        }
        bind.tvStatus.text = espState.description
    }

    private fun piConnection(){
        //CONNECTION PART
        //now from Bluetooth device list  to string list to device adapter....
        var myDevicesNames = arrayOf<String>()
        for (d in myDevices){
            val name = d.name
            myDevicesNames += name
        }
        val adapterDevices = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,myDevicesNames)
        bind.spDevices.adapter = adapterDevices
        bind.spDevices.setSelection(startPos)
    }

    private fun piStripMain(){
        var effectNames = arrayOf<String>()
        for (e in allStripData.effects.indices){
            effectNames += allStripData.effects[e].name
        }
        val adapterEffectNames = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,effectNames)

        with(
            bind,
            {
                //visibility
                lbStripMode.setVisibility(true)
                spStripMode.setVisibility(true)
                lbStripEffect.setVisibility(true)
                spStripEffect.setVisibility(true)
                lbStripTime.setVisibility(true)
                lbStripTimeVal.setVisibility(true)
                sbStripTime.setVisibility(true)
                sbStripTime.min = 20
                sbStripTime.max = 120
                btnStripColorMain.setVisibility(true)
                tvStripColorMain.setVisibility(true)
                btnStripMainConfirm.setVisibility(true)
                panelStripMainSettings.setVisibility(true)
                //parameters
                spStripMode.setSelection(allStripData.config.mode, false)
                if (effectNames.size > 0) {
                    spStripEffect.adapter = adapterEffectNames
                    spStripEffect.setSelection(allStripData.config.selected)
                }else{
                    lbStripEffect.setVisibility(false)
                    spStripEffect.setVisibility(false)
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
            },
        )

    }

    private fun hideMainInterface(){
        bind.panelStripMainSettings.setVisibility(false)
    }

    private fun piPanelMain(){
        //bind.spPanelMode.adapter = ArrayAdapter
        for (s in allPanelData.sentences.indices){
            sentenceList.add(allPanelData.sentences[s])
        }

        for (s in allPanelData.fonts.indices){
            fontList.add(allPanelData.fonts[s])
        }

        with(bind,{
            lbPanelMode.setVisibility(true)
            spPanelMode.setVisibility(true)
            btnPanelMainConfirm.setVisibility(true)
            lvPanelSentences.setVisibility(true)
        })
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
    private fun hideStripEffectInterface(){
        bind.tvStripEffectName.text = resources.getString(R.string.tvEffectName)
        bind.edStripColor1.setBackgroundColor(Color.parseColor("#000000"))
        bind.edStripColor2.setBackgroundColor(Color.parseColor("#000000"))
        bind.lbStripPalette.text = resources.getString(R.string.lbPalette)
        bind.spStripPalette.setSelection(0)
        //TAKIE TROCHE NA PALE
        bind.lbStripCustom.text = resources.getString(R.string.lbCustom)
/*
        //zaskakujące, ale To w chwili onCreate jest nullem
        bind.spCustom.adapter = ArrayAdapter(this,
                                                android.R.layout.simple_spinner_dropdown_item,
                                                resources.getStringArray(R.array.TestCustomParameterList))
 */
        bind.spStripCustom.setSelection(0)
        bind.lbStripParam1.text = resources.getString(R.string.lbParam1)
        clearStripParamVal(bind.sbStripParam1)
        bind.lbStripParam2.text = resources.getString(R.string.lbParam2)
        clearStripParamVal(bind.sbStripParam2)
        bind.lbStripParam3.text = resources.getString(R.string.lbParam3)
        clearStripParamVal(bind.sbStripParam3)
        bind.lbStripParam4.text = resources.getString(R.string.lbParam4)
        clearStripParamVal(bind.sbStripParam4)

        bind.lbStripBool1.text = resources.getString(R.string.lbBool1)
        bind.swStripBool1.isChecked = false

        bind.lbStripBool2.text = resources.getString(R.string.lbBool2)
        bind.swStripBool2.isChecked = false

/*
        //tutaj pewnie dodatkowo ukrywane są wiersze tabeli , problem się pojawia z
        //dostępem do nich , zatem rozwiązanie na pałe , zwyczajnie ręcznie powyłączać elementy
        for (i in bind.panelEffect.children ){
            i.setVisibility(false)
        }
*/
        //wersja na pałe
        bind.tvStripEffectName.setVisibility(false)
        bind.btnStripColor1.setVisibility(false)
        bind.edStripColor1.setVisibility(false)
        bind.btnStripColor2.setVisibility(false)
        bind.edStripColor2.setVisibility(false)
        bind.lbStripPalette.setVisibility(false)
        bind.spStripPalette.setVisibility(false)
        bind.lbStripCustom.setVisibility(false)
        bind.spStripCustom.setVisibility(false)

        bind.lbStripParam1.setVisibility(false)
        bind.lbStripParam1Val.setVisibility(false)
        bind.sbStripParam1.setVisibility(false)

        bind.lbStripParam2.setVisibility(false)
        bind.lbStripParam2Val.setVisibility(false)
        bind.sbStripParam2.setVisibility(false)

        bind.lbStripParam3.setVisibility(false)
        bind.lbStripParam3Val.setVisibility(false)
        bind.sbStripParam3.setVisibility(false)

        bind.lbStripParam4.setVisibility(false)
        bind.lbStripParam4Val.setVisibility(false)
        bind.sbStripParam4.setVisibility(false)

        bind.lbStripBool1.setVisibility(false)
        bind.swStripBool1.setVisibility(false)

        bind.lbStripBool2.setVisibility(false)
        bind.swStripBool2.setVisibility(false)

        bind.btnStripEffectConfirm.setVisibility(false)
    }
    private fun showStripConfirmButton(){
        bind.btnStripEffectConfirm.setVisibility(true)
    }
    private fun setStripEffectName(name : String ){
        bind.panelEffect.setVisibility(visible = true)
        bind.tvStripEffectName.setVisibility(visible = true)
        bind.tvStripEffectName.text = name
    }
    private fun setStripParamColor(pColorNum : Int, r :Int, g : Int, b : Int){
        when (pColorNum){
            1 -> {
                bind.btnStripColor1.setVisibility(true)
                bind.edStripColor1.setVisibility(true)
                bind.edStripColor1.setBackgroundColor(Color.rgb(r,g,b))
            }
            2 -> {
                bind.btnStripColor2.setVisibility(true)
                bind.edStripColor2.setVisibility(true)
                bind.edStripColor2.setBackgroundColor(Color.rgb(r,g,b))
            }
        }
    }
    private fun setStripPalette(index : Int){
        bind.lbStripPalette.setVisibility(true)
        bind.spStripPalette.setVisibility(true)
        if (index > bind.spStripPalette.count - 1) bind.spStripPalette.setSelection(0,false)
        else bind.spStripPalette.setSelection(index,false)
        //   Toast.makeText(this,"Count P:" +bind.spPalette.count,Toast.LENGTH_SHORT).show()
    }
    private fun setStripCustom(desc : String, elem : Array<String>, index : Int){
        if (elem.isNotEmpty()){
            bind.lbStripCustom.setVisibility(true)
            bind.lbStripCustom.text = desc
            bind.spStripCustom.setVisibility(true)
            val aC = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,elem)
            bind.spStripCustom.adapter = aC
            if (index > elem.size -1) bind.spStripCustom.setSelection(0)
            else bind.spStripCustom.setSelection(index)
        }
    }
    private fun setStripParamVal(pNum : Int, desc : String, pVal : Int, pMin : Int, pMax : Int){
        when (pNum){
            1 -> {
                bind.lbStripParam1.setVisibility(true)
                bind.lbStripParam1Val.setVisibility(true)
                bind.sbStripParam1.setVisibility(true)
                bind.lbStripParam1.text = desc
                bind.sbStripParam1.min = pMin
                bind.sbStripParam1.max = pMax
                bind.sbStripParam1.progress = pVal
            }
            2->{
                bind.lbStripParam2.setVisibility(true)
                bind.lbStripParam2Val.setVisibility(true)
                bind.sbStripParam2.setVisibility(true)
                bind.lbStripParam2.text = desc
                bind.sbStripParam2.min = pMin
                bind.sbStripParam2.max = pMax
                bind.sbStripParam2.progress = pVal
            }
            3->{
                bind.lbStripParam3.setVisibility(true)
                bind.lbStripParam3Val.setVisibility(true)
                bind.sbStripParam3.setVisibility(true)
                bind.lbStripParam3.text = desc
                bind.sbStripParam3.min = pMin
                bind.sbStripParam3.max = pMax
                bind.sbStripParam3.progress = pVal
            }
            4->{
                bind.lbStripParam4.setVisibility(true)
                bind.lbStripParam4Val.setVisibility(true)
                bind.sbStripParam4.setVisibility(true)
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
        var tmpBool : Boolean = false
        if (state == 1) tmpBool = true
        when (pNum){
            1-> {
                bind.lbStripBool1.setVisibility(true)
                bind.lbStripBool1.text = desc
                bind.swStripBool1.setVisibility(true)
                bind.swStripBool1.isChecked = tmpBool
            }
            2-> {
                bind.lbStripBool2.setVisibility(true)
                bind.lbStripBool2.text = desc
                bind.swStripBool2.setVisibility(true)
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
            if (sw.isChecked == true ) tmpBoolAsInt = 1
            thisEffect.data.addProperty(parmName, tmpBoolAsInt)
        } else{
            Toast.makeText(this, "Effect dont have $parmName parameter", Toast.LENGTH_SHORT).show()
        }
    }

    // This methods updates data structures in APP and send to ESP modified effect
    private fun prepareStripUpdatedData() : String {
        val thisEffect = allStripData.effects[bind.spStripEffect.selectedItemPosition]
        val toSendEffect = JsonObject()
        toSendEffect.addProperty("cmd","UPDATE_EFFECT")
        toSendEffect.addProperty("name", thisEffect.name)
        toSendEffect.addProperty("editable" , thisEffect.editable)
        toSendEffect.add("data",thisEffect.data)
        return toSendEffect.toString()
    }

    //UWAGA !!! w buttonie funkcjonuje nadal stara wersja z pierwszych testow
    private fun prepareStripUpdatedConfig() : String {
        val toSendEffect = JsonObject()
        toSendEffect.addProperty("cmd","UPDATE_CONFIG")
        toSendEffect.addProperty("mode",bind.spStripMode.selectedItemPosition) //przerobic parm custom by bylo zgodnie
        toSendEffect.addProperty("selected",bind.spStripEffect.selectedItemPosition)
        updateStripParamCol("color",bind.tvStripColorMain)
        updateStripParamVal("time",bind.sbStripTime)
        bind.lbTest.text =  toSendEffect.toString()
        return toSendEffect.toString()
    }

    // "Beat wave" parm1 , parm2 , parm3 , parm4
    private fun piBeatWave(){
        val index = bind.spStripEffect.selectedItemPosition
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pulse1")) {
                setStripParamVal(1, "Pulse 1:", thisEffectData.get("pulse1").asInt, 1, 30)
            }
            if (thisEffectData.has("pulse2")) {
                setStripParamVal(2, "Pulse 2:", thisEffectData.get("pulse2").asInt, 1, 30)
            }
            if (thisEffectData.has("pulse3")) {
                setStripParamVal(3, "Pulse 3:", thisEffectData.get("pulse3").asInt, 1, 30)
            }
            if (thisEffectData.has("pulse4")) {
                setStripParamVal(4, "Pulse 4:", thisEffectData.get("pulse4").asInt, 1, 30)
            }
            showStripConfirmButton()
        }else{
            Toast.makeText(this,"This "+allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("speed")) {
                setStripParamVal(1, "Speed:", thisEffectData.get("speed").asInt, 1, 12)
            }
            if (thisEffectData.has("mH1")) {
                setStripParamVal(2, "Step 1:", thisEffectData.get("mH1").asInt, 1, 24)
            }
            if (thisEffectData.has("mH2")) {
                setStripParamVal(3, "Step 2:", thisEffectData.get("mH2").asInt, 1, 24)
            }
            showStripConfirmButton()
        }else{
            Toast.makeText(this,"This "+allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("speed")) {
                setStripParamVal(1, "Speed:", thisEffectData.get("speed").asInt, 1, 10)
            }
            if (thisEffectData.has("o1")) {
                setStripParamVal(2, "Offset 1:", thisEffectData.get("o1").asInt, 1, 20)
            }
            if (thisEffectData.has("o2")) {
                setStripParamVal(3, "Offset 2:", thisEffectData.get("o2").asInt, 1, 20)
            }
            if (thisEffectData.has("o3")) {
                setStripParamVal(4, "Offset 3:", thisEffectData.get("o3").asInt, 1, 20)
            }
            showStripConfirmButton()
        }else{
            Toast.makeText(this,"This "+allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("fade")) {
                setStripParamVal(1, "Fade :", thisEffectData.get("fade").asInt, 1, 16)
            }
            if (thisEffectData.has("mDiff")) {
                setStripParamVal(2, "Ofset 1 :", thisEffectData.get("mDiff").asInt, 1, 15)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("bpm")) {
                setStripParamVal(1, "Bpm :", thisEffectData.get("bpm").asInt, 5, 32)
            }
            if (thisEffectData.has("fade")) {
                setStripParamVal(2, "Fade:", thisEffectData.get("fade").asInt, 5, 25)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("bpm")) {
                setStripParamVal(1, "Speed :", thisEffectData.get("bpm").asInt, 10, 120)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("stepHue")) {
                setStripParamVal(1, "Step :", thisEffectData.get("stepHue").asInt, 1, 8)
            }
            if (thisEffectData.has("fade")) {
                setStripParamVal(2, "Fade :", thisEffectData.get("fade").asInt, 5, 32)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
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
                setStripParamVal(1, "Bpm :", thisEffectData.get("bpm").asInt, 5, 32)
            }
            if (thisEffectData.has("fadeMod")) {
                setStripParamVal(2, "Fade:", thisEffectData.get("fadeMod").asInt, 5, 25)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
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
                setStripParamVal(1, "Fade :", thisEffectData.get("multiplier").asInt, 1, 8)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
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
                setStripParamVal(1, "Fade :", thisEffectData.get("bpm").asInt, 1, 20)
            }
            if (thisEffectData.has("low")) {
                setStripParamVal(2, "Low :", thisEffectData.get("low").asInt, 5, 50)
            }
            if (thisEffectData.has("high")) {
                setStripParamVal(3, "High :", thisEffectData.get("high").asInt, 100, 200)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("start")) {
                setStripParamVal(1, "Speed1 :", thisEffectData.get("start").asInt, 1, 16)
            }
            if (thisEffectData.has("end")) {
                setStripParamVal(2, "Speed2 :", thisEffectData.get("end").asInt, 1, 16)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("cooling")) {
                setStripParamVal(1, "Cooling :", thisEffectData.get("cooling").asInt, 20, 100)
            }
            if (thisEffectData.has("sparking")) {
                setStripParamVal(2, "Sparking:", thisEffectData.get("sparking").asInt, 50, 200)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("cooling")) {
                setStripParamVal(1, "Cooling :", thisEffectData.get("cooling").asInt, 20, 100)
            }
            if (thisEffectData.has("sparking")) {
                setStripParamVal(2, "Sparking:", thisEffectData.get("sparking").asInt, 50, 200)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("adjust")) {
                setStripParamVal(1, "Adjust :", thisEffectData.get("adjust").asInt, 1, 24)
            }
            if (thisEffectData.has("nextBlend")) {
                setStripParamVal(2, "Next blend :", thisEffectData.get("nextBlend").asInt, 5, 30)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data

            if (thisEffectData.has("dir")) {
                val customParams = arrayOf(
                    "right to left",
                    "left to right",
                    "both sites"
                )
                setStripCustom("Direction :", customParams,thisEffectData.get("dir").asInt)
            }
            if (thisEffectData.has("intensity")) {
                setStripParamVal(1, "Intensity :", thisEffectData.get("intensity").asInt, 1, 10)
            }
            if (thisEffectData.has("speed")) {
                setStripParamVal(2, "Speed :", thisEffectData.get("speed").asInt, 1, 20)
            }

            showStripConfirmButton()
        }else{
            Toast.makeText(this,"This "+allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("low")) {
                setStripParamVal(1, "Low :", thisEffectData.get("low").asInt, 1, 10)
            }
            if (thisEffectData.has("high")) {
                setStripParamVal(2, "High :", thisEffectData.get("high").asInt, 11, 20)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("dots")) {
                setStripParamVal(1, "Dots :", thisEffectData.get("dots").asInt, 1, 10)
            }
            if (thisEffectData.has("beat")) {
                setStripParamVal(2, "Beat :", thisEffectData.get("beat").asInt, 1, 20)
            }
            if (thisEffectData.has("fade")) {
                setStripParamVal(3, "Fade :", thisEffectData.get("fade").asInt, 1, 10)
            }
            showStripConfirmButton()
        }else{
            Toast.makeText(this,"This "+allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("dir")) {
                val customParams = arrayOf(
                    "left to right",
                    "right to left"
                )
                setStripCustom("Direction :", customParams,thisEffectData.get("dir").asInt)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("flash")) {
                setStripParamVal(1, "Flash :", thisEffectData.get("flash").asInt, 1, 10)
            }

            showStripConfirmButton()
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
        hideStripEffectInterface()
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
            }
            if (thisEffectData.has("bgBright")) {
                setStripParamVal(1, "Fade :", thisEffectData.get("bgBright").asInt, 0, 50)
            }
            if (thisEffectData.has("bgStatic")){
                setStripParamBool(1,"Static :",thisEffectData.get("bgStatic").asInt)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("phaseTime")) {
                setStripParamVal(1, "Next phase(sec) :", thisEffectData.get("phaseTime").asInt, 5, 30)
            }
            showStripConfirmButton()
        }
    }
    private fun upDiscoDots() {
        updateStripParamVal("phaseTime",bind.sbStripParam1)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    // "Plasma" palette , parm1 , parm2 , parm 3
    private fun piPlasma(){
        val index = bind.spStripEffect.selectedItemPosition
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setStripPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("low")) {
                setStripParamVal(1, "Low :", thisEffectData.get("low").asInt, -128, -32)
            }
            if (thisEffectData.has("high")) {
                setStripParamVal(2, "High :", thisEffectData.get("high").asInt, 32, 128)
            }
            if (thisEffectData.has("calcMod")) {
                setStripParamVal(3, "Speed :", thisEffectData.get("calcMod").asInt, 1, 5)
            }
            showStripConfirmButton()
        }
    }
    private fun upPlasma(){
        updateStripPalette("pIndex")
        updateStripParamVal("low",bind.sbStripParam1)
        updateStripParamVal("high",bind.sbStripParam2)
        updateStripParamVal("calcMod",bind.sbStripParam3)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Rainbow sine" , parm1 , parm2
    private fun piRainbowSine(){
        val index = bind.spStripEffect.selectedItemPosition
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("speed")) {
                setStripParamVal(1, "Speed :", thisEffectData.get("speed").asInt, 8, 48)
            }
            if (thisEffectData.has("hueStep")) {
                setStripParamVal(2, "Step :", thisEffectData.get("hueStep").asInt, 2, 16)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("speed")) {
                setStripParamVal(1, "Speed :", thisEffectData.get("speed").asInt, 1, 10)
            }
            if (thisEffectData.has("delta")) {
                setStripParamVal(2, "Delta :", thisEffectData.get("delta").asInt, 1, 10)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data

            if (thisEffectData.has("dir")) {
                val customParams = arrayOf(
                    "forward",
                    "backward",
                )
                setStripCustom("Direction :", customParams,thisEffectData.get("dir").asInt)
            }
            if (thisEffectData.has("rot")) {
                setStripParamVal(1, "Rotation :", thisEffectData.get("rot").asInt, 1, 5)
            }
            if (thisEffectData.has("hue")) {
                setStripParamVal(2, "Hue:", thisEffectData.get("hue").asInt, 1, 20)
            }
            if (thisEffectData.has("delay")) {
                setStripParamVal(3, "Delay :", thisEffectData.get("delay").asInt, 10, 50)
            }
            showStripConfirmButton()
        }else{
            Toast.makeText(this,"This "+allStripData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("size")) {
                setStripParamVal(1, "Size :", thisEffectData.get("size").asInt, 1, 9)
            }
            if (thisEffectData.has("speed")) {
                setStripParamVal(2, "Speed :", thisEffectData.get("speed").asInt, 1, 3)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("life")) {
                setStripParamVal(1, "Life :", thisEffectData.get("life").asInt, 1, 6)
            }
            if (thisEffectData.has("fade")) {
                setStripParamVal(2, "Fade :", thisEffectData.get("fade").asInt, 1, 6)
            }
            showStripConfirmButton()
        }
    }
    private fun upFireworks2(){
        updateStripParamVal("life",bind.sbStripParam1)
        updateStripParamVal("fade",bind.sbStripParam2)
    }
    //"Sin-neon" , parm1
    private fun piSinNeon(){
        val index = bind.spStripEffect.selectedItemPosition
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("phaseTime")) {
                setStripParamVal(1, "Next phase(sec) :", thisEffectData.get("phaseTime").asInt, 10, 30)
            }
            showStripConfirmButton()
        }
    }
    private fun upSinNeon(){
        updateStripParamVal("phaseTime",bind.sbStripParam1)
    }
    //"Carusel" , parm1 , parm1
    private fun piCarusel(){
        val index = bind.spStripEffect.selectedItemPosition
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("phaseTime")) {
                setStripParamVal(1, "Next phase(sec) :", thisEffectData.get("phaseTime").asInt, 5, 30)
            }
            if (thisEffectData.has("freq")) {
                setStripParamVal(2, "Frequency :", thisEffectData.get("freq").asInt, 1, 3)
            }
            showStripConfirmButton()
        }

    }
    private fun upCarusel(){
        updateStripParamVal("phaseTime",bind.sbStripParam1)
        updateStripParamVal("freq",bind.sbStripParam2)
    }
    //"Color Wipe" , color1 , color2 , parm1 , parm2 , bool
    private fun piColorWipe(){
        val index = bind.spStripEffect.selectedItemPosition
        hideStripEffectInterface()
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
                setStripParamVal(1, "Delay 1:", thisEffectData.get("delay1").asInt, 10, 100)
            }
            if (thisEffectData.has("delay2")) {
                setStripParamVal(2, "Delay 2:", thisEffectData.get("delay2").asInt, 10, 100)
            }
            if (thisEffectData.has("clear")){
                setStripParamBool(1,"Clear :",thisEffectData.get("clear").asInt)
            }

            showStripConfirmButton()
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
        hideStripEffectInterface()
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
                setStripParamVal(1, "Size :", thisEffectData.get("size").asInt, 3, 10)
            }
            if (thisEffectData.has("delay")) {
                setStripParamVal(2, "Delay :", thisEffectData.get("delay").asInt, 10, 250)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
        setStripEffectName(allStripData.effects[index].name)
        if (allStripData.effects[index].editable > 0) {
            val thisEffectData = allStripData.effects[index].data
            if (thisEffectData.has("heat")) {
                setStripParamVal(1, "Heat :", thisEffectData.get("heat").asInt, 0, 140)
            }
            if (thisEffectData.has("delay")) {
                setStripParamVal(2, "Delay :", thisEffectData.get("delay").asInt, 20, 70)
            }
            showStripConfirmButton()
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
        hideStripEffectInterface()
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
                setStripParamVal(1, "Size :", thisEffectData.get("size").asInt, 3, 10)
            }
            if (thisEffectData.has("delay")) {
                setStripParamVal(2, "Delay :", thisEffectData.get("delay").asInt, 10, 100)
            }
            if (thisEffectData.has("solid")){
                setStripParamBool(1,"Rainbow :",thisEffectData.get("solid").asInt)
            }

            showStripConfirmButton()
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

        sentencePopup.setOnMenuItemClickListener { it->
            when (it.itemId){
                R.id.sentenceSet-> {
                    Log.d(TAG,"SET sentence")
                }
                R.id.sentenceNew-> {
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

    private  fun dialogSentenceAction(sentence : jPanelSentence , mode : String){
        val mDialog = Dialog(this)
        Log.d(TAG,"Passed sentence ID : ${sentence.id} and mode $mode")
        Log.d(TAG,"Sentence : ${sentence.sentence} ")
        Log.d(TAG,"Font color : ${sentence.fontColor.r} , ${sentence.fontColor.g} , ${sentence.fontColor.b}" +
                "Font id : ${sentence.fontId} ")
        Log.d(TAG, "Background type ${sentence.bgType} , BG Id : ${sentence.bgId}")

        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mDialog.setContentView(R.layout.ledp_dialog)
        mDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //magic should be here
        val tvHeader = mDialog.findViewById<View>(R.id.tvLedpHeader) as TextView
        val etSentence = mDialog.findViewById<View>(R.id.etLedpSentence) as EditText
        val btnColor = mDialog.findViewById<View>(R.id.btnLedpFontColor) as Button
        val tvColor = mDialog.findViewById<View>(R.id.tvLedpFontColor) as TextView
        val spFont = mDialog.findViewById<View>(R.id.spLedpFonts) as Spinner
        val spEffect = mDialog.findViewById<View>(R.id.spLedpEffects) as Spinner
        val btnConfirm =mDialog.findViewById<View>(R.id.btnLedpConfirm) as Button

        //bind.lvPanelSentences.adapter = SentenceListAdapter(this@MainActivity,sentenceList)
        spFont.adapter = FontListAdapter(this@MainActivity,fontList)
        tvHeader.text = mode
        if (mode == getString(R.string.sentenceHeaderAdd)){
            etSentence.isEnabled = true
            spFont.setVisibility(true)
            btnColor.setVisibility(true)
            tvColor.setVisibility(true)
            spEffect.setVisibility(true)
            etSentence.hint ="Wpisz frazę"
            etSentence.text.clear()

        }else if (mode == getString(R.string.sentenceHeaderEdit)){
            etSentence.isEnabled = true
            spFont.setVisibility(true)
            btnColor.setVisibility(true)
            tvColor.setVisibility(true)
            spEffect.setVisibility(true)
            etSentence.hint =""
            etSentence.text.clear()
            etSentence.text.append(sentence.sentence)

        }else if (mode == getString(R.string.sentenceHeaderDelete)){
            etSentence.isEnabled = false
            spFont.setVisibility(false)
            btnColor.setVisibility(false)
            tvColor.setVisibility(false)
            spEffect.setVisibility(false)
            etSentence.text.clear()
            etSentence.text.append(sentence.sentence)
        }
        spFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                //val testFont = spFont.getItemAtPosition(position) as String
                //Log.d(TAG,"Lede font 0 :$testFont")
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d(TAG,"Lede font NOTHING selected")
            }
        }

        spEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val testEffect = spEffect.getItemAtPosition(position) as String
                Log.d(TAG,"Lede effect 0  :$testEffect")
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d(TAG,"Lede effect NOTHING selected")
            }
        }

        btnColor.setOnClickListener {
            Log.d(TAG,"Lede sentence color button clicked.")
        }
        btnConfirm.setOnClickListener {
            Log.d(TAG,"Lede sentence : ${etSentence.text.toString()}")
            Log.d(TAG,"Lede sentence confirm.")
            mDialog.dismiss()

        }
        //wonderfull .....
        mDialog.setCancelable(true)
        mDialog.show()
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        mDialog.window!!.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        bind = ActivityMainBinding.inflate(layoutInflater)

        stripModeList.addAll(resources.getStringArray(R.array.StripModeList))
        stripPaletteList.addAll(resources.getStringArray(R.array.StripPaletteList))
        stripCustomList.addAll(resources.getStringArray(R.array.TestCustomParameterList))

        panelModeList.addAll(resources.getStringArray(R.array.PanelModeList))

        super.onCreate(savedInstanceState)
        setContentView(bind.root)



        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        AcceptIncommingThread().start() // listen for controlers , trying to connect to app (backward flow)
        myHandler = Handler()    //handle data from  threds : AcceptIncommingThread() ,
        dataHandler = BtHandler()


        //data from StartActivity
        myDevices = intent.getParcelableArrayListExtra<BluetoothDevice>("START_DEVICE_LIST") as ArrayList<BluetoothDevice>
        Log.i("ESP_DEVICE_LIST_SIZE", "${myDevices.size}")
        startPos = intent.getIntExtra("START_CURRENT_SELECTED",0)
        for (d in myDevices){
            val adr = d.address
            val name = d.name
            Log.i("ESP_MAIN","$adr -> $name")
        }
        piConnection() //prepare interface connection
        //piMain() //prepare interface main
        hideMainInterface()
        hideStripEffectInterface()

        piPanelMain()
        //--------------------------connection panel------------------------------------------------
        bind.spDevices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,  view: View, position: Int, id: Long) {
                //Toast.makeText(this@MainActivity, "DEVICE : " + parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        //----btn connect
        bind.btnConnect.setOnClickListener {
            val selectedNum = bind.spDevices.selectedItemPosition
            //mySelectedBluetoothDevice = bluetoothAdapter.getRemoteDevice("AC:67:B2:2C:D2:B2")
            mySelectedBluetoothDevice = myDevices[selectedNum]
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
            }
            //           Toast.makeText(this, "Fake Conneting to device", Toast.LENGTH_SHORT).show()
        }
        //------------------------main  strip settings----------------------------------------------
        //-----mode
        //val adapterStripMode = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,modeList)
        bind.spStripMode.adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,stripModeList)
        bind.spStripMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                //Toast.makeText(this@MainActivity, "M: " + parent.getItemAtPosition(position) + " : " + position, Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        //----effects
        //Uwaga ten adapter jest z zasobow , nowy ustawiany jest w uiMain()
        //val adapterEffects = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,effectList)
        //bind.spEffect.adapter = adapterEffects
        bind.spStripEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                allStripData.config.selected = position
                when (parent.getItemAtPosition(position)) {
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
                    else -> hideStripEffectInterface()
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
            override fun onStartTrackingTouch(sb: SeekBar?) {
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {

//                Toast.makeText(this@MainActivity, "Time seek bar last val : ${sb?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        //----main color pick
        bind.btnStripColorMain.setOnClickListener {
            ColorPickerDialog
                .Builder(this)        				// Pass Activity Instance
                .setTitle("Wybierz kolor")           	// Default "Choose Color"
                .setColorShape(ColorShape.CIRCLE)   // Default ColorShape.CIRCLE
                .setDefaultColor("#ff0000")     // Pass Default Color
                .setColorListener { _, colorHex ->
                    val thisColorInt = Color.parseColor(colorHex)
                    bind.tvStripColorMain.setBackgroundColor(thisColorInt)
//                  Toast.makeText(this, "Main Color HEX:"+colorHex+"  ", Toast.LENGTH_SHORT).show()
                }.show()
        }
        //----confirm
        bind.btnStripMainConfirm.setOnClickListener {
            val updatedConfig = JsonObject()
            val updatedColor = JsonObject()
            allStripData.config.mode = bind.spStripMode.selectedItemPosition
            allStripData.config.selected = bind.spStripEffect.selectedItemPosition
            allStripData.config.time = bind.sbStripTime.progress
            //now color part...
            val colDraw = bind.tvStripColorMain.background as ColorDrawable
            val colInt = colDraw.color
            allStripData.config.color.r = Color.red(colInt)
            allStripData.config.color.g = Color.green(colInt)
            allStripData.config.color.b = Color.blue(colInt)
            //...
            updatedConfig.addProperty("cmd","UPDATE_CONFIG")
            updatedConfig.addProperty("mode", allStripData.config.mode)
            updatedConfig.addProperty("selected", allStripData.config.selected)
            updatedColor.addProperty("r", allStripData.config.color.r)
            updatedColor.addProperty("g", allStripData.config.color.g)
            updatedColor.addProperty("b", allStripData.config.color.b)
            updatedConfig.add("color",updatedColor)
            updatedConfig.addProperty("time", allStripData.config.time)
            ConnectThread(mySelectedBluetoothDevice).writeMessage(updatedConfig.toString())

            //prepareUpdatedConfig()// NOWA WERSJA Do poprawienia
//            Toast.makeText(this, updatedConfig.toString(), Toast.LENGTH_LONG).show()
        }
        //------------------------Strip effect settings---------------------------------------------
        //----pick color 1
        bind.btnStripColor1.setOnClickListener {
            ColorPickerDialog
                .Builder(this)        				// Pass Activity Instance
                .setTitle("Wybierz kolor")           	// Default "Choose Color"
                .setColorShape(ColorShape.CIRCLE)   // Default ColorShape.CIRCLE
                .setDefaultColor("#ff0000")     // Pass Default Color
                .setColorListener { _, colorHex ->
                    //Toast.makeText(this, "Test1 Color"+colorHex+" ", Toast.LENGTH_SHORT).show()
                    bind.edStripColor1.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()
        }
        //----pick color 2
        bind.btnStripColor2.setOnClickListener {
            ColorPickerDialog
                .Builder(this)        				// Pass Activity Instance
                .setTitle("Wybierz kolor")           	// Default "Choose Color"
                .setColorShape(ColorShape.CIRCLE)   // Default ColorShape.CIRCLE
                .setDefaultColor("#ff0000")     // Pass Default Color
                .setColorListener { _, colorHex ->
                    //Toast.makeText(this, "Test1 Color"+colorHex+" ", Toast.LENGTH_SHORT).show()
                    bind.edStripColor2.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()
        }
        //----palette pick
        val adapterPalette = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,stripPaletteList)
        bind.spStripPalette.adapter = adapterPalette
        bind.spStripPalette.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                //Toast.makeText(this@MainActivity, "P: " + parent.getItemAtPosition(position) + " : " + position, Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        //----custom pick
        val adapterCustom = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,stripCustomList)
        //adapterCustom.also { bind.spCustom.adapter = it } // ?!?!!?!!?!?!??!?!?!!? JA JEBIE
        bind.spStripCustom.adapter = adapterCustom
        bind.spStripCustom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                //               Toast.makeText(this@MainActivity, "C: " + parent.getItemAtPosition(position) + " : " + position, Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        //----param 1
        bind.sbStripParam1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbStripParam1Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
//                Toast.makeText(this@MainActivity, "P1: ${sb?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        //----param 2
        bind.sbStripParam2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbStripParam2Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                //               Toast.makeText(this@MainActivity, "P2: ${sb?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        //----param 3
        bind.sbStripParam3.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbStripParam3Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
//                Toast.makeText(this@MainActivity, "P3: ${sb?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        //----param 4
        bind.sbStripParam4.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbStripParam4Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
//                Toast.makeText(this@MainActivity, "P4: ${sb?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        //----bool 1
        bind.swStripBool1.setOnCheckedChangeListener { _, b ->
//            Toast.makeText(this, "B1:"+b.toString(), Toast.LENGTH_SHORT).show()
        }
        //----bool 2
        bind.swStripBool2.setOnCheckedChangeListener { _, b ->
//            Toast.makeText(this, "B2:"+b.toString(), Toast.LENGTH_SHORT).show()
        }
        //----button confirm effect
        bind.btnStripEffectConfirm.setOnClickListener {
            when (bind.spStripEffect.selectedItem as String) {
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
                "Running col    or dots" -> upRunningColorDots()
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
            prepareStripUpdatedData()
            ConnectThread(mySelectedBluetoothDevice).writeMessage(prepareStripUpdatedData())
        }
        //------------------------Panel main settings-----------------------------------------------
        //-----mode
        bind.spPanelMode.adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,panelModeList)
        bind.spPanelMode.setSelection(0)
        bind.spPanelMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                //Toast.makeText(this@MainActivity, "M: " + parent.getItemAtPosition(position) + " : " + position, Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        //----Panel sentence list
        bind.lvPanelSentences.isClickable = true
        bind.lvPanelSentences.adapter = SentenceListAdapter(this@MainActivity,sentenceList)
        bind.lvPanelSentences.setOnItemLongClickListener { adapterView, view, position, id ->
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