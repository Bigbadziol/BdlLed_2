package com.example.bdlled_02

data class jPanelFont(
    var id : Int,
    var name : String,
    var source : String
)
//background
data class jPanelBackgrounds(
    var id : Int = 0 ,
    var bgType : String="", //values "calc" or "recorded" , default first "calc"
    var name : String="",
    var source : String=""
)


data class  jPanelSentence(
    var id : Int=0,
    var sentence  :String="",
    var fontColor: jColor = jColor(),
    var fontId : Int= 0,
    var bgId : Int = 0

)

data class jPanelData(
    var mode : Int,
    var cmd : String,
    var fonts : List<jPanelFont>,
    var backgrounds : List<jPanelBackgrounds>,
    var sentences : List<jPanelSentence>
)
