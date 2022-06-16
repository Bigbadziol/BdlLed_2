package com.example.bdlled_02

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView

import com.badziol.bdlled_02.TAG

enum class DEVICE_ACTION_TYPE{ADD,REMOVE,NOACTION}
data class DeviceListModel (var deviceName : String, var deviceAdress : String, var iconId : Int)

class DeviceListAdapter(private val  context: Activity,
                        var items : ArrayList<DeviceListModel>,
                        var bluetoothAdapter: BluetoothAdapter,
                        var action : DEVICE_ACTION_TYPE) :
    ArrayAdapter<DeviceListModel>(context , R.layout.device_list_row_new , items){

    @SuppressLint("MissingPermission")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(context)
        val view : View = layoutInflater.inflate(R.layout.device_list_row_new,null)

        val icon : ImageView = view.findViewById(R.id.ivIcon)
        val deviceName : TextView = view.findViewById(R.id.tvDeviceName)
        val deviceAdress : TextView = view.findViewById(R.id.tvDeviceAdress)
        val btn : ImageButton = view.findViewById(R.id.ibDeviceAction)


        icon.setImageResource(items[position].iconId)
        deviceName.text = items[position].deviceName
        deviceAdress.text = items[position].deviceAdress
        btn.setImageResource(R.drawable.device_no) //by default
        when(action){
            DEVICE_ACTION_TYPE.ADD->{
                btn.setImageResource(R.drawable.device_add)
            }
            DEVICE_ACTION_TYPE.REMOVE->{
                btn.setImageResource(R.drawable.device_remove)
            }
            DEVICE_ACTION_TYPE.NOACTION->{
                btn.setImageResource(R.drawable.device_no)
            }
        }

        btn.setOnClickListener {
            val f = context.getString(R.string.NO_DEVICE_NAME)
            if (items[position].deviceName.contentEquals(f) == false) {
                val device = bluetoothAdapter.getRemoteDevice(items[position].deviceAdress)
                if (device != null) {
                    when (action) {
                        DEVICE_ACTION_TYPE.ADD -> {
                            Log.d(TAG, "click to add : -> ${device.name} : ${device.address} ")
                            device.createBond()
                        }
                        DEVICE_ACTION_TYPE.REMOVE -> {
                            Log.d(TAG, "click to remove : -> ${device.name} : ${device.address} ")
                            try {
                                device::class.java.getMethod("removeBond").invoke(device)
                            } catch (e: Exception) {
                                Log.e(TAG, "Removing bond has been failed. ${e.message}")
                            }
                        }
                        DEVICE_ACTION_TYPE.NOACTION -> {
                            Log.d(TAG, "click no action")
                        }
                    }
                }
            }
        }
        return view
    }
}