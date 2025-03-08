## J2ME emulator based on freej2me, supporting 3D games
### reference
[freej2me](https://github.com/hex007/freej2me)

[J2ME-Loader](https://github.com/nikita36078/J2ME-Loader)

[JL-Mod](https://github.com/woesss/JL-Mod)

Tested on miyoomini, gkdminiplus, rg28xx, ubuntu18, trimui brick
 
#### compile:
 
The front-end uses sdl2,  needs to compile **sdl_interface**
 
The sound uses sdl2-mixer, needs to compile **libaudio.so**
 
3D game support, M3G needs to compile **libm3g.so** (depending on EGL, GLES1), Mascot Capsule v3 needs to compile **libmicro3d.so** (depending on EGL, GLES2)
 
#### run:
 
```
export JAVA_TOOL_OPTIONS='-Xverify:none -Djava.util.prefs.systemRoot=./.java -Djava.util.prefs.userRoot=./.java/.userPrefs -Djava.library.path=./'

java -jar freej2me-sdl.jar /home/game.jar 240 320 100
```

240 320 refers to the game resolution, and 100 refers to the volume (0-100)