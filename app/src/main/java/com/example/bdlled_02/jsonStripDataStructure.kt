package com.example.bdlled_02

import com.google.gson.JsonObject

data class jStripConfig(
    var mode : Int ,
    var selected : Int ,
    var color : jColor ,
    var time : Int
)

data class jStripEffect(
    var name : String,
    var editable : Int,  //ciekawa sprawa !! editable intem bo esp boola robi 1/0 i przez to sie wywala
    var data  : JsonObject
)

data class jStripData(
    var cmd : String,
    var config : jStripConfig,
    var effects : List<jStripEffect>
)
