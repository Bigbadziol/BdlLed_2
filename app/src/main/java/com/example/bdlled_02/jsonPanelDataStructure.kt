package com.example.bdlled_02

data class jPanelFont(
    var id : Int,
    var name : String,
    var source : String
)

data class jPanelBgCalc(
    var id : Int,
    var name : String
)

data class jPanelBgRecorded(
    var id : Int,
    var name : String,
    var source : String,
)

data class  jPanelSentence(
    var id : Int,
    var sentence  :String,
    var fontColor: jColor,
    var fontId : Int,
    var bgType : String,
    var bgId : Int
)

data class jPanelData(
    var mode : Int,
    var cmd : String,
    var fonts : List<jPanelFont>,
    var bgCalc : List<jPanelBgCalc>,
    var bgRecorded : List<jPanelBgRecorded>,
    var sentences : List<jPanelSentence>
)
