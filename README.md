# FreeJ2ME-MiyooMini — J2ME 模拟器 (支持 3D 游戏)

[English Version](#freej2me-miyoomini--j2me-emulator-with-3d-support)

基于 [FreeJ2ME](https://github.com/hex007/freej2me) 的 J2ME 模拟器，参考了 [J2ME-Loader](https://github.com/nikita36078/J2ME-Loader) 和 [JL-Mod](https://github.com/woesss/JL-Mod)，专为掌机设备适配。支持 M3G (JSR-184) 和 MascotCapsule v3 (micro3d) 3D API，使用 SDL2 作为前端，通过 JNI 原生库实现音频和 3D 渲染。

**已测试设备:** Miyoo Mini / GKD Mini Plus / RG28XX / RG35XX Plus / RG35XX H / TrimUI Brick / Miyoo Flip / Miyoo A30 / Ubuntu 18

---

## 目录

- [项目结构](#项目结构)
- [依赖项](#依赖项)
- [编译构建](#编译构建)
- [运行方法](#运行方法)
- [按键说明](#按键说明)
- [自定义配置](#自定义配置)
- [许可证](#许可证)

---

## 项目结构

```
freej2me-miyoomini/
├── build.xml                      # Ant 构建文件 (Java 编译打包)
├── keymap.cfg                     # 按键映射配置文件 (JSON)
├── lib/
│   └── jsr305-3.0.2.jar          # JSR305 注解库 (仅构建时使用)
├── cpp/
│   ├── sdl2/                      # SDL2 前端 (C++)
│   │   ├── run.sh                 # 各平台编译命令脚本
│   │   ├── miyoomini.cpp          # Miyoo Mini SDL2 前端
│   │   └── sdl2_interface_general.cpp  # 通用SDL2 前端
│   └── native/
│       ├── sdlmixer/              # SDL2_mixer JNI 音频桥接
│       ├── m3g/                   # M3G (JSR-184) 3D渲染
│       └── micro3d/               # MascotCapsule v3 3D渲染
└── src/                           # Java 源代码
```

---

## 依赖项

### 运行时依赖

| 依赖 | 用途 | 必需 |
|------|------|------|
| **SDL2** (`libSDL2`) | 图形窗口、输入处理 | 是 |
| **SDL2_mixer** (`libSDL2_mixer`) | 音频播放 | 是 (声效) |
| **OpenGL ES 1.1** (`libGLESv1_CM`) | M3G 3D 渲染 | 需要 3D 时 |
| **OpenGL ES 2.0** (`libGLESv2`) | micro3d 渲染 | 需要 3D 时 |
| **EGL** (`libEGL`) | OpenGL 上下文管理 | 需要 3D 时 |
| **font.ttf** | 游戏字体 | 是 |

---

## 编译构建

### 1. 编译 Java 代码

在项目根目录下执行（使用jdk17编译）：

```bash
ant
```

生成 `build/freej2me-sdl.jar`，主类为 `org.recompile.freej2me.Anbu`。

### 2. 编译 SDL2 前端

根据目标设备选择对应的源文件和编译命令，参考 `cpp/sdl2/run.sh`：

| 目标设备 | 编译器 | 源文件 |
|----------|--------|--------|
| Miyoo Mini (ARM32) | `arm-linux-gnueabihf-g++` | `miyoomini.cpp` | 
| 通用版本 | `aarch64-none-linux-gnu-g++` | `sdl2_interface_general.cpp` |
| ... | `...` | `...` |

输出均为 `sdl_interface` 可执行文件。

### 3. 编译原生库

根据目标架构选择对应的 Makefile：

```bash
cd cpp/native

# 音频库 (必需)
make -f Makefile.sdlmixer.arm32   # 或 .arm64 → libaudio.so 

# M3G 3D 库 (可选, 依赖 EGL + GLES1)
make -f Makefile.m3g.arm32        # 或 .arm64 → libm3g.so

# MascotCapsule v3 3D 库 (可选, 依赖 EGL + GLES2)
make -f Makefile.micro3d.arm32    # 或 .arm64 → libmicro3d.so
```

最终构建产物的目录结构示例：

```
.
├── freej2me-sdl.jar          # Java 主程序
├── sdl_interface             # SDL2 前端
├── libaudio.so               # 音频库
├── libm3g.so                 # M3G 3D 库 (可选)
├── libmicro3d.so             # micro3d 库 (可选)
├── font.ttf                  # 自定义字体
└── keymap.cfg                # 按键配置
```

---

## 运行方法

```bash
export JAVA_TOOL_OPTIONS='-Xverify:none -Djava.util.prefs.systemRoot=./.java -Djava.util.prefs.userRoot=./.java/.userPrefs -Djava.library.path=./'

java -jar freej2me-sdl.jar <游戏JAR路径> <宽度> <高度> <音量>
```

**示例:**

```bash
java -jar freej2me-sdl.jar /home/games/city.jar 240 320 100
```

**参数说明:**

| 参数 | 说明 | 示例 |
|------|------|------|
| `<游戏JAR路径>` | J2ME 游戏 JAR 文件的绝对路径 | `/path/to/game.jar` |
| `<宽度>` | 游戏画面宽度 (像素) | `240` |
| `<高度>` | 游戏画面高度 (像素) | `320` |
| `<音量>` | 音量等级 (0-100，实际代码里没使用) | `100` |

---

## 按键说明

### 默认按键映射

| 游戏按键 | 掌机按键 | 说明 |
|----------|----------|------|
| 左键 | Y | 手机左软键 |
| 右键 | A | 手机右软键 |
| OK | X | 确认键 |
| 0 | B | 数字键 0 |
| * | SELECT | 星号键 |
| # | START | 井号键 |
| 1 | L1 | 数字键 1 |
| 3 | R1 | 数字键 3 |
| 7 | L2 | 数字键 7 |
| 9 | R2 | 数字键 9 |

### 快捷键操作

| 组合键 | 功能 |
|--------|------|
| **SELECT + START** | 循环切换按键模式 (p→n→e→s→m, 共5种) |
| **SELECT + B** | 画面旋转 90 度 (顺/逆时针) |
| **SELECT + Y** | 切换触屏模式 (白色鼠标光标, X键确认) |
| **MENU/GUIDE** | 强制退出游戏 |
| **左摇杆按下** | 十字方向键变为 2/4/6/8 |
| **右摇杆按下** | 作为 OK 键 |

### 文字输入

- 数字键 **1 / 3** (默认 L1 / R1): 输入数字
- **左 / 右** 方向键: 输入字母
- **\*** 键 (默认 SELECT): 删除字符
- **#** 键 (默认 START): 添加字符

---

## 自定义配置

### 按键映射 (`keymap.cfg`)

编辑 `keymap.cfg` (JSON 格式):

```json
{
    "左键": "Y",
    "右键": "A",
    "OK":   "X",
    "*":    "SELECT",
    "#":    "START",
    "0":    "B",
    "1":    "L",
    "3":    "R",
    "7":    "L2",
    "9":    "R2"
}
```

### 字体 (`font.ttf`)

替换程序同目录下的 `font.ttf` 文件可更改游戏内字体。

---
### 演示

![都市摩天楼](https://github.com/aweigit/freej2me-miyoomini/blob/master/img/ubuntu18.png)

---

## 许可证

本项目基于 [GNU General Public License v3.0](LICENSE) 发布。部分代码 (ObjectWeb ASM) 采用 **ObjectWeb ASM License**。

Portions copyright (c) 2000-2011 INRIA, France Telecom. All rights reserved.

---

# FreeJ2ME-MiyooMini — J2ME Emulator (with 3D Support)

[中文版](#freej2me-miyoomini--j2me-模拟器-支持-3d-游戏)

A J2ME emulator based on [FreeJ2ME](https://github.com/hex007/freej2me), referencing [J2ME-Loader](https://github.com/nikita36078/J2ME-Loader) and [JL-Mod](https://github.com/woesss/JL-Mod), specifically adapted for handheld gaming devices. Supports M3G (JSR-184) and MascotCapsule v3 (micro3d) 3D APIs, using SDL2 as the frontend with JNI native libraries for audio and 3D rendering.

**Tested on:** Miyoo Mini / GKD Mini Plus / RG28XX / RG35XX Plus / RG35XX H / TrimUI Brick / Miyoo Flip / Miyoo A30 / Ubuntu 18

---

## Table of Contents

- [Project Structure](#project-structure)
- [Dependencies](#dependencies)
- [Build](#build)
- [Usage](#usage)
- [Controls](#controls)
- [Custom Configuration](#custom-configuration)
- [License](#license)

---

## Project Structure

```
freej2me-miyoomini/
├── build.xml                      # Ant build file (Java compilation)
├── keymap.cfg                     # Key mapping config (JSON)
├── lib/
│   └── jsr305-3.0.2.jar          # JSR305 annotations (build only)
├── cpp/
│   ├── sdl2/                      # SDL2 frontend (C++)
│   │   ├── run.sh                 # Build commands for all targets
│   │   ├── miyoomini.cpp          # Miyoo Mini SDL2 frontend
│   │   └── sdl2_interface_general.cpp  # Generic SDL2 frontend
│   └── native/
│       ├── sdlmixer/              # SDL2_mixer JNI audio bridge
│       ├── m3g/                   # M3G (JSR-184) native renderer
│       └── micro3d/               # MascotCapsule v3 native renderer
└── src/                           # Java source code
```

---

## Dependencies

### Runtime

| Dependency | Purpose | Required |
|------------|---------|----------|
| **SDL2** (`libSDL2`) | Graphics window, input handling | Yes |
| **SDL2_mixer** (`libSDL2_mixer`) | Audio playback | Yes (for sound) |
| **OpenGL ES 1.1** (`libGLESv1_CM`) | M3G 3D rendering | For 3D games |
| **OpenGL ES 2.0** (`libGLESv2`) | micro3d rendering | For 3D games |
| **EGL** (`libEGL`) | OpenGL context management | For 3D games |
| **font.ttf** | Custom game font | Yes |

---

## Build

### 1. Build Java Code

Run from the project root (using JDK 17):

```bash
ant
```

Produces `build/freej2me-sdl.jar` with main class `org.recompile.freej2me.Anbu`.

### 2. Build SDL2 Frontend

Choose the source file matching your target device. See `cpp/sdl2/run.sh`:

| Target | Compiler | Source |
|--------|----------|--------|
| Miyoo Mini (ARM32) | `arm-linux-gnueabihf-g++` | `miyoomini.cpp` |
| general version | `aarch64-none-linux-gnu-g++` | `sdl2_interface_general.cpp` |
| ... | `...` | `...` |

All produce `sdl_interface` executable.

### 3. Build Native Libraries

Select the appropriate Makefile for your target architecture:

```bash
cd cpp/native

# Audio library (required)
make -f Makefile.sdlmixer.arm32   # or .arm64 → libaudio.so

# M3G 3D library (optional, requires EGL + GLES1)
make -f Makefile.m3g.arm32        # or .arm64 → libm3g.so

# MascotCapsule v3 library (optional, requires EGL + GLES2)
make -f Makefile.micro3d.arm32    # or .arm64 → libmicro3d.so
```

Expected output layout:

```
.
├── freej2me-sdl.jar          # Java main program
├── sdl_interface             # SDL2 frontend
├── libaudio.so               # Audio library
├── libm3g.so                 # M3G 3D library (optional)
├── libmicro3d.so             # micro3d library (optional)
├── font.ttf                  # Custom font
└── keymap.cfg                # Key configuration
```

---

## Usage

```bash
export JAVA_TOOL_OPTIONS='-Xverify:none -Djava.util.prefs.systemRoot=./.java -Djava.util.prefs.userRoot=./.java/.userPrefs -Djava.library.path=./'

java -jar freej2me-sdl.jar <game.jar> <width> <height> <volume>
```

**Example:**

```bash
java -jar freej2me-sdl.jar /home/games/city.jar 240 320 100
```

**Parameters:**

| Argument | Description | Example |
|----------|-------------|---------|
| `<game.jar>` | Absolute path to J2ME game JAR | `/path/to/game.jar` |
| `<width>` | Game display width (pixels) | `240` |
| `<height>` | Game display height (pixels) | `320` |
| `<volume>` | Volume level (0-100, unused in code) | `100` |

---

## Controls

### Default Button Mapping

| Phone Key | Button | Description |
|-----------|--------|-------------|
| Left Soft | Y | Left soft key |
| Right Soft | A | Right soft key |
| OK | X | Confirm key |
| 0 | B | Number 0 |
| * | SELECT | Asterisk |
| # | START | Hash |
| 1 | L1 | Number 1 |
| 3 | R1 | Number 3 |
| 7 | L2 | Number 7 |
| 9 | R2 | Number 9 |

### Hotkeys

| Combo | Action |
|-------|--------|
| **SELECT + START** | Cycle key mode (p→n→e→s→m, 5 modes) |
| **SELECT + B** | Rotate screen 90° (clockwise / counterclockwise) |
| **SELECT + Y** | Toggle touchscreen mode (white cursor, X to confirm) |
| **MENU / GUIDE** | Force quit game |
| **Left Stick Press** | D-pad becomes 2/4/6/8 |
| **Right Stick Press** | Acts as OK key |

### Text Input

- Keys **1 / 3** (default L1 / R1): Enter numbers
- **Left / Right** D-pad: Enter letters
- **\*** key (default SELECT): Delete character
- **#** key (default START): Add character

---

## Custom Configuration

### Key Mapping (`keymap.cfg`)

Edit `keymap.cfg` (JSON format):

```json
{
    "左键": "Y",
    "右键": "A",
    "OK":   "X",
    "*":    "SELECT",
    "#":    "START",
    "0":    "B",
    "1":    "L",
    "3":    "R",
    "7":    "L2",
    "9":    "R2"
}
```

### Font (`font.ttf`)

Replace `font.ttf` in the execution directory to change the in-game font.

---

## License

This project is released under the [GNU General Public License v3.0](LICENSE). Portions (ObjectWeb ASM) are covered by the **ObjectWeb ASM License**.

Portions copyright (c) 2000-2011 INRIA, France Telecom. All rights reserved.
