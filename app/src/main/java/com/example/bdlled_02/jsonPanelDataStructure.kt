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
    var textEffect : JsonObject = JsonObject(),  //added = JsonObject
    var background : JsonObject = JsonObject()  //added = JsonObject
)

data class jPanelData(
    var mode : Int = 0,
    var cmd : String = "xxx",
    var cmdId : Int = 0,
    var lastSet : Int = -1,
    var panelBrightness : Int = 4,
    var fonts : List<jPanelFont>,
    var textEffects : List<jPanelTextEffect>,
    var backgrounds : List<jPanelBackgrounds>,
    var sentences : List<jPanelSentence>
)
