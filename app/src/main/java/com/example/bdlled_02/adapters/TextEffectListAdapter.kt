package com.example.bdlled_02.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.bdlled_02.R
import com.example.bdlled_02.jPanelTextEffect

class TextEffectListAdapter(val context: Context, var dataSource: ArrayList<jPanelTextEffect>) : BaseAdapter() {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val vh: ItemHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.spinner_row, parent, false)
            vh = ItemHolder(view)
            view?.tag = vh

        } else {
            view = convertView
            vh = view.tag as ItemHolder
        }
        vh.label.text = dataSource.get(position).name
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