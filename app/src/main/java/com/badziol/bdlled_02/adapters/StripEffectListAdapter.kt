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
import com.badziol.bdlled_02.jStripEffect

class StripEffectListAdapter(val context: Context, var dataSource: ArrayList<jStripEffect>) : BaseAdapter() {
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
            "Beat wave" -> translatedDescription = context.getString(R.string.keyStBeatWave)
            "Blend wave" -> translatedDescription = context.getString(R.string.keyStBlendWave)
            "Blur" -> translatedDescription = context.getString(R.string.keyStBlur)
            "Confeti" -> translatedDescription = context.getString(R.string.keyStConfeti)
            "Sinelon" -> translatedDescription = context.getString(R.string.keyStSinelon)

            "Bpm" -> translatedDescription = context.getString(R.string.keyStBpm)
            "Juggle" -> translatedDescription = context.getString(R.string.keyStJuggle)
            "Dot beat" -> translatedDescription = context.getString(R.string.keyStDotBeat)
            "Easing" -> translatedDescription = context.getString(R.string.keyStEasing)
            "Hyper dot" -> translatedDescription = context.getString(R.string.keyStHyperDot)

            "Beat sin gradient" -> translatedDescription = context.getString(R.string.keyStBeatSinGradient)
            "Fire 1" -> translatedDescription = context.getString(R.string.keyStFire1)
            "Fire 1 two flames" -> translatedDescription = context.getString(R.string.keyStFire1TwoFlames)
            "Worm" -> translatedDescription = context.getString(R.string.keyStWorm)
            "Fire 2" -> translatedDescription = context.getString(R.string.keyStFire2)

            "Noise 1" -> translatedDescription = context.getString(R.string.keyStNoise1)
            "Juggle 2" -> translatedDescription = context.getString(R.string.keyStJuggle2)
            "Running color dots" -> translatedDescription = context.getString(R.string.keyStRunningColorDots)
            "Disco 1" -> translatedDescription = context.getString(R.string.keyStDisco1)
            "Running color dots 2" -> translatedDescription = context.getString(R.string.keyStRunningColorDots2)

            "Disco dots" -> translatedDescription = context.getString(R.string.keyStDiscoDots)
            "Plasma" -> translatedDescription = context.getString(R.string.keyStPlasma)
            "Rainbow sine" -> translatedDescription = context.getString(R.string.keyStRainbowSine)
            "Fast rainbow" -> translatedDescription = context.getString(R.string.keyStFastRainbow)
            "Pulse rainbow" -> translatedDescription = context.getString(R.string.keyStPulseRainbow)

            "Fireworks" -> translatedDescription = context.getString(R.string.keyStFireworks)
            "Fireworks 2" -> translatedDescription = context.getString(R.string.keyStFirewworks2)
            "Sin-neon" -> translatedDescription = context.getString(R.string.keyStSinNeon)
            "Carusel" -> translatedDescription = context.getString(R.string.keyStCarusel)
            "Color Wipe" -> translatedDescription = context.getString(R.string.keyStColorWipe)

            "Bounce bar" -> translatedDescription = context.getString(R.string.keyStBounceBar)
            "Chillout" -> translatedDescription = context.getString(R.string.keyStChillout)
            "Comet" -> translatedDescription = context.getString(R.string.keyStComet)

        }
        if (translatedDescription == "ERROR"){
            Log.d(TAG,"[ERROR]Strip effect adapter -> key : $sourceKey not found, deleted from data source.")
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