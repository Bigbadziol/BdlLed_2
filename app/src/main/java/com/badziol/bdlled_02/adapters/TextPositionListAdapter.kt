/*
* 13.02.2022
*  Od teraz nazwe traktuje się jako klucz , tłumaczenie na podstawie zasobów z stringów
 */
package com.badziol.bdlled_02.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.bdlled_02.R
import com.badziol.bdlled_02.TAG
import com.badziol.bdlled_02.jPanelTextPosition

class TextPositionListAdapter(val context: Context, var dataSource: ArrayList<jPanelTextPosition>) : BaseAdapter() {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val vh: ItemHolder
        var translatedDescription ="ERROR"

        if (convertView == null) {
            view = inflater.inflate(R.layout.spinner_row, parent, false)
            vh = ItemHolder(view)
            view?.tag = vh

        } else {
            view = convertView
            vh = view.tag as ItemHolder
        }

        val sourceKey :String = dataSource.get(position).name
        when (sourceKey){
            "Static" -> translatedDescription = context.getString(R.string.keyTpStatic)
            "Scroll" -> translatedDescription = context.getString(R.string.keyTpScroll)
            "Word by word" -> translatedDescription =context.getString(R.string.keyTpWordByWord)
        }

        if (translatedDescription == "ERROR"){
            Log.d(TAG,"[ERROR]Text position adapter -> key : $sourceKey not found, deleted from data source.")
            dataSource.removeAt(position)
        }
        //old
        //vh.label.text = dataSource.get(position).name
        vh.label.text = translatedDescription
        return view
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getCount(): Int {
        return dataSource.size
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