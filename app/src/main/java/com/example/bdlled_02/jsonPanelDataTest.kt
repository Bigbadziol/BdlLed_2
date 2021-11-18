package com.example.bdlled_02

var jsonPanelDataTest_small : String = """
{
  "mode": 0,
  "cmd" : "xxx",
  "cmdId" : 0,
  "lastSet" : -1,
  "panelBrightness" : 3,
  "fonts": [
    {
        "id": 0,
        "name": "Dialog"
    },
    {
        "id": 1,
        "name": "Chewy"
    },
    {
        "id": 2,
        "name": "Orbiton"
    }
  ],
  
  "textEffects": [
    {
      "name": "Scroll",
      "editable": 1,
      "type": 100
    },
    {
      "name": "Statyczny",
      "editable": 1,
      "type": 100
    }
  ],  
  
  "backgrounds": [
     {
      "name": "Dark net",
      "editable": 0,
      "type": 10
    },
    {
      "name": "Drops",
      "editable": 0,
      "type": 10
    },
    {
      "name": "Light lines",
      "editable": 0,
      "type": 10
    },
    {
      "name": "Spiral",
      "editable": 0,
      "type": 10
    },
    {
      "name": "Squaeres",
      "editable": 0,
      "type": 10
    },
    {
      "name": "Stars",
      "editable": 0,
      "type": 10
    },
    {
      "name": "Fire 1",
      "editable": 1,
      "type": 30
    },
    {
      "name": "Fire 2",
      "editable": 1,
      "type": 30
    },
    {
      "name": "Fire 3",
      "editable": 1,
      "type": 30
    },
    {
      "name": "Rain",
      "editable": 1,
      "type": 30
    }  
  ],

  "sentences": [
    {
      "id": 0,
      "sentence": "Jacek jedzie",
      "scrollDelay": 20,
      "bgDelay": 30,
      "font": {
        "fontId": 0,
        "fontType": "lbi",
        "color": {
          "r": 255,
          "g": 128,
          "b": 34
        }
      },
      "textEffect": {
        "name": "Scroll",
        "editable": 1,
        "type": 100,
        "data": {
          "scrollType": 6
        }
      },
      "background": {
        "name": "Rain",
        "editable": 1,
        "type": 30,
        "data": {
          "color1Start": {
            "r": 255,
            "g": 255,
            "b": 255
          },
          "color1Stop": {
            "r": 255,
            "g": 0,
            "b": 0
          },
          "color2Start": {
            "r": 255,
            "g": 0,
            "b": 0
          },
          "color2Stop": {
            "r": 32,
            "g": 0,
            "b": 0
          },
          "fillBg": 0
        }
      }
    },
    {
      "id": 1,
      "sentence": "Fire 1 Fire 1",
      "scrollDelay": 20,
      "bgDelay": 30,
      "font": {
        "fontId": 1,
        "fontType": "sn",
        "color": {
          "r": 0,
          "g": 128,
          "b": 196
        }
      },
      "textEffect": {
        "name": "Scroll",
        "editable": 1,
        "type": 100,
        "data": {
          "scrollType": 0
        }
      },
      "background": {
        "name": "Fire 1",
        "editable": 1,
        "type": 30,
        "data": {
          "flareRows": 2,
          "flareChance": 3,
          "flareDecay": 6,
          "dir": 2
        }
      }
    },
    {
      "id": 2,
      "sentence": "GreeN",
      "scrollDelay": 30,
      "bgDelay": 40,
      "font": {
        "fontId": 2,
        "fontType": "mbi",
        "color": {
          "r": 0,
          "g": 255,
          "b": 0
        }
      },
      "textEffect": {
        "name": "Scroll",
        "editable": 1,
        "type": 100,
        "data": {
          "scrollType": 2
        }
      },
      "background": {
        "name": "Fire 2",
        "editable": 1,
        "type": 30,
        "data": {
          "palette": 2,
          "heat": 2
        }
      }
    },
    {
      "id": 3,
      "sentence": "Fire 3 Fire 3 ",
      "scrollDelay": 20,
      "bgDelay": 50,
      "font": {
        "fontId": 0,
        "fontType": "mb",
        "color": {
          "r": 0,
          "g": 0,
          "b": 255
        }
      },
      "textEffect": {
        "name": "Scroll",
        "editable": 1,
        "type": 100,
        "data": {
          "scrollType": 6
        }
      },
      "background": {
        "name": "Fire 3",
        "editable": 1,
        "type": 30,
        "data": {
          "palette": 0,
          "cooling": 6,
          "sparking": 11
        }
      }
    },
    {
      "id": 4,
      "sentence": "ID4: text jakis dlugi itd. ",
      "scrollDelay": 20,
      "bgDelay": 40,
      "font": {
        "fontId": 0,
        "fontType": "si",
        "color": {
          "r": 0,
          "g": 0,
          "b": 255
        }
      },
      "textEffect": {
        "name": "Scroll",
        "editable": 1,
        "type": 100,
        "data": {
          "scrollType": 1
        }
      },
      "background": {
        "name": "Spiral",
        "editable": 0,
        "type": 10,
        "data": {
          "source": "spiral1.out",
          "loop": 1
        }
      }
    },
    {
      "id": 156,
      "sentence": "handle ADD",
      "scrollDelay": 20,
      "bgDelay": 30,
      "font": {
        "fontId": 0,
        "fontType": "mn",
        "olor": {
          "": 34
        }
      },
      "textEffect": {
        "name": "Scroll",
        "editable": 1,
        "type": 100,
        "data": {
          "scrollType": 3
        }
      },
      "background": {
        "name": "Rain",
        "editable": 1,
        "type": 30,
        "data": {
          "color1Start": {
            "r": 255,
            "g": 255,
            "b": 255
          },
          "color1Stop": {
            "r": 255,
            "g": 0,
            "b": 0
          },
          "color2Start": {
            "r": 255,
            "g": 0,
            "b": 0
          },
          "color2Stop": {
            "r": 32,
            "g": 0,
            "b": 0
          },
          "fillBg": 0
        }
      }
    },
    {
      "id": 157,
      "sentence": "Update1",
      "scrollDelay": 30,
      "bgDelay": 30,
      "font": {
        "fontId": 2,
        "fontType": "lbi",
        "color": {
          "r": 255,
          "g": 0,
          "b": 255
        }
      },
      "textEffect": {
        "name": "Scroll",
        "editable": 1,
        "type": 100,
        "data": {
          "scrollType": 2
        }
      },
      "background": {
        "name": "Fire 2",
        "editable": 1,
        "type": 30,
        "data": {
          "heat": 2,
          "palette": 3
        }
      }
    }
  ]
}    

""".trimIndent()