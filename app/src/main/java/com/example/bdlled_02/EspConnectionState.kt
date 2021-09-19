package com.example.bdlled_02

enum class EspConnectionState(val description : String){
    DISCONNECTED("Rozlaczono"),
    CONNECTING("Lacze sie"),
    CONNECTED("Połączono"),
    CONNECTION_ERROR("Urzadzenie nie dostępne");
}