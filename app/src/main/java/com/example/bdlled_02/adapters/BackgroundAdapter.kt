/*
* 13.02.2022
*  Od teraz nazwe traktuje się jako klucz , tłumaczenie na podstawie zasobów z stringów
* Wazne ! Dostęp do danych troszkę inaczej zrealizowany niz w TextPositionAdapter czy TextEffectAdapter
 */
package com.example.bdlled_02.adapters

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.bdlled_02.R
import com.example.bdlled_02.TAG
import com.example.bdlled_02.jPanelBackgrounds


//Background calculated , only for panel devices
class BgCalcAdapter(private val  context: Activity, var items : ArrayList<jPanelBackgrounds>)
    : BaseAdapter(){
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(context)
        val view : View = layoutInflater.inflate(R.layout.spinner_row,null)
        val effectName : TextView = view.findViewById(R.id.tvSpinnerName)

        var translatedDescription ="ERROR"

        var prefix  ="[?]"
        var bg = items[position]
        when(bg.type){
            10 -> prefix = "[A]"
            20 -> prefix = "[P]"
            30 -> prefix = "[L]"
        }

        val sourceKey :String = items.get(position).name
        when (sourceKey){
            "Selected color" -> translatedDescription = context.getString(R.string.keyBgSelectedColor)
            "Fire 1"         -> translatedDescription = context.getString(R.string.keyBgFire1)
            "Fire 2"         -> translatedDescription = context.getString(R.string.keyBgFire2)
            "Fire 3"         -> translatedDescription = context.getString(R.string.keyBgFire3)
            "Rain"           -> translatedDescription = context.getString(R.string.keyBgRain)
            "Streak"         -> translatedDescription = context.getString(R.string.keyBgStreak)
            "Liquid plasma"  -> translatedDescription = context.getString(R.string.keyBgLiquidPlasma)
            "Big plasma"     -> translatedDescription = context.getString(R.string.keyBgBigPlasma)
            "Dark net"       -> translatedDescription = context.getString(R.string.keyBgDarkNet)
            "Drops"          -> translatedDescription = context.getString(R.string.keyBgDrops)
            "Light lines"    -> translatedDescription = context.getString(R.string.keyBgLightLines)
            "Spiral"         -> translatedDescription = context.getString(R.string.keyBgSpiral)
            "Squaeres"       -> translatedDescription = context.getString(R.string.keyBgSquaeres)
            "Stars"          -> translatedDescription = context.getString(R.string.keyBgStars)
            "Color fluid"    -> translatedDescription = context.getString(R.string.keyBgColorFluid)
            "Colormania"     -> translatedDescription = context.getString(R.string.keyBgColormania)
            "Dancing colors" -> translatedDescription = context.getString(R.string.keyBgDancingColors)
            "Fire octopus"   -> translatedDescription = context.getString(R.string.keyBgFireOctopus)
            "Evil eye"       -> translatedDescription = context.getString(R.string.keyBgEvilEye)
            "Spotlights"     -> translatedDescription = context.getString(R.string.keyBgSpotlights)
        }
        if (translatedDescription == "ERROR"){
            Log.d(TAG,"[ERROR]Background adapter -> key : $sourceKey not found, deleted from data source.")
            items.removeAt(position)
        }
        //old
        //effectName.text = prefix + items[position].name

        //new
        effectName.text = prefix + translatedDescription
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