package com.example.bdlled_02


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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.bdlled_02.databinding.ActivityMainBinding
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.github.dhaval2404.colorpicker.util.setVisibility
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException
import java.io.InputStream
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

var gAllData = Gson().fromJson(jsonData,jAllData::class.java)


class MainActivity : AppCompatActivity(){
    private lateinit var bind : ActivityMainBinding
    var myDevices : ArrayList<BluetoothDevice> = ArrayList() //list form start activity
    var startPos : Int = 0 // position in list , current sellected device
    var espState : EspConnectionState = EspConnectionState.DISCONNECTED
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
                            jsonData = allMessage.substring(0, allMessage.length - 1)
                            gAllData = Gson().fromJson(jsonData ,jAllData::class.java)
                            allMessage = ""
                            Log.d("DEBUG_INSIDE","Data loaded system alive")
                            piMain()
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
                hideEffectInterface()
                bind.btnConnect.isEnabled = true
                bind.spDevices.isEnabled = true
                bind.btnConnect.text = getString(R.string.iConnect)
            }

            EspConnectionState.CONNECTING ->{
                hideMainInterface()
                hideEffectInterface()
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

    private fun piMain(){
        var effectNames = arrayOf<String>()
        for (e in gAllData.effects.indices){
            effectNames += gAllData.effects[e].name
        }
        val adapterEffectNames = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,effectNames)

        with(
            bind,
            {
                //visibility
                lbMode.setVisibility(true)
                spMode.setVisibility(true)
                lbEffect.setVisibility(true)
                spEffect.setVisibility(true)
                lbTime.setVisibility(true)
                lbTimeVal.setVisibility(true)
                sbTime.setVisibility(true)
                sbTime.min = 20
                sbTime.max = 120
                btnColorMain.setVisibility(true)
                tvColorMain.setVisibility(true)
                btnMainConfirm.setVisibility(true)
                panelMainSettings.setVisibility(true)
                //parameters
                spMode.setSelection(gAllData.config.mode, false)
                if (effectNames.size > 0) {
                    spEffect.adapter = adapterEffectNames
                    spEffect.setSelection(gAllData.config.selected)
                }else{
                    lbEffect.setVisibility(false)
                    spEffect.setVisibility(false)
                }
                sbTime.setProgress(gAllData.config.time, false)
                lbTimeVal.text = gAllData.config.time.toString()
                tvColorMain.setBackgroundColor(
                    Color.rgb(
                        gAllData.config.color.r,
                        gAllData.config.color.g,
                        gAllData.config.color.b,
                    ),
                )
            },
        )

    }

    private fun hideMainInterface(){
        bind.panelMainSettings.setVisibility(false)
    }


    //All set "Metods" simply turn on visibility and set params
    //This methods do only UI stuff, DONT set any datas
    /*
    Clear Seek Bar parameter , little help only for : clearAndHideEffectInterface()
    */
    private fun clearParamVal(p : SeekBar){
        p.min = 0
        p.max = 255
        p.setProgress(0,false)
    }
    private fun hideEffectInterface(){
        bind.tvEffectName.text = resources.getString(R.string.tvEffectName)
        bind.edColor1.setBackgroundColor(Color.parseColor("#000000"))
        bind.edColor2.setBackgroundColor(Color.parseColor("#000000"))
        bind.lbPalette.text = resources.getString(R.string.lbPalette)
        bind.spPalette.setSelection(0)
        //TAKIE TROCHE NA PALE
        bind.lbCustom.text = resources.getString(R.string.lbCustom)
/*
        //zaskakujące, ale To w chwili onCreate jest nullem
        bind.spCustom.adapter = ArrayAdapter(this,
                                                android.R.layout.simple_spinner_dropdown_item,
                                                resources.getStringArray(R.array.TestCustomParameterList))
 */
        bind.spCustom.setSelection(0)
        bind.lbParam1.text = resources.getString(R.string.lbParam1)
        clearParamVal(bind.sbParam1)
        bind.lbParam2.text = resources.getString(R.string.lbParam2)
        clearParamVal(bind.sbParam2)
        bind.lbParam3.text = resources.getString(R.string.lbParam3)
        clearParamVal(bind.sbParam3)
        bind.lbParam4.text = resources.getString(R.string.lbParam4)
        clearParamVal(bind.sbParam4)

        bind.lbBool1.text = resources.getString(R.string.lbBool1)
        bind.swBool1.isChecked = false

        bind.lbBool2.text = resources.getString(R.string.lbBool2)
        bind.swBool2.isChecked = false

/*
        //tutaj pewnie dodatkowo ukrywane są wiersze tabeli , problem się pojawia z
        //dostępem do nich , zatem rozwiązanie na pałe , zwyczajnie ręcznie powyłączać elementy
        for (i in bind.panelEffect.children ){
            i.setVisibility(false)
        }
*/
        //wersja na pałe
        bind.tvEffectName.setVisibility(false)
        bind.btnColor1.setVisibility(false)
        bind.edColor1.setVisibility(false)
        bind.btnColor2.setVisibility(false)
        bind.edColor2.setVisibility(false)
        bind.lbPalette.setVisibility(false)
        bind.spPalette.setVisibility(false)
        bind.lbCustom.setVisibility(false)
        bind.spCustom.setVisibility(false)

        bind.lbParam1.setVisibility(false)
        bind.lbParam1Val.setVisibility(false)
        bind.sbParam1.setVisibility(false)

        bind.lbParam2.setVisibility(false)
        bind.lbParam2Val.setVisibility(false)
        bind.sbParam2.setVisibility(false)

        bind.lbParam3.setVisibility(false)
        bind.lbParam3Val.setVisibility(false)
        bind.sbParam3.setVisibility(false)

        bind.lbParam4.setVisibility(false)
        bind.lbParam4Val.setVisibility(false)
        bind.sbParam4.setVisibility(false)

        bind.lbBool1.setVisibility(false)
        bind.swBool1.setVisibility(false)

        bind.lbBool2.setVisibility(false)
        bind.swBool2.setVisibility(false)

        bind.btnEffectConfirm.setVisibility(false)
    }
    private fun showConfirmButton(){
        bind.btnEffectConfirm.setVisibility(true)
    }
    private fun setEffectName( name : String ){
        bind.panelEffect.setVisibility(visible = true)
        bind.tvEffectName.setVisibility(visible = true)
        bind.tvEffectName.text = name
    }
    private fun setParamColor(pColorNum : Int ,  r :Int , g : Int ,b : Int){
        when (pColorNum){
            1 -> {
                bind.btnColor1.setVisibility(true)
                bind.edColor1.setVisibility(true)
                bind.edColor1.setBackgroundColor(Color.rgb(r,g,b))
            }
            2 -> {
                bind.btnColor2.setVisibility(true)
                bind.edColor2.setVisibility(true)
                bind.edColor2.setBackgroundColor(Color.rgb(r,g,b))
            }
        }
    }
    private fun setPalette( index : Int){
        bind.lbPalette.setVisibility(true)
        bind.spPalette.setVisibility(true)
        if (index > bind.spPalette.count - 1) bind.spPalette.setSelection(0,false)
        else bind.spPalette.setSelection(index,false)
        //   Toast.makeText(this,"Count P:" +bind.spPalette.count,Toast.LENGTH_SHORT).show()
    }
    private fun setCustom(desc : String , elem : Array<String>, index : Int){
        if (elem.isNotEmpty()){
            bind.lbCustom.setVisibility(true)
            bind.lbCustom.text = desc
            bind.spCustom.setVisibility(true)
            val aC = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,elem)
            bind.spCustom.adapter = aC
            if (index > elem.size -1) bind.spCustom.setSelection(0)
            else bind.spCustom.setSelection(index)
        }
    }
    private fun setParamVal(pNum : Int , desc : String , pVal : Int , pMin : Int , pMax : Int){
        when (pNum){
            1 -> {
                bind.lbParam1.setVisibility(true)
                bind.lbParam1Val.setVisibility(true)
                bind.sbParam1.setVisibility(true)
                bind.lbParam1.text = desc
                bind.sbParam1.min = pMin
                bind.sbParam1.max = pMax
                bind.sbParam1.progress = pVal
            }
            2->{
                bind.lbParam2.setVisibility(true)
                bind.lbParam2Val.setVisibility(true)
                bind.sbParam2.setVisibility(true)
                bind.lbParam2.text = desc
                bind.sbParam2.min = pMin
                bind.sbParam2.max = pMax
                bind.sbParam2.progress = pVal
            }
            3->{
                bind.lbParam3.setVisibility(true)
                bind.lbParam3Val.setVisibility(true)
                bind.sbParam3.setVisibility(true)
                bind.lbParam3.text = desc
                bind.sbParam3.min = pMin
                bind.sbParam3.max = pMax
                bind.sbParam3.progress = pVal
            }
            4->{
                bind.lbParam4.setVisibility(true)
                bind.lbParam4Val.setVisibility(true)
                bind.sbParam4.setVisibility(true)
                bind.lbParam4.text = desc
                bind.sbParam4.min = pMin
                bind.sbParam4.max = pMax
                bind.sbParam4.progress = pVal
            }
        }
    }
    /*  Obejscie problemu , dla Kotlina bool to true/false , dla ESP32 jeden ch....
        czy true/false czy 1/0 , domyslnie zwraca jednak 1/0
        aby zachowac zgodność bool bedzie intem, mniej roboty i przerobek
        Podobnie w updateParamBool
     */
    private fun setParamBool(pNum : Int , desc : String ,state : Int){
        var tmpBool : Boolean = false
        if (state == 1) tmpBool = true
        when (pNum){
            1-> {
                bind.lbBool1.setVisibility(true)
                bind.lbBool1.text = desc
                bind.swBool1.setVisibility(true)
                bind.swBool1.isChecked = tmpBool
            }
            2-> {
                bind.lbBool2.setVisibility(true)
                bind.lbBool2.text = desc
                bind.swBool2.setVisibility(true)
                bind.swBool2.isChecked = tmpBool
            }
        }
    }

    //THIS metods are triggered by uiXXXX methods , update data structures
    private fun updateParamCol(parmName: String , tvCol : TextView){
        val thisEffect = gAllData.effects[bind.spEffect.selectedItemPosition]
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
    private fun updatePalette(parmName : String){
        val thisEffect = gAllData.effects[bind.spEffect.selectedItemPosition]
        if(thisEffect.data.has(parmName)){
            thisEffect.data.addProperty(parmName,bind.spPalette.selectedItemPosition)
        } else{
            Toast.makeText(this, "Effect dont have $parmName parameter", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateCustom(parmName : String){
        val thisCustom = gAllData.effects[bind.spEffect.selectedItemPosition]
        if (thisCustom.data.has(parmName)){
            thisCustom.data.addProperty(parmName,bind.spCustom.selectedItemPosition)
        }else{
            Toast.makeText(this, "Effect dont have $parmName parameter", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateParamVal(parmName : String , sb :SeekBar ){
        val thisEffect = gAllData.effects[bind.spEffect.selectedItemPosition]
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
    private fun updatParamBool(parmName: String, sw : SwitchCompat){
        val thisEffect = gAllData.effects[bind.spEffect.selectedItemPosition]
        if(thisEffect.data.has(parmName)){
            var tmpBoolAsInt = 0
            if (sw.isChecked == true ) tmpBoolAsInt = 1
            thisEffect.data.addProperty(parmName, tmpBoolAsInt)
        } else{
            Toast.makeText(this, "Effect dont have $parmName parameter", Toast.LENGTH_SHORT).show()
        }
    }

    // This methods updates data structures in APP and send to ESP modified effect
    private fun prepareUpdatedData() : String {
        val thisEffect = gAllData.effects[bind.spEffect.selectedItemPosition]
        val toSendEffect = JsonObject()
        toSendEffect.addProperty("cmd","UPDATE_EFFECT")
        toSendEffect.addProperty("name", thisEffect.name)
        toSendEffect.addProperty("editable" , thisEffect.editable)
        toSendEffect.add("data",thisEffect.data)
        return toSendEffect.toString()
    }

    //UWAGA !!! w buttonie funkcjonuje nadal stara wersja z pierwszych testow
    private fun prepareUpdatedConfig() : String {
        val toSendEffect = JsonObject()
        toSendEffect.addProperty("cmd","UPDATE_CONFIG")
        toSendEffect.addProperty("mode",bind.spMode.selectedItemPosition) //przerobic parm custom by bylo zgodnie
        toSendEffect.addProperty("selected",bind.spEffect.selectedItemPosition)
        updateParamCol("color",bind.tvColorMain)
        updateParamVal("time",bind.sbTime)
        bind.lbTest.text =  toSendEffect.toString()
        return toSendEffect.toString()
    }

    // "Beat wave" parm1 , parm2 , parm3 , parm4
    private fun piBeatWave(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("pulse1")) {
                setParamVal(1, "Pulse 1:", thisEffectData.get("pulse1").asInt, 1, 30)
            }
            if (thisEffectData.has("pulse2")) {
                setParamVal(2, "Pulse 2:", thisEffectData.get("pulse2").asInt, 1, 30)
            }
            if (thisEffectData.has("pulse3")) {
                setParamVal(3, "Pulse 3:", thisEffectData.get("pulse3").asInt, 1, 30)
            }
            if (thisEffectData.has("pulse4")) {
                setParamVal(4, "Pulse 4:", thisEffectData.get("pulse4").asInt, 1, 30)
            }
            showConfirmButton()
        }else{
            Toast.makeText(this,"This "+gAllData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upBeatWave(){
        updateParamVal("pulse1",bind.sbParam1)
        updateParamVal("pulse2",bind.sbParam2)
        updateParamVal("pulse3",bind.sbParam3)
        updateParamVal("pulse4",bind.sbParam4)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Blend wave" parm1 , parm2 , parm3
    private fun piBlendWave(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("speed")) {
                setParamVal(1, "Speed:", thisEffectData.get("speed").asInt, 1, 12)
            }
            if (thisEffectData.has("mH1")) {
                setParamVal(2, "Step 1:", thisEffectData.get("mH1").asInt, 1, 24)
            }
            if (thisEffectData.has("mH2")) {
                setParamVal(3, "Step 2:", thisEffectData.get("mH2").asInt, 1, 24)
            }
            showConfirmButton()
        }else{
            Toast.makeText(this,"This "+gAllData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upBlendWave() {
        updateParamVal("speed", bind.sbParam1)
        updateParamVal("mH1", bind.sbParam2)
        updateParamVal("mH2", bind.sbParam3)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Blur" parm1 , parm2 , parm3 , parm4
    private fun piBlur(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("speed")) {
                setParamVal(1, "Speed:", thisEffectData.get("speed").asInt, 1, 10)
            }
            if (thisEffectData.has("o1")) {
                setParamVal(2, "Offset 1:", thisEffectData.get("o1").asInt, 1, 20)
            }
            if (thisEffectData.has("o2")) {
                setParamVal(3, "Offset 2:", thisEffectData.get("o2").asInt, 1, 20)
            }
            if (thisEffectData.has("o3")) {
                setParamVal(4, "Offset 3:", thisEffectData.get("o3").asInt, 1, 20)
            }
            showConfirmButton()
        }else{
            Toast.makeText(this,"This "+gAllData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upBlur(){
        updateParamVal("speed",bind.sbParam1)
        updateParamVal("o1",bind.sbParam2)
        updateParamVal("o2",bind.sbParam3)
        updateParamVal("o3",bind.sbParam4)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Confeti" palette , parm1 , parm2
    private fun piConfeti(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("fade")) {
                setParamVal(1, "Fade :", thisEffectData.get("fade").asInt, 1, 16)
            }
            if (thisEffectData.has("mDiff")) {
                setParamVal(2, "Ofset 1 :", thisEffectData.get("mDiff").asInt, 1, 15)
            }
            showConfirmButton()
        }
    }
    private  fun upConfeti(){
        updatePalette("pIndex")
        updateParamVal("fade",bind.sbParam1)
        updateParamVal("mDiff",bind.sbParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Sinelon" , parm1 , parm2
    private fun piSinelon(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("bpm")) {
                setParamVal(1, "Bpm :", thisEffectData.get("bpm").asInt, 5, 32)
            }
            if (thisEffectData.has("fade")) {
                setParamVal(2, "Fade:", thisEffectData.get("fade").asInt, 5, 25)
            }
            showConfirmButton()
        }
    }
    private fun upSinelon() {
        updateParamVal("bpm",bind.sbParam1)
        updateParamVal("fade",bind.sbParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Bpm" palette , parm1
    private fun piBpm(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("bpm")) {
                setParamVal(1, "Speed :", thisEffectData.get("bpm").asInt, 10, 120)
            }
            showConfirmButton()
        }
    }
    private fun upBpm(){
        updatePalette("pIndex")
        updateParamVal("bpm",bind.sbParam1)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Juggle" parm1 , parm2
    private fun piJuggle(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("stepHue")) {
                setParamVal(1, "Step :", thisEffectData.get("stepHue").asInt, 1, 8)
            }
            if (thisEffectData.has("fade")) {
                setParamVal(2, "Fade :", thisEffectData.get("fade").asInt, 5, 32)
            }
            showConfirmButton()
        }
    }
    private fun upJuggle() {
        updateParamVal("stepHue",bind.sbParam1)
        updateParamVal("fade",bind.sbParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Dot beat" color1 ,color2 , parm1 , parm2
    private fun piDotBeat(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            var col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color1")){
                col = thisEffectData.get("color1").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("color2")){
                col = thisEffectData.get("color2").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt }
                if (col.has("g")) { g = col.get("g").asInt }
                if (col.has("b")) { b = col.get("b").asInt }
                setParamColor(2 , r , g , b)
            }
            if (thisEffectData.has("bpm")) {
                setParamVal(1, "Bpm :", thisEffectData.get("bpm").asInt, 5, 32)
            }
            if (thisEffectData.has("fadeMod")) {
                setParamVal(2, "Fade:", thisEffectData.get("fadeMod").asInt, 5, 25)
            }
            showConfirmButton()
        }
    }
    private fun upDotBeat(){
        updateParamCol("color1",bind.edColor1)
        updateParamCol("color2",bind.edColor2)
        updateParamVal("bpm",bind.sbParam1)
        updateParamVal("fadeMod",bind.sbParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Easing" color1 , parm1
    private fun piEasing(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            val col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color")){
                col = thisEffectData.get("color").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("multiplier")) {
                setParamVal(1, "Fade :", thisEffectData.get("multiplier").asInt, 1, 8)
            }
            showConfirmButton()
        }
    }
    private fun upEasing(){
        updateParamCol("color",bind.edColor1)
        updateParamVal("multiplier",bind.sbParam1)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Hyper dot" color1 ,  parm1 , parm2 , parm3
    private fun piHyperDot(){ //color 1 parm 1 parm2 parm 3
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            val col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color")){
                col = thisEffectData.get("color").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("bpm")) {
                setParamVal(1, "Fade :", thisEffectData.get("bpm").asInt, 1, 20)
            }
            if (thisEffectData.has("low")) {
                setParamVal(2, "Low :", thisEffectData.get("low").asInt, 5, 50)
            }
            if (thisEffectData.has("high")) {
                setParamVal(3, "High :", thisEffectData.get("high").asInt, 100, 200)
            }
            showConfirmButton()
        }
    }
    private fun upHyperDot(){
        updateParamCol("color",bind.edColor1)
        updateParamVal("bpm",bind.sbParam1)
        updateParamVal("low",bind.sbParam2)
        updateParamVal("high",bind.sbParam3)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Beat sin gradient" parm1 , parm2
    private fun piBeatSinGradient(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("start")) {
                setParamVal(1, "Speed1 :", thisEffectData.get("start").asInt, 1, 16)
            }
            if (thisEffectData.has("end")) {
                setParamVal(2, "Speed2 :", thisEffectData.get("end").asInt, 1, 16)
            }
            showConfirmButton()
        }
    }
    private fun upBeatSinGradient() {
        updateParamVal("start",bind.sbParam1)
        updateParamVal("end",bind.sbParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fire 1" parm1 , parm2
    private fun piFire1(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("cooling")) {
                setParamVal(1, "Cooling :", thisEffectData.get("cooling").asInt, 20, 100)
            }
            if (thisEffectData.has("sparking")) {
                setParamVal(2, "Sparking:", thisEffectData.get("sparking").asInt, 50, 200)
            }
            showConfirmButton()
        }
    }
    private fun upFire1() {
        updateParamVal("cooling",bind.sbParam1)
        updateParamVal("sparking",bind.sbParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fire 1 two flames" parm1 , parm2
    private fun piFire1TwoFlames(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("cooling")) {
                setParamVal(1, "Cooling :", thisEffectData.get("cooling").asInt, 20, 100)
            }
            if (thisEffectData.has("sparking")) {
                setParamVal(2, "Sparking:", thisEffectData.get("sparking").asInt, 50, 200)
            }
            showConfirmButton()
        }
    }
    private fun upFire1TwoFlames() {
        updateParamVal("cooling",bind.sbParam1)
        updateParamVal("sparking",bind.sbParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Worm" param1 , param2
    private fun piWorm(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("adjust")) {
                setParamVal(1, "Adjust :", thisEffectData.get("adjust").asInt, 1, 24)
            }
            if (thisEffectData.has("nextBlend")) {
                setParamVal(2, "Next blend :", thisEffectData.get("nextBlend").asInt, 5, 30)
            }
            showConfirmButton()
        }
    }
    private fun upWorm() {
        updateParamVal("adjust",bind.sbParam1)
        updateParamVal("nextBlend",bind.sbParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fire 2" custom , parm1 , parm2
    private fun piFire2(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data

            if (thisEffectData.has("dir")) {
                val customParams = arrayOf(
                    "right to left",
                    "left to right",
                    "both sites"
                )
                setCustom("Direction :", customParams,thisEffectData.get("dir").asInt)
            }
            if (thisEffectData.has("intensity")) {
                setParamVal(1, "Intensity :", thisEffectData.get("intensity").asInt, 1, 10)
            }
            if (thisEffectData.has("speed")) {
                setParamVal(2, "Speed :", thisEffectData.get("speed").asInt, 1, 20)
            }

            showConfirmButton()
        }else{
            Toast.makeText(this,"This "+gAllData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upFire2(){
        updateCustom("dir") //data gets form spCustom
        updateParamVal("intensity",bind.sbParam1)
        updateParamVal("speed",bind.sbParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Noise 1" , palette , parm1 , parm2
    private fun piNoise1(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("low")) {
                setParamVal(1, "Low :", thisEffectData.get("low").asInt, 1, 10)
            }
            if (thisEffectData.has("high")) {
                setParamVal(2, "High :", thisEffectData.get("high").asInt, 11, 20)
            }
            showConfirmButton()
        }
    }
    private fun upNoise1(){
        updatePalette("pIndex")
        updateParamVal("low",bind.sbParam1)
        updateParamVal("high",bind.sbParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Juggle 2" parm1 , parm2 , parm3
    private fun piJuggle2(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("dots")) {
                setParamVal(1, "Dots :", thisEffectData.get("dots").asInt, 1, 10)
            }
            if (thisEffectData.has("beat")) {
                setParamVal(2, "Beat :", thisEffectData.get("beat").asInt, 1, 20)
            }
            if (thisEffectData.has("fade")) {
                setParamVal(3, "Fade :", thisEffectData.get("fade").asInt, 1, 10)
            }
            showConfirmButton()
        }else{
            Toast.makeText(this,"This "+gAllData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upJuggle2() {
        updateParamVal("dots", bind.sbParam1)
        updateParamVal("beat", bind.sbParam2)
        updateParamVal("fade", bind.sbParam3)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Running color dots"  palette , custom
    private fun piRunningColorDots(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("dir")) {
                val customParams = arrayOf(
                    "left to right",
                    "right to left"
                )
                setCustom("Direction :", customParams,thisEffectData.get("dir").asInt)
            }
            showConfirmButton()
        }
    }
    private fun upRunningColorDots(){
        updatePalette("pIndex")
        updateCustom("dir") //data gets form spCustom
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Disco 1" palette , parm1
    private fun piDisco1(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("flash")) {
                setParamVal(1, "Flash :", thisEffectData.get("flash").asInt, 1, 10)
            }

            showConfirmButton()
        }
    }
    private fun upDisco1(){
        updatePalette("pIndex")
        updateParamVal("flash",bind.sbParam1)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Running color dots 2" palette , color1 , param1 , param2
    private fun piRunningColorDots2(){ //color 1 parm 1 bool1
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            val col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("pIndex")){
                setPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("bgColor")){
                col = thisEffectData.get("bgColor").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("bgBright")) {
                setParamVal(1, "Fade :", thisEffectData.get("bgBright").asInt, 0, 50)
            }
            if (thisEffectData.has("bgStatic")){
                setParamBool(1,"Static :",thisEffectData.get("bgStatic").asInt)
            }
            showConfirmButton()
        }
    }
    private fun upRunningColorDots2(){
        updatePalette("pIndex")
        updateParamCol("bgColor",bind.edColor1)
        updateParamVal("bgBright",bind.sbParam1)
        updatParamBool("bgStatic",bind.swBool1)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Disco dots" parm1
    private fun piDiscoDots(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("phaseTime")) {
                setParamVal(1, "Next phase(sec) :", thisEffectData.get("phaseTime").asInt, 5, 30)
            }
            showConfirmButton()
        }
    }
    private fun upDiscoDots() {
        updateParamVal("phaseTime",bind.sbParam1)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    // "Plasma" palette , parm1 , parm2 , parm 3
    private fun piPlasma(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("pIndex")){
                setPalette(thisEffectData.get("pIndex").asInt)
            }
            if (thisEffectData.has("low")) {
                setParamVal(1, "Low :", thisEffectData.get("low").asInt, -128, -32)
            }
            if (thisEffectData.has("high")) {
                setParamVal(2, "High :", thisEffectData.get("high").asInt, 32, 128)
            }
            if (thisEffectData.has("calcMod")) {
                setParamVal(3, "Speed :", thisEffectData.get("calcMod").asInt, 1, 5)
            }
            showConfirmButton()
        }
    }
    private fun upPlasma(){
        updatePalette("pIndex")
        updateParamVal("low",bind.sbParam1)
        updateParamVal("high",bind.sbParam2)
        updateParamVal("calcMod",bind.sbParam3)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Rainbow sine" , parm1 , parm2
    private fun piRainbowSine(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("speed")) {
                setParamVal(1, "Speed :", thisEffectData.get("speed").asInt, 8, 48)
            }
            if (thisEffectData.has("hueStep")) {
                setParamVal(2, "Step :", thisEffectData.get("hueStep").asInt, 2, 16)
            }
            showConfirmButton()
        }
    }
    private fun upRainbowSine() {
        updateParamVal("speed",bind.sbParam1)
        updateParamVal("hueStep",bind.sbParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fast rainbow" , parm1 , parm2
    private fun piFastRainbow(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("speed")) {
                setParamVal(1, "Speed :", thisEffectData.get("speed").asInt, 1, 10)
            }
            if (thisEffectData.has("delta")) {
                setParamVal(2, "Delta :", thisEffectData.get("delta").asInt, 1, 10)
            }
            showConfirmButton()
        }
    }
    private fun upFastRainbow() {
        updateParamVal("speed",bind.sbParam1)
        updateParamVal("delta",bind.sbParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Pulse rainbow" , custom ,parm1 , parm2 , parm3
    private fun piPulseRainbow(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data

            if (thisEffectData.has("dir")) {
                val customParams = arrayOf(
                    "forward",
                    "backward",
                )
                setCustom("Direction :", customParams,thisEffectData.get("dir").asInt)
            }
            if (thisEffectData.has("rot")) {
                setParamVal(1, "Rotation :", thisEffectData.get("rot").asInt, 1, 5)
            }
            if (thisEffectData.has("hue")) {
                setParamVal(2, "Hue:", thisEffectData.get("hue").asInt, 1, 20)
            }
            if (thisEffectData.has("delay")) {
                setParamVal(3, "Delay :", thisEffectData.get("delay").asInt, 10, 50)
            }
            showConfirmButton()
        }else{
            Toast.makeText(this,"This "+gAllData.effects[index].name+ "is not editable",Toast.LENGTH_SHORT).show()
        }
    }
    private fun upPulseRainbow(){
        updateCustom("dir") //data gets form spCustom
        updateParamVal("rot",bind.sbParam1)
        updateParamVal("hue",bind.sbParam2)
        updateParamVal("delay",bind.sbParam3)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fireworks" , parm1 , parm2
    private fun piFireworks(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("size")) {
                setParamVal(1, "Size :", thisEffectData.get("size").asInt, 1, 9)
            }
            if (thisEffectData.has("speed")) {
                setParamVal(2, "Speed :", thisEffectData.get("speed").asInt, 1, 3)
            }
            showConfirmButton()
        }
    }
    private fun upFireworks() {
        updateParamVal("size",bind.sbParam1)
        updateParamVal("speed",bind.sbParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Fireworks 2" , parm1 , parm2
    private fun piFireworks2(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("life")) {
                setParamVal(1, "Life :", thisEffectData.get("life").asInt, 1, 6)
            }
            if (thisEffectData.has("fade")) {
                setParamVal(2, "Fade :", thisEffectData.get("fade").asInt, 1, 6)
            }
            showConfirmButton()
        }
    }
    private fun upFireworks2(){
        updateParamVal("life",bind.sbParam1)
        updateParamVal("fade",bind.sbParam2)
    }
    //"Sin-neon" , parm1
    private fun piSinNeon(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("phaseTime")) {
                setParamVal(1, "Next phase(sec) :", thisEffectData.get("phaseTime").asInt, 10, 30)
            }
            showConfirmButton()
        }
    }
    private fun upSinNeon(){
        updateParamVal("phaseTime",bind.sbParam1)
    }
    //"Carusel" , parm1 , parm1
    private fun piCarusel(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("phaseTime")) {
                setParamVal(1, "Next phase(sec) :", thisEffectData.get("phaseTime").asInt, 5, 30)
            }
            if (thisEffectData.has("freq")) {
                setParamVal(2, "Frequency :", thisEffectData.get("freq").asInt, 1, 3)
            }
            showConfirmButton()
        }

    }
    private fun upCarusel(){
        updateParamVal("phaseTime",bind.sbParam1)
        updateParamVal("freq",bind.sbParam2)
    }
    //"Color Wipe" , color1 , color2 , parm1 , parm2 , bool
    private fun piColorWipe(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            var col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color1")){
                col = thisEffectData.get("color1").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("color2")){
                col = thisEffectData.get("color2").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt }
                if (col.has("g")) { g = col.get("g").asInt }
                if (col.has("b")) { b = col.get("b").asInt }
                setParamColor(2 , r , g , b)
            }
            if (thisEffectData.has("delay1")) {
                setParamVal(1, "Delay 1:", thisEffectData.get("delay1").asInt, 10, 100)
            }
            if (thisEffectData.has("delay2")) {
                setParamVal(2, "Delay 2:", thisEffectData.get("delay2").asInt, 10, 100)
            }
            if (thisEffectData.has("clear")){
                setParamBool(1,"Clear :",thisEffectData.get("clear").asInt)
            }

            showConfirmButton()
        }

    }
    private fun upColorWipe(){
        updateParamCol("color1",bind.edColor1)
        updateParamCol("color2",bind.edColor2)
        updateParamVal("delay1",bind.sbParam1)
        updateParamVal("delay2",bind.sbParam2)
        updatParamBool("clear",bind.swBool1)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Bounce bar" , color1 , parm1 , parm1
    private fun piBounceBar(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            val col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color")){
                col = thisEffectData.get("color").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("size")) {
                setParamVal(1, "Size :", thisEffectData.get("size").asInt, 3, 10)
            }
            if (thisEffectData.has("delay")) {
                setParamVal(2, "Delay :", thisEffectData.get("delay").asInt, 10, 250)
            }
            showConfirmButton()
        }
    }
    private fun upBounceBar(){
        updateParamCol("color",bind.edColor1)
        updateParamVal("size",bind.sbParam1)
        updateParamVal("delay",bind.sbParam2)
//        Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Chillout" , parm1 , parm2
    private fun piChillout(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            if (thisEffectData.has("heat")) {
                setParamVal(1, "Heat :", thisEffectData.get("heat").asInt, 0, 140)
            }
            if (thisEffectData.has("delay")) {
                setParamVal(2, "Delay :", thisEffectData.get("delay").asInt, 20, 70)
            }
            showConfirmButton()
        }
    }
    private fun upChillout() {
        updateParamVal("heat",bind.sbParam1)
        updateParamVal("delay",bind.sbParam2)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }
    //"Comet", color , parm1 , parm2 , bool
    private fun piComet(){
        val index = bind.spEffect.selectedItemPosition
        hideEffectInterface()
        setEffectName(gAllData.effects[index].name)
        if (gAllData.effects[index].editable > 0) {
            val thisEffectData = gAllData.effects[index].data
            val col: JsonObject
            var r = 0
            var g = 0
            var b = 0
            if (thisEffectData.has("color")){
                col = thisEffectData.get("color").asJsonObject
                if (col.has("r")) { r = col.get("r").asInt  }
                if (col.has("g")) { g = col.get("g").asInt  }
                if (col.has("b")) { b = col.get("b").asInt  }
                setParamColor(1 , r , g , b)
            }
            if (thisEffectData.has("size")) {
                setParamVal(1, "Size :", thisEffectData.get("size").asInt, 3, 10)
            }
            if (thisEffectData.has("delay")) {
                setParamVal(2, "Delay :", thisEffectData.get("delay").asInt, 10, 100)
            }
            if (thisEffectData.has("solid")){
                setParamBool(1,"Rainbow :",thisEffectData.get("solid").asInt)
            }

            showConfirmButton()
        }
    }
    private fun upComet(){
        updateParamCol("color",bind.edColor1)
        updateParamVal("size",bind.sbParam1)
        updateParamVal("delay",bind.sbParam2)
        updatParamBool("solid",bind.swBool1)
        //       Toast.makeText(this, prepareUpdatedData(), Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        bind = ActivityMainBinding.inflate(layoutInflater)

        val modeList = resources.getStringArray(R.array.ModeList)
        val paletteList = resources.getStringArray(R.array.PaletteList)
        val customList = resources.getStringArray(R.array.TestCustomParameterList)//nad tym tez trzeba popracowac, znaczy sie wyjebac
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
        hideEffectInterface()

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
        //------------------------main settings-----------------------------------------------------
        //-----mode
        val adapterMode = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,modeList)
        bind.spMode.adapter = adapterMode
        bind.spMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
        bind.spEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                gAllData.config.selected = position
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
                    else -> hideEffectInterface()
                }
//                Toast.makeText(this@MainActivity, "E: " + parent.getItemAtPosition(position) + " : " + position, Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        //----time
        bind.sbTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbTimeVal.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {

//                Toast.makeText(this@MainActivity, "Time seek bar last val : ${sb?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        //----main color pick
        bind.btnColorMain.setOnClickListener {
            ColorPickerDialog
                .Builder(this)        				// Pass Activity Instance
                .setTitle("Wybierz kolor")           	// Default "Choose Color"
                .setColorShape(ColorShape.CIRCLE)   // Default ColorShape.CIRCLE
                .setDefaultColor("#ff0000")     // Pass Default Color
                .setColorListener { _, colorHex ->
                    val thisColorInt = Color.parseColor(colorHex)
                    bind.tvColorMain.setBackgroundColor(thisColorInt)
//                  Toast.makeText(this, "Main Color HEX:"+colorHex+"  ", Toast.LENGTH_SHORT).show()
                }.show()
        }
        //----confirm
        bind.btnMainConfirm.setOnClickListener {
            val updatedConfig = JsonObject()
            val updatedColor = JsonObject()
            gAllData.config.mode = bind.spMode.selectedItemPosition
            gAllData.config.selected = bind.spEffect.selectedItemPosition
            gAllData.config.time = bind.sbTime.progress
            //now color part...
            val colDraw = bind.tvColorMain.background as ColorDrawable
            val colInt = colDraw.color
            gAllData.config.color.r = Color.red(colInt)
            gAllData.config.color.g = Color.green(colInt)
            gAllData.config.color.b = Color.blue(colInt)
            //...
            updatedConfig.addProperty("cmd","UPDATE_CONFIG")
            updatedConfig.addProperty("mode", gAllData.config.mode)
            updatedConfig.addProperty("selected", gAllData.config.selected)
            updatedColor.addProperty("r", gAllData.config.color.r)
            updatedColor.addProperty("g", gAllData.config.color.g)
            updatedColor.addProperty("b", gAllData.config.color.b)
            updatedConfig.add("color",updatedColor)
            updatedConfig.addProperty("time", gAllData.config.time)
            ConnectThread(mySelectedBluetoothDevice).writeMessage(updatedConfig.toString())

            //prepareUpdatedConfig()// NOWA WERSJA Do poprawienia
//            Toast.makeText(this, updatedConfig.toString(), Toast.LENGTH_LONG).show()
        }
        //------------------------Effect settings----------------------------------------------------
        //----pick color 1
        bind.btnColor1.setOnClickListener {
            ColorPickerDialog
                .Builder(this)        				// Pass Activity Instance
                .setTitle("Wybierz kolor")           	// Default "Choose Color"
                .setColorShape(ColorShape.CIRCLE)   // Default ColorShape.CIRCLE
                .setDefaultColor("#ff0000")     // Pass Default Color
                .setColorListener { _, colorHex ->
                    //Toast.makeText(this, "Test1 Color"+colorHex+" ", Toast.LENGTH_SHORT).show()
                    bind.edColor1.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()
        }
        //----pick color 2
        bind.btnColor2.setOnClickListener {
            ColorPickerDialog
                .Builder(this)        				// Pass Activity Instance
                .setTitle("Wybierz kolor")           	// Default "Choose Color"
                .setColorShape(ColorShape.CIRCLE)   // Default ColorShape.CIRCLE
                .setDefaultColor("#ff0000")     // Pass Default Color
                .setColorListener { _, colorHex ->
                    //Toast.makeText(this, "Test1 Color"+colorHex+" ", Toast.LENGTH_SHORT).show()
                    bind.edColor2.setBackgroundColor(Color.parseColor(colorHex))
                }
                .show()
        }
        //----palette pick
        val adapterPalette = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,paletteList)
        bind.spPalette.adapter = adapterPalette
        bind.spPalette.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                //Toast.makeText(this@MainActivity, "P: " + parent.getItemAtPosition(position) + " : " + position, Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        //----custom pick
        val adapterCustom = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,customList)
        //adapterCustom.also { bind.spCustom.adapter = it } // ?!?!!?!!?!?!??!?!?!!? JA JEBIE
        bind.spCustom.adapter = adapterCustom
        bind.spCustom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long) {
                //               Toast.makeText(this@MainActivity, "C: " + parent.getItemAtPosition(position) + " : " + position, Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        //----param 1
        bind.sbParam1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbParam1Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
//                Toast.makeText(this@MainActivity, "P1: ${sb?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        //----param 2
        bind.sbParam2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbParam2Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                //               Toast.makeText(this@MainActivity, "P2: ${sb?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        //----param 3
        bind.sbParam3.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbParam3Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
//                Toast.makeText(this@MainActivity, "P3: ${sb?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        //----param 4
        bind.sbParam4.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bind.lbParam4Val.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
//                Toast.makeText(this@MainActivity, "P4: ${sb?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        //----bool 1
        bind.swBool1.setOnCheckedChangeListener { _, b ->
//            Toast.makeText(this, "B1:"+b.toString(), Toast.LENGTH_SHORT).show()
        }
        //----bool 2
        bind.swBool2.setOnCheckedChangeListener { _, b ->
//            Toast.makeText(this, "B2:"+b.toString(), Toast.LENGTH_SHORT).show()
        }
        //----button confirm effect
        bind.btnEffectConfirm.setOnClickListener {
            when (bind.spEffect.selectedItem as String) {
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
            prepareUpdatedData()
            ConnectThread(mySelectedBluetoothDevice).writeMessage(prepareUpdatedData())
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