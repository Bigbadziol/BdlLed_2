package com.example.bdlled_02.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.bdlled_02.R
import com.example.bdlled_02.jPanelBackgrounds


//Background calculated , only for panel devices
class BgCalcAdapter(private val  context: Activity, var items : ArrayList<jPanelBackgrounds>)
    : BaseAdapter(){
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(context)
        val view : View = layoutInflater.inflate(R.layout.spinner_row,null)

        val effectName : TextView = view.findViewById(R.id.tvSpinnerName)

        var prefix : String ="[?]"
        var bg = items[position]
        if (bg.type==10) prefix="[A]"
        else if (bg.type==20) prefix="[P]"
        else if (bg.type == 30) prefix ="[L]"

        effectName.text = prefix + items[position].name
        return view
    }

    override fun getItem(position: Int): Any {
        return items [position]
    }

    override fun getCount(): Int {
        return items .size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private class ItemHolder(row: View?) {
        val label: TextView
        init {
            label = row?.findViewById(R.id.tvSpinnerName) as TextView
        }
    }
}