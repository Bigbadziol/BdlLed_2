package com.example.bdlled_02

import com.google.gson.JsonObject

data class jPanelFont(
    var id : Int,
    var name : String
)

data class  jPanelTextPosition(
    var name : String="",
    var editable: Int = 0,
    var type: Int = 0
)

data class jPanelTextEffect(
    var name : String="",
    var editable: Int = 0,
    var type : Int = 0
)

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
    var font : JsonObject = JsonObject(),
    var textPosition : JsonObject = JsonObject(),
    var textEffect : JsonObject = JsonObject(),
    var background : JsonObject = JsonObject()
)

data class jPanelData(
    var flash : Int = 0,
    var mode : Int = 0,
    var cmd : String = "xxx",
    var cmdId : Int = 0,
    var lastSet : Int = -1,
    var panelBrightness : Int = 4,
    var fonts : List<jPanelFont>,
    var textPositions : List<jPanelTextPosition>,
    var textEffects : List<jPanelTextEffect>,
    var backgrounds : List<jPanelBackgrounds>,
    var sentences : List<jPanelSentence>
)
