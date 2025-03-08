## 基于freej2me的j2me模拟器，支持3D游戏
参考：

[freej2me](https://github.com/hex007/freej2me)

[J2ME-Loader](https://github.com/nikita36078/J2ME-Loader)

[JL-Mod](https://github.com/woesss/JL-Mod)

在miyoomini 、gkdminiplus、rg28xx、ubuntu18、trimui brick上进行了测试

编译：

前端使用sdl2，需编译出sdl_interface

声音使用sdl2-mixer，需编译出libaudio

3D游戏支持，M3G需编译出libm3g（依赖EGL,GLES1），Mascot Capsule v3需编译出libmicro3d（依赖EGL,GLES2）

运行：
```
export JAVA_TOOL_OPTIONS='-Xverify:none -Djava.util.prefs.systemRoot=./.java -Djava.util.prefs.userRoot=./.java/.userPrefs -Djava.library.path=./'

java -jar freej2me-sdl.jar /home/game.jar 240 320 100
```
240 320指游戏分辨率，100指音量（0-100）

可通过修改keymap.cfg，自定义按键映射。

替换font.ttf，更改字体。

[按键说明](https://github.com/aweigit/freej2me-miyoomini/blob/master/KEYMAP.md)

![都市摩天楼](https://github.com/aweigit/freej2me-miyoomini/blob/master/img/ubuntu18.png)

