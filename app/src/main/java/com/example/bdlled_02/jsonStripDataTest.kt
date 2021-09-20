package com.example.bdlled_02
var jsonStripDataTest_small = """{
    "cmd":"DATA",
	"config": {
		"mode": 0,
		"selected": 0,
		"color": {
			"r": 255,
			"g": 255,
			"b": 67
		},
		"time": 20
	}
, 
	"effects": [
	{
		"name": "Beat wave",
		"editable": 1,
		"data": {
			"pulse1": 6,
			"pulse2": 7,
			"pulse3": 8,
			"pulse4": 9
		}
	},
	{
		"name": "Blend wave",
		"editable": 1,
		"data": {
			"speed": 6,
			"mH1": 6,
			"mH2": 8
		}
	}
]
}""".trimIndent()


///--------BIG DATA------
var jsonStripDataTest_big : String ="""
{
    "cmd":"DUPA",
	"config": {
		"mode": 0,
		"selected": 6,
		"color": {
			"r": 255,
			"g": 255,
			"b": 67
		},
		"time": 20
	}
, 
	"effects": [
	{
		"name": "Beat wave",
		"editable": 1,
		"data": {
			"pulse1": 6,
			"pulse2": 7,
			"pulse3": 8,
			"pulse4": 9
		}
	},
	{
		"name": "Blend wave",
		"editable": 1,
		"data": {
			"speed": 6,
			"mH1": 6,
			"mH2": 8
		}
	},
	{
		"name": "Blur",
		"editable": 1,
		"data": {
			"speed": 5,
			"o1": 20,
			"o2": 13,
			"o3": 9
		}
	},
	{
		"name": "Confeti",
		"editable": 1,
		"data": {
			"pIndex": 2,
			"fade": 8,
			"mDiff": 1
		}
	},
	{
		"name": "Sinelon",
		"editable": 1,
		"data": {
			"bpm": 13,
			"fade": 20
		}
	},
	{
		"name": "Bpm",
		"editable": 1,
		"data": {
			"pIndex": 5,
			"bpm": 60
		}
	},
	{
		"name": "Juggle",
		"editable": 1,
		"data": {
			"stepHue": 4,
			"fade": 20
		}
	},
	{
		"name": "Dot beat",
		"editable": 1,
		"data": {
			"color1": {
				"r": 34,
				"g": 139,
				"b": 34
			},
			"color2": {
				"r": 173,
				"g": 255,
				"b": 47
			},
			"bpm": 20,
			"fadeMod": 0
		}
	},
	{
		"name": "Easing",
		"editable": 1,
		"data": {
			"color": {
				"r": 34,
				"g": 139,
				"b": 34
			},
			"multiplier": 1
		}
	},
	{
		"name": "Hyper dot",
		"editable": 1,
		"data": {
			"color": {
				"r": 178,
				"g": 34,
				"b": 34
			},
			"bpm": 5,
			"low": 20,
			"high": 100
		}
	},
	{
		"name": "Beat sin gradient",
		"editable": 1,
		"data": {
			"start": 5,
			"end": 7
		}
	},
	{
		"name": "Fire 1",
		"editable": 1,
		"data": {
			"cooling": 55,
			"sparking": 120
		}
	},
	{
		"name": "Fire 1 two flames",
		"editable": 1,
		"data": {
			"cooling": 55,
			"sparking": 120
		}
	},
	{
		"name": "Worm",
		"editable": 1,
		"data": {
			"adjust": 16,
			"nextBlend": 15
		}
	},
	{
		"name": "Fire 2",
		"editable": 1,
		"data": {
			"dir": 1,
			"intensity": 0,
			"speed": 2
		}
	},
	{
		"name": "Noise 1",
		"editable": 1,
		"data": {
			"pIndex": 3,
			"low": 1,
			"high": 11
		}
	},
	{
		"name": "Juggle 2",
		"editable": 1,
		"data": {
			"dots": 8,
			"beat": 10,
			"fade": 7
		}
	},
	{
		"name": "Running color dots",
		"editable": 1,
		"data": {
			"pIndex": 0,
			"dir": 0
		}
	},
	{
		"name": "Disco 1",
		"editable": 1,
		"data": {
			"pIndex": 5,
			"flash": 1
		}
	},
	{
		"name": "Running color dots 2",
		"editable": 1,
		"data": {
			"pIndex": 5,
			"bgColor": {
				"r": 0,
				"g": 0,
				"b": 0
			},
			"bgBright": 50,
			"bgStatic": 1
		}
	},
	{
		"name": "Disco dots",
		"editable": 1,
		"data": {
			"phaseTime": 5

		}
	},
	{
		"name": "Plasma",
		"editable": 1,
		"data": {
			"pIndex": 2,
			"low": -50,
			"high": 96,
			"calcMod": 3
		}
	},
	{
		"name": "Rainbow sine",
		"editable": 1,
		"data": {
			"speed": 8,
			"hueStep": 8
		}
	},
	{
		"name": "Fast rainbow",
		"editable": 1,
		"data": {
			"speed": 2,
			"delta": 3
		}
	},
	{
		"name": "Pulse rainbow",
		"editable": 1,
		"data": {
			"rot": 1,
			"hue": 1,
			"dir": 0,
			"delay": 40
		}
	},
	{
		"name": "Fireworks",
		"editable": 1,
		"data": {
			"size": 1,
			"speed": 2
		}
	},
	{
		"name": "Fireworks 2",
		"editable": 0,
		"data": {}
	},
	{
		"name": "Sin-neon",
		"editable": 0,
		"data": {}
	},
	{
		"name": "Carusel",
		"editable": 0,
		"data": {}
	},
	{
		"name": "Color Wipe",
		"editable": 1,
		"data": {
			"color1": {
				"r": 195,
				"g": 186,
				"b": 0
			},
			"color2": {
				"r": 0,
				"g": 93,
				"b": 42
			},
			"delay1": 50,
			"delay2": 25,
			"clear": 0
		}
	},
	{
		"name": "Bounce bar",
		"editable": 1,
		"data": {
			"color": {
				"r": 255,
				"g": 0,
				"b": 0
			},
			"size": 6,
			"delay": 20
		}
	},
	{
		"name": "Chillout",
		"editable": 1,
		"data": {
			"heat": 0,
			"delay": 20
		}
	},
	{
		"name": "Comet",
		"editable": 1,
		"data": {
			"solid": 1,
			"color": {
				"r": 255,
				"g": 0,
				"b": 0
			},
			"size": 5,
			"delay": 50
		}
	}

] }
    
""".trimIndent()
