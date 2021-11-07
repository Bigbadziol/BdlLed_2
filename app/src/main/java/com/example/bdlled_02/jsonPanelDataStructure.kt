package com.example.bdlled_02

import com.google.gson.JsonObject

data class jPanelFont(
    var id : Int,
    var name : String
)

data class  jPanelTextEffect(
    var name : String="",
    var editable: Int = 0,
    var type: Int = 0
)

//background
data class jPanelBackgrounds(
    var name : String="",
    var editable : Int= 0,
    var type : Int = 0
)


data class  jPanelSentence(
    var id : Int=0,
    var sentence  :String="",
    var scrollDelay : Int = 0,
    var bgDelay : Int = 0,
    var font : JsonObject = JsonObject(),       //added = JsonObject
    var texEffect : JsonObject = JsonObject(),  //added = JsonObject
    var background : JsonObject = JsonObject()  //added = JsonObject
)

data class jPanelData(
    var mode : Int,
    var cmd : String,
    var fonts : List<jPanelFont>,
    var textEffects : List<jPanelTextEffect>,
    var backgrounds : List<jPanelBackgrounds>,
    var sentences : List<jPanelSentence>
)
