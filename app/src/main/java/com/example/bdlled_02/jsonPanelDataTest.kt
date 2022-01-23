package com.example.bdlled_02

var jsonPanelDataTest_small : String = """
{
  "fonts": [
    {"id": 0,"name": "Dialog"},
    {"id": 1,"name": "Chewy"},
    {"id": 2,"name": "Orbiton"},
    {"id": 3,"name": "Mountains"},
    {"id": 4,"name": "Redressed"},
    {"id": 5,"name": "Roboto slab"},
    {"id": 6,"name": "Walter"}
  ],
  "textPositions": [
    {"name": "Static","editable": 1,"type": 100},
    {"name": "Scroll","editable": 1,"type": 100},
    {"name": "Word by word","editable": 1,"type": 100}
  ],
  "textEffects": [
    {"name": "Simple","editable": 1,"type": 200},
    {"name": "Fire text","editable": 1,"type": 200},
    {"name": "Rolling border","editable": 1,"type": 200},
    {"name": "Colors","editable": 1,"type": 200}
  ],
  "backgrounds": [
    {"name": "Selected color","editable": 1,"type": 30},
    {"name": "Fire 1","editable": 1,"type": 30},
    {"name": "Fire 2","editable": 1,"type": 30},
    {"name": "Fire 3","editable": 1,"type": 30},
    {"name": "Rain","editable": 1,"type": 30},
    {"name": "Streak","editable": 1,"type": 30},
    {"name": "Liquid plasma","editable": 1,"type": 30},
    {"name": "Big plasma","editable": 1,"type": 30},
    {"name": "Dark net","editable": 0,"type": 10},
    {"name": "Drops","editable": 0,"type": 10},
    {"name": "Light lines","editable": 0,"type": 10},
    {"name": "Spiral","editable": 0,"type": 10},
    {"name": "Squaeres","editable": 0,"type": 10},
    {"name": "Stars","editable": 0,"type": 10},
    {"name": "Color fluid","editable": 0,"type": 10},
    {"name": "Colormania","editable": 0,"type": 10},
    {"name": "Dancing colors","editable": 0,"type": 10},
    {"name": "Fire octopus","editable": 0,"type": 10},
    {"name": "Evil eye","editable": 0,"type": 10},
    {"name": "Spotlights","editable": 0,"type": 10}
  ],
  "sentences": [
    {
      "id": 100,
      "sentence": "Big Zerg",
      "scrollDelay": 30,
      "bgDelay": 30,
      "font": {"fontId": 1,"fontType": "lb",
        "color": {"r": 3,"g": 218,"b": 197},
        "borderType": 0,
        "borderColor": {"r": 0,"g": 0,"b": 0}
      },
      "textPosition": {"name": "Scroll","editable": 1,"type": 100,"data": {"scrollType": 4}},
      "textEffect": { "name": "Simple color","editable": 1,"type": 200,"data": {"mode": 0} },  
      "background": {
        "name": "Big plasma","editable": 1,"type": 30,
        "data": {"palette": 1,"nextMove": 1}
      }
    },
    {
      "id": 101,
      "sentence": "PLOMienie",
      "scrollDelay": 30,
      "bgDelay": 30,
      "font": {"fontId": 4,"fontType": "lb",
        "color": {"r": 255,"g": 218,"b": 255},
        "borderType": 1,
        "borderColor": {"r": 0,"g": 255,"b": 0}
      },
      "textPosition": {"name": "Scroll","editable": 1,"type": 100,"data": {"scrollType": 0}},
      "textEffect": {"name": "Fire text","editable": 1,"type": 200,"data": {"cooling": 4,"sparking": 8}},
      "background": {
        "name": "Selected color","editable": 1,"type": 30,
        "data": {"color": {"r": 0,"g": 0,"b": 0}}
      }
    }
  ],
  "flash": 1,
  "cmd": "MY_ALL_DATA",
  "cmdId": 0,
  "mode": 0,
  "lastSet": -1
}

""".trimIndent()