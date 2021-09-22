package com.example.bdlled_02

data class jPanelFont(
    var id : Int,
    var name : String,
    var source : String
)
//background calculated
data class jPanelBgCalc(
    var id : Int,
    var name : String
)
//background pre-recorded
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
    var bgType : String, //values "calc" or "recorded" , default first "calc"
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
