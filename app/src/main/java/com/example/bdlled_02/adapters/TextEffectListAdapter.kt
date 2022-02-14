/*
* 13.02.2022
*  Od teraz nazwe traktuje się jako klucz , tłumaczenie na podstawie zasobów z stringów
 */
package com.example.bdlled_02.adapters
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.bdlled_02.R
import com.example.bdlled_02.TAG
import com.example.bdlled_02.jPanelTextEffect


class TextEffectListAdapter(val context: Context, var dataSource: ArrayList<jPanelTextEffect>) : BaseAdapter() {
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
        //Old
        //vh.label.text = dataSource.get(position).name
        //New from here
        val sourceKey :String = dataSource.get(position).name
        when (sourceKey){
            "Simple" -> translatedDescription = context.getString(R.string.keyTeSimple)
            "Fire text" -> translatedDescription = context.getString(R.string.keyTeFireText)
            "Rolling border" -> translatedDescription =context.getString(R.string.keyTeRollingBorder)
            "Colors"-> translatedDescription = context.getString(R.string.keyTeColors)
        }
        if (translatedDescription == "ERROR"){
            Log.d(TAG,"[ERROR]Text effect adapter -> key : $sourceKey not found, deleted from data source.")
            dataSource.removeAt(position)
        }
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