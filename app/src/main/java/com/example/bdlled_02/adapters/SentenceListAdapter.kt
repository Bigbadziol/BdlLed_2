package com.example.bdlled_02.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.bdlled_02.R
import com.example.bdlled_02.jPanelSentence

class SentenceListAdapter(private val  context: Activity, var items : ArrayList<jPanelSentence>) :
    ArrayAdapter<jPanelSentence>(context , R.layout.device_list_row, items){
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(context)
        val view : View = layoutInflater.inflate(R.layout.sentence_list_row,null)

        val sentence : TextView = view.findViewById(R.id.tvSentence)

        sentence.text = items[position].sentence

        return view
    }
}