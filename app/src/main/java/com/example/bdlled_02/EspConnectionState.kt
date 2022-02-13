package com.example.bdlled_02
/*
* 12.02.2022
* At this point description means nothing
* Some problems with translation, now is a function "espStateDescription()" with get current
*  string from resources
*/

enum class EspConnectionState(val description : String){
    DISCONNECTED("Rozłączono"),
    CONNECTING("Lacze sie"),
    CONNECTED("Połączono"),
    CONNECTION_ERROR("Urzadzenie nie dostępne");
}




