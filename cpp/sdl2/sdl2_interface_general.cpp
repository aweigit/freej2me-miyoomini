/*
Anbu, an interface between FreeJ2ME emulator and SDL2
Authors:
	Anbu        Saket Dandawate (hex @ retropie)
	FreeJ2ME    D. Richardson (recompile @ retropie)
	
To compile : g++ -std=c++11 -lSDL2 -lpthread -lfreeimage -o anbu anbu.cpp

This file is part of FreeJ2ME.

FreeJ2ME is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

FreeJ2ME is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/

*/

//aarch64-none-linux-gnu-g++ -std=c++11  -fPIC -O3 -fno-strict-aliasing -march=armv8-a+crc -mtune=cortex-a53 -lSDL2 -lpthread  sdl2_interface_general.cpp cJSON.c -o sdl_interface

#include <iostream>
#include <pthread.h>
#include <SDL2/SDL.h>
#include "cJSON.h"
#include <map>
#include <math.h>

#define SDL_AXIS_TRIGGERLEFT 100
#define SDL_AXIS_TRIGGERRIGHT 101

#define BYTES 2

//模拟器按键与sdl键值对应关系
int KEY_LEFT=SDLK_q;
int KEY_RIGHT=SDLK_w;
int KEY_OK=SDLK_RETURN;
int KEY_STAR=SDLK_e;
int KEY_POUND=SDLK_r;
int KEY_1=SDLK_1;
int KEY_3=SDLK_3;
int KEY_7=SDLK_7;
int KEY_9=SDLK_9;
int KEY_0=SDLK_0;

//这个地方会根据配置变化
int BUTTON_Y=KEY_LEFT;
int BUTTON_A=KEY_RIGHT;
int BUTTON_X=KEY_OK;
int BUTTON_BACK=KEY_STAR;
int BUTTON_START=KEY_POUND;
int BUTTON_LEFTSHOULDER=KEY_1;
int BUTTON_RIGHTSHOULDER=KEY_3;
int TRIGGERLEFT=KEY_7;
int TRIGGERRIGHT=KEY_9;
int BUTTON_B=KEY_0;

int joy2emu(int joy)
{
	switch(joy)
	{
		case SDL_CONTROLLER_BUTTON_Y:
			return BUTTON_X;
		case SDL_CONTROLLER_BUTTON_A:
			return BUTTON_B;
		case SDL_CONTROLLER_BUTTON_X:
			return BUTTON_Y;
		case SDL_CONTROLLER_BUTTON_BACK:
			return BUTTON_BACK;
		case SDL_CONTROLLER_BUTTON_START:
			return BUTTON_START;
		case SDL_CONTROLLER_BUTTON_LEFTSHOULDER:
			return BUTTON_LEFTSHOULDER;
		case SDL_CONTROLLER_BUTTON_RIGHTSHOULDER:
			return BUTTON_RIGHTSHOULDER;
		case SDL_AXIS_TRIGGERLEFT://注意这是自定义的摇杆的轴值，确认是否跟Button值重复
			return TRIGGERLEFT;
		case SDL_AXIS_TRIGGERRIGHT:
			return TRIGGERRIGHT;
		case SDL_CONTROLLER_BUTTON_B:
			return BUTTON_A;
		case SDL_CONTROLLER_BUTTON_GUIDE:
			return SDLK_BACKSPACE;
	}
	
	return 0;
}

pthread_t t_capturing;


//原始游戏画面大小
int source_width = 0, source_height = 0;
//掌机的屏幕大小
int display_width = 640, display_height = 480;

unsigned int last_time = 0;

bool capturing = true;
int rotate=0;
int overlay_scale=2;

SDL_Renderer *mRenderer;
SDL_Texture *mTexture;
SDL_Texture *romTexture;
SDL_Texture *mBackground;
SDL_Texture *mOverlay;
SDL_Window *mWindow;

SDL_Joystick *g_joystick=NULL;


short joymouseX = 0;
short joymouseY = 0;
bool use_mouse = 0;
// mouse cursor image
unsigned char joymouseImage[374] =
{
	0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,0,0,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,0,0,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,0,0,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,1,1,0,0,0,0,0,0,0,
	0,0,0,0,1,2,2,1,2,2,1,1,1,0,0,0,0,
	0,0,0,0,1,2,2,1,2,2,1,2,2,1,1,0,0,
	0,0,0,0,1,2,2,1,2,2,1,2,2,1,2,1,0,
	1,1,1,0,1,2,2,1,2,2,1,2,2,1,2,2,1,
	1,2,2,1,1,2,2,2,2,2,2,2,2,1,2,2,1,
	1,2,2,2,1,2,2,2,2,2,2,2,2,2,2,2,1,
	0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,1,
	0,0,0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,
	0,0,0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,
	0,0,0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,
	0,0,0,0,0,1,2,2,2,2,2,2,2,2,1,0,0,
	0,0,0,0,0,1,2,2,2,2,2,2,2,2,1,0,0,
	0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,0,0
};

uint32_t frameDeadline = 0;

#define STICK_DEAD_ZONE 8000  // 死区阈值，可调
#define TRIGGER_PRESS_THRESHOLD   8000  // 按下阈值
#define TRIGGER_RELEASE_THRESHOLD 2000  // 释放阈值（必须 <= PRESS）
// 配置参数（可调整）
#define DEAD_ZONE      0.15f   // 摇杆死区（0.0 ～ 1.0）
#define SENSITIVITY    4.0f    // 每帧最大移动像素（越高越快）
#define USE_RADIAL_DZ  1       // 1 = 径向死区，0 = 轴向死区
static bool left_trigger_active  = false;
static bool right_trigger_active = false;

// 当前方向状态（避免重复打印/触发）
static bool up    = false;
static bool down  = false;
static bool left  = false;
static bool right = false;

// 存储当前轴值（用于帧更新或事件驱动）
static Sint16 left_x = 0;
static Sint16 left_y = 0;

//SDL_CONTROLLER_BUTTON_LEFTSTICK / RIGHTSTICK（按下摇杆）
//SDL_CONTROLLER_AXIS_TRIGGERLEFT / RIGHT L2、R2为扳机键，与摇杆相同，但是只有正值
//SDL_CONTROLLER_BUTTON_DPAD_UP 方向键

// 用于保存已连接的手柄
std::map<SDL_JoystickID, SDL_GameController*> g_gameControllers;

// 归一化单轴（仅用于轴向死区）
static float normalize_axis(Sint16 axis, float deadzone) {
    float val = axis / 32768.0f;
    if (fabsf(val) < deadzone) return 0.0f;
    float sign = (val > 0) ? 1.0f : -1.0f;
    return sign * ((fabsf(val) - deadzone) / (1.0f - deadzone));
}

void updateMouse_xy()
{
	// === 摇杆输入处理 ===
	Sint16 raw_x = SDL_GameControllerGetAxis(g_gameControllers[0], SDL_CONTROLLER_AXIS_LEFTX);
	Sint16 raw_y = SDL_GameControllerGetAxis(g_gameControllers[0], SDL_CONTROLLER_AXIS_LEFTY);

	float nx, ny;

#if USE_RADIAL_DZ
	// --- 径向死区（推荐用于 2D 移动）---
	float fx = raw_x / 32768.0f;
	float fy = raw_y / 32768.0f;
	float magnitude = sqrtf(fx * fx + fy * fy);

	if (magnitude < DEAD_ZONE) {
		nx = ny = 0.0f;
	} else {
		// 重映射有效区域到 [0, 1]
		float scale = (magnitude - DEAD_ZONE) / (1.0f - DEAD_ZONE);
		// 保持方向，缩放幅度
		if (magnitude > 0) {
			nx = (fx / magnitude) * scale;
			ny = (fy / magnitude) * scale;
		} else {
			nx = ny = 0.0f;
		}
	}
#else
	// --- 轴向死区 ---
	nx = normalize_axis(raw_x, DEAD_ZONE);
	ny = normalize_axis(raw_y, DEAD_ZONE);
#endif

	// 应用灵敏度（注意：Y 轴反向，因为屏幕坐标向下为正）
	joymouseX += nx * SENSITIVITY;
	joymouseY += ny * SENSITIVITY;  // 减号：上推摇杆 → 鼠标向上（Y 减小）

	// === 边界检测：限制在窗口内 ===
	if (joymouseX < 0) joymouseX = 0;
	if (joymouseY < 0) joymouseY = 0;
	if (joymouseX >= source_width-8) joymouseX = source_width-8;
	if (joymouseY >= source_height-8) joymouseY = source_height-8;
}

void updateMouse(int direct)
{
	switch(direct)
	{
		case SDLK_UP:
			if(joymouseY<6)
			{
				joymouseY=0;
			}
			else
			{
				joymouseY-=6;
			}
			break;
		case SDLK_DOWN:
			if(joymouseY+6>=source_height-11)
			{
				joymouseY=source_height-11;
			}
			else
			{
				joymouseY+=6;
			}
			break;
		case SDLK_LEFT:
			if(joymouseX<6)
			{
				joymouseX=0;
			}
			else
			{
				joymouseX-=6;
			}
			break;
		case SDLK_RIGHT:
			if(joymouseX+6>=source_width-8)
			{
				joymouseX=source_width-8;
			}
			else
			{
				joymouseX+=6;
			}
			break;
		
	}
}

void sendKey(int key, bool pressed)
{
	unsigned char bytes [5];
	bytes[0] = (char) (0x00 | pressed );

	bytes[1] = (char) (key >> 24 & 0xFF);
	bytes[2] = (char) (key >> 16 & 0xFF);
	bytes[3] = (char) (key >> 8 & 0xFF);
	bytes[4] = (char) (key & 0xFF);
	
	//std::cout<<"sendkey:"<<key<<" pressed:"<<pressed<<std::endl;
	
	fwrite(&bytes, sizeof(char), 5, stderr);

	fflush(stderr);
}

void sendKey_Mouse(bool pressed)
{
	unsigned char bytes [5];
	bytes[0] = (char) (0x10 | pressed );

	bytes[1] = (char) (joymouseX >> 8 & 0xFF);
	bytes[2] = (char) (joymouseX & 0xFF);
	bytes[3] = (char) (joymouseY >> 8 & 0xFF);
	bytes[4] = (char) (joymouseY & 0xFF);
	
	fwrite(&bytes, sizeof(char), 5, stderr);

	fflush(stderr);
}

void sendKey_Direct(int key , bool pressed)
{
	int newkey=0;
	switch (key)
	{
		case SDL_CONTROLLER_BUTTON_DPAD_UP:
			newkey = (rotate ==0 ? SDLK_UP : (rotate == 2 ? SDLK_RIGHT : SDLK_LEFT ));
			break;
		case SDL_CONTROLLER_BUTTON_DPAD_DOWN:
			newkey = (rotate ==0 ? SDLK_DOWN : (rotate == 2 ? SDLK_LEFT : SDLK_RIGHT));
			break;
		case SDL_CONTROLLER_BUTTON_DPAD_LEFT:
			newkey = (rotate ==0 ? SDLK_LEFT : (rotate == 2 ? SDLK_UP : SDLK_DOWN));
			break;
		case SDL_CONTROLLER_BUTTON_DPAD_RIGHT:
			newkey = (rotate ==0 ? SDLK_RIGHT : (rotate == 2 ? SDLK_DOWN : SDLK_UP));
			break;
		default:
			return;
	}

	if(use_mouse)
	{
		updateMouse(newkey);
	}
	else
	{
		sendKey(newkey, pressed);
	}
}

void sendKey_Axis(int key , bool pressed)
{
	int newkey=0;
	switch (key)
	{
		case SDLK_UP:
			newkey = (rotate ==0 ? SDLK_UP : (rotate == 2 ? SDLK_RIGHT : SDLK_LEFT));
			break;
		case SDLK_DOWN:
			newkey = (rotate ==0 ? SDLK_DOWN : (rotate == 2 ? SDLK_LEFT : SDLK_RIGHT));
			break;
		case SDLK_LEFT:
			newkey = (rotate ==0 ? SDLK_LEFT : (rotate == 2 ? SDLK_UP : SDLK_DOWN));
			break;
		case SDLK_RIGHT:
			newkey = (rotate ==0 ? SDLK_RIGHT : (rotate == 2 ? SDLK_DOWN : SDLK_UP ));
			break;
		default:
			return;
	}

	if(!use_mouse){
		sendKey(newkey, pressed);
	}
}

void update_direction_from_stick() {
    bool new_up    = (left_y < -STICK_DEAD_ZONE);
    bool new_down  = (left_y >  STICK_DEAD_ZONE);
    bool new_left  = (left_x < -STICK_DEAD_ZONE);
    bool new_right = (left_x >  STICK_DEAD_ZONE);

    // 检测“按下”（从 false → true）
    if (new_up && !up){
		printf("UP pressed\n");
		sendKey_Axis(SDLK_UP, true);
	}    
    if (new_down && !down){
		printf("DOWN pressed\n");
		sendKey_Axis(SDLK_DOWN, true);
	} 
    if (new_left && !left){
		printf("LEFT pressed\n");
		sendKey_Axis(SDLK_LEFT, true);
	} 
    if (new_right && !right)
	{
		printf("RIGHT pressed\n");
		sendKey_Axis(SDLK_RIGHT, true);
	} 

    // 检测“释放”（从 true → false）
    if (!new_up && up){
		printf("UP released\n");
		sendKey_Axis(SDLK_UP, false);
	}    
    if (!new_down && down){
		printf("DOWN released\n");
		sendKey_Axis(SDLK_DOWN, false);
	} 
    if (!new_left && left){
		printf("LEFT released\n");
		sendKey_Axis(SDLK_LEFT, false);
	} 
    if (!new_right && right){
		printf("RIGHT released\n");
		sendKey_Axis(SDLK_RIGHT, false);
	} 

    // 更新状态
    up    = new_up;
    down  = new_down;
    left  = new_left;
    right = new_right;
}


// Limit FPS to avoid high CPU load, use when v-sync isn't available
void LimitFrameRate()
{
    const int refreshDelay = 16666;//1000000 / 60;
    uint32_t tc = SDL_GetTicks() * 1000;
    uint32_t v = 0;
    if (frameDeadline > tc)
    {
        v = tc % refreshDelay;
        SDL_Delay(v / 1000 + 1); // ceil
    }
    frameDeadline = tc + v + refreshDelay;
}

bool sendQuitEvent()
{
	SDL_Event* quit = new SDL_Event();
	quit->type = SDL_QUIT;
	SDL_PushEvent(quit);
	return true;
}


//实际显示区域
SDL_Rect getDestinationRect(int source_width,int source_height)
{
	double scale= std::min( (double) display_width/source_width, (double) display_height/source_height );

	int w = source_width * scale, h = source_height * scale;
	return { (display_width - w )/2, (display_height - h)/2, w, h };
}

bool updateFrame(size_t num_chars, unsigned char* buffer, FILE* input = stdin)
{
	int read_count = fread(buffer, sizeof(char), num_chars, input);
	return read_count == num_chars;
}

void rotate_image(const unsigned char *src, unsigned char *dst, 
                  int width, int height, int angle)
{
    int x, y;
    int src_x, src_y;
    int src_idx, dst_idx;
    
    switch (angle) {
        case 0:
            memcpy(dst, src, width * height * BYTES);
            break;
            
        case 1:  // 顺时针90°
            for (y = 0; y < width; y++) {
                for (x = 0; x < height; x++) {
                    src_x = y;
                    src_y = height - 1 - x;
                    src_idx = (src_y * width + src_x) * BYTES;
                    dst_idx = (y * height + x) * BYTES;
                    memcpy(dst + dst_idx, src + src_idx, BYTES);
                }
            }
            break;
          
        case 2:  // 逆时针90°
            for (y = 0; y < width; y++) {
                for (x = 0; x < height; x++) {
                    src_x = width - 1 - y;
                    src_y = x;
                    src_idx = (src_y * width + src_x) * BYTES;
                    dst_idx = (y * height + x) * BYTES;
                    memcpy(dst + dst_idx, src + src_idx, BYTES);
                }
            }
            break;
        
        case 3: //180
            for (y = 0; y < height; y++) {
                for (x = 0; x < width; x++) {
                    src_x = width - 1 - x;
                    src_y = height - 1 - y;
                    src_idx = (src_y * width + src_x) * BYTES;
                    dst_idx = (y * width + x) * BYTES;
                    memcpy(dst + dst_idx, src + src_idx, BYTES);
                }
            }
            break;
    }
}


void drawFrame(unsigned char *frame, SDL_Texture *mTexture,size_t pitch, SDL_Rect *dest, int interFrame = 16)
{
	//Cutoff rendering at 60fps，毫秒
	// if (SDL_GetTicks() - last_time < interFrame) {
	// 	return;
	// }

	//last_time = SDL_GetTicks();

	SDL_RenderClear(mRenderer);
	//SDL_RenderCopy(mRenderer, mBackground, NULL, NULL);
	
	SDL_UpdateTexture(mTexture, NULL, frame, pitch);
	SDL_RenderCopy(mRenderer, mTexture, NULL, dest);
	
	//SDL_RenderCopy(mRenderer, mOverlay, NULL, dest);

	//更新到屏幕
	SDL_RenderPresent(mRenderer);
	
}

void AddController(int joystick_index) {
    if (SDL_IsGameController(joystick_index)) {
        SDL_GameController* gc = SDL_GameControllerOpen(joystick_index);
        if (gc) {
            SDL_JoystickID instance_id = SDL_JoystickInstanceID(SDL_GameControllerGetJoystick(gc));
            g_gameControllers[instance_id] = gc;
            std::cout << "手柄已连接: " << SDL_GameControllerName(gc) << " (ID: " << instance_id << ")\n";
        } else {
            std::cerr << "无法打开手柄: " << SDL_GetError() << "\n";
        }
    } else {
        std::cout << "设备 " << joystick_index << " 不是有效的 Game Controller\n";
    }
}

void RemoveController(SDL_JoystickID instance_id) {
    auto it = g_gameControllers.find(instance_id);
    if (it != g_gameControllers.end()) {
        SDL_GameControllerClose(it->second);
        g_gameControllers.erase(it);
        std::cout << "手柄已断开 (ID: " << instance_id << ")\n";
    }
}

/******************************************************** Processing Function */
void init()
{
	if (source_width == 0 || source_height == 0)
	{
		std::cout << "anbu: Neither width nor height parameters can be 0." << std::endl;
		exit(0);
	}

	if(SDL_Init(SDL_INIT_VIDEO | SDL_INIT_GAMECONTROLLER) < 0) 
	{
		std::cout<<"SDL无法初始化! SDL_Error: "<<SDL_GetError()<<std::endl;
		exit(0);
	}
	
	// if (SDL_NumJoysticks() >= 1)
	// {
	// 	g_joystick = SDL_JoystickOpen(0);
	// 	if (g_joystick == NULL)
	// 	{
	// 		std::cout<<"Unable to open joystick."<<std::endl;
	// 		exit(0);
	// 	}
	// }

	SDL_GameControllerEventState(SDL_ENABLE);

    // 检测启动时已连接的手柄
    int num_joysticks = SDL_NumJoysticks();
    for (int i = 0; i < num_joysticks; ++i) {
		// 获取 GUID
        SDL_JoystickGUID guid = SDL_JoystickGetDeviceGUID(i);
        
        // 转为字符串（格式：00000000000000000000000000000000）
        char guid_str[33];
        SDL_JoystickGetGUIDString(guid, guid_str, sizeof(guid_str));

        const char* name = SDL_JoystickNameForIndex(i);
        printf("Joystick %d: %s\n", i, name);
        printf("  GUID: %s\n", guid_str);
		
        AddController(i);
    }


	SDL_SetHint(SDL_HINT_RENDER_SCALE_QUALITY, "best");

	int window_flag = SDL_WINDOW_OPENGL | SDL_WINDOW_SHOWN;
	
	int window_x = SDL_WINDOWPOS_UNDEFINED, window_y = SDL_WINDOWPOS_UNDEFINED;

	try{
		mWindow = SDL_CreateWindow(NULL, window_x, window_y, display_width, display_height, window_flag);
		
	}
	catch(std::exception& e)
	{
		std::cout << "SDL_CreateWindow err:"<<e.what() << std::endl;
		exit(-1);
	}
	
	if(mWindow == NULL) {
        std::cout<<"Could not create window: "<<SDL_GetError()<<std::endl;
        exit(-1);
    }
	SDL_SetHint(SDL_HINT_RENDER_VSYNC, "1"); 

	Uint32 render_flag = SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC;

    mRenderer = SDL_CreateRenderer(mWindow, -1, render_flag);
	
    SDL_RenderSetLogicalSize(mRenderer, display_width, display_height);
    SDL_SetRenderDrawColor(mRenderer, 0, 0, 0, 255);
	
	SDL_RenderClear(mRenderer);
	SDL_RenderPresent(mRenderer);
	
}

void loadBackground()
{	
	SDL_Surface *bmp = NULL;
	bmp = SDL_LoadBMP("bg.bmp");
    if (bmp == NULL) {
        std::cout<<"SDL_LoadBMP failed: "<<SDL_GetError()<<std::endl;
    }
	
    mBackground = SDL_CreateTextureFromSurface(mRenderer, bmp);
	
    SDL_FreeSurface(bmp);
}

void loadOverlay(SDL_Rect &rect)
{
	int psize =  overlay_scale * rect.w / source_width;
	int size = rect.w * rect.h * 4;
	unsigned char *bytes = new unsigned char[size];

	for (int h = 0; h < rect.h; h++)
		for (int w = 0; w < rect.w; w++)
		{
			int c = (h * rect.w + w) * 4;
			bytes[c+3] = 64;
			bytes[c+2] = 64;
			bytes[c+1] = 64;
			bytes[c] = w % psize == 0 || h % psize == 0 ? 32 : 0;
		}

	mOverlay = SDL_CreateTexture(mRenderer, SDL_PIXELFORMAT_RGBA8888, SDL_TEXTUREACCESS_STATIC, rect.w, rect.h);
	SDL_SetTextureBlendMode(mOverlay, SDL_BLENDMODE_BLEND);
	SDL_UpdateTexture(mOverlay, NULL, bytes, rect.w * sizeof(unsigned char) * 4);
	delete[] bytes;
}


void startStreaming()
{
	size_t pitch,ropitch;
	SDL_Rect dest,rodest;
	
	dest= getDestinationRect(source_width,source_height);
	pitch= source_width * sizeof(char) * BYTES;
	//纹理大小是jar游戏大小
	mTexture = SDL_CreateTexture(mRenderer, SDL_PIXELFORMAT_RGB565, SDL_TEXTUREACCESS_STREAMING, source_width, source_height);
	
	rodest= getDestinationRect(source_height,source_width);
	ropitch= source_height * sizeof(char) * BYTES;
	romTexture = SDL_CreateTexture(mRenderer, SDL_PIXELFORMAT_RGB565, SDL_TEXTUREACCESS_STREAMING,  source_height,source_width);

	//loadBackground();
	//loadOverlay(dest);
	
	size_t num_chars = source_width * source_height * BYTES;
	unsigned char* frame = new unsigned char[num_chars];
	unsigned char* tmp_frame = new unsigned char[num_chars];
	
	//从输入流里读取到frame
	while (capturing && updateFrame(num_chars, frame) || !sendQuitEvent())
	{
		if(rotate==0)
		{
			
			if(use_mouse)
			{
				int t=0;
				for(int i=0; i<22; i++)
				{
					for (int j=0; j<17; j++)
					{
						if((joymouseX+j>=source_width) || (joymouseY+i>=source_height))
						{
							continue;
						}
						t = ((joymouseY + i)*source_width+(joymouseX + j))*BYTES;
						
						switch (joymouseImage[(i*17)+j])
						{
							case 1: 
								frame[t] = 0x00; 
								frame[t+1] = 0x00;
								// frame[t+2] = 0x00;
								// frame[t+3] = 0x00;
								break;
							case 2: 
								frame[t] = 0xFF; 
								frame[t+1] = 0xFF;
								// frame[t+2] = 0xFF; 
								// frame[t+3] = 0xFF; 
								break;
						}
						
					}
				}
			}
			
			drawFrame(frame, mTexture, pitch, &dest);
		}
        else
        {
            rotate_image(frame,tmp_frame,source_width, source_height,rotate);
			drawFrame(tmp_frame, romTexture, ropitch, &rodest);
        }


		
	}

	if(g_joystick)
	{
		SDL_JoystickClose(g_joystick);
	}
	SDL_DestroyTexture(mTexture);
	SDL_DestroyTexture(romTexture);
	//SDL_DestroyTexture(mBackground);
	//SDL_DestroyTexture(mOverlay);
	
	SDL_DestroyRenderer(mRenderer);
	
    SDL_DestroyWindow(mWindow);
	delete[] frame;
	delete[] tmp_frame;
}

void *startCapturing(void *args)
{
	int mod=0;
	int ignore=0;
	SDL_Event e;
	//int isFirst=1;
	while (capturing)
	{
		//if (SDL_WaitEvent(&e))
		while(SDL_PollEvent(&e))
		{
			switch (e.type)
			{
				case SDL_QUIT:
					capturing = false;
					sendKey(-1, true);
					//continue;
					break;
				case SDL_CONTROLLERDEVICEADDED:
					//e.cdevice.which 在 CONTROLLERDEVICEADDED/REMOVED 事件中 就是 Instance ID（不是 device index)
					//实际上：SDL 允许将 instance ID 作为 "virtual index" 传入 SDL_GameControllerOpen
					//这是 SDL2 的特殊行为：支持用 instance ID 打开
                    std::cout << "检测到新手柄插入\n";
					//先判断是否已经打开了
					if (g_gameControllers.find(e.cdevice.which) == g_gameControllers.end()) {
						AddController(e.cdevice.which);
					}
                    
                    break;

                case SDL_CONTROLLERDEVICEREMOVED:
                    std::cout << "手柄被拔出\n";
                    RemoveController(e.cdevice.which);
                    break;
				
				case SDL_KEYDOWN:
				case SDL_KEYUP:
				{
					// printf("keycode: 0x%x name: %s state: %d\n",event.key.keysym.sym,SDL_GetKeyName(event.key.keysym.sym),event.key.state);
					// fflush(stdout);
					
					// int key = e.key.keysym.sym;
					// if(key==SDLK_UP)//上
					// {
					// 	if(rotate==1)
					// 	{
					// 		key=SDLK_RIGHT;
					// 	}
					// 	else if(rotate==2)
					// 	{
					// 		key=SDLK_LEFT;
					// 	}
						
					// 	if(use_mouse && e.key.state == SDL_PRESSED)
					// 	{
					// 		updateMouse(key);
					// 	}
						
					// }
					// else if(key==SDLK_DOWN)//下
					// {
					// 	if(rotate==1)
					// 	{
					// 		key=SDLK_LEFT;
					// 	}
					// 	else if(rotate==2)
					// 	{
					// 		key=SDLK_RIGHT;
					// 	}
					// 	if(use_mouse && e.key.state == SDL_PRESSED)
					// 	{
					// 		updateMouse(key);
					// 	}
					// }
					// else if(key==SDLK_LEFT) //左
					// {
					// 	if(rotate==1)
					// 	{
					// 		key=SDLK_UP;
					// 	}
					// 	else if(rotate==2)
					// 	{
					// 		key=SDLK_DOWN;
					// 	}
					// 	if(use_mouse && e.key.state == SDL_PRESSED)
					// 	{
					// 		updateMouse(key);
					// 	}
					// }
					// else if(key==SDLK_RIGHT) //右
					// {
					// 	if(rotate==1)
					// 	{
					// 		key=SDLK_DOWN;
					// 	}
					// 	else if(rotate==2)
					// 	{
					// 		key=SDLK_UP;
					// 	}
					// 	if(use_mouse && e.key.state == SDL_PRESSED)
					// 	{
					// 		updateMouse(key);
					// 	}
					// }
					
					// if(!use_mouse)
					// {
					// 	sendKey(key, e.key.state == SDL_PRESSED);
					// }
				}
				break;

				case SDL_CONTROLLERAXISMOTION: {
                    Sint16 value = e.caxis.value;
					const char* axis_name = "";
					
					switch (e.caxis.axis) {
						case SDL_CONTROLLER_AXIS_LEFTX://左摇杆用于控制方向
							left_x = value;
							update_direction_from_stick();
							axis_name = "Left Stick X";
							break;
						case SDL_CONTROLLER_AXIS_LEFTY:
							left_y = value;
							update_direction_from_stick();
							axis_name = "Left Stick Y";
							break;
						case SDL_CONTROLLER_AXIS_RIGHTX:
							axis_name = "Right Stick X";
							break;
						case SDL_CONTROLLER_AXIS_RIGHTY:
							axis_name = "Right Stick Y";
							break;
						case SDL_CONTROLLER_AXIS_TRIGGERLEFT:
						{
							if (!left_trigger_active && value > TRIGGER_PRESS_THRESHOLD) {
								left_trigger_active = true;
								printf("Left Trigger PRESSED (value=%d)\n", value);
								// 在这里执行“按下”逻辑
								sendKey(SDL_AXIS_TRIGGERLEFT,true);
							}
							else if (left_trigger_active && value < TRIGGER_RELEASE_THRESHOLD) {
								left_trigger_active = false;
								printf("Left Trigger RELEASED (value=%d)\n", value);
								// 在这里执行“释放”逻辑
								sendKey(SDL_AXIS_TRIGGERLEFT,false);
							}
						
							axis_name = "Left Trigger";
							break;
						}
						case SDL_CONTROLLER_AXIS_TRIGGERRIGHT:
						{
							if (!right_trigger_active && value > TRIGGER_PRESS_THRESHOLD) {
								right_trigger_active = true;
								printf("Right Trigger PRESSED (value=%d)\n", value);
								sendKey(SDL_AXIS_TRIGGERRIGHT,true);
							}
							else if (right_trigger_active && value < TRIGGER_RELEASE_THRESHOLD) {
								right_trigger_active = false;
								printf("Right Trigger RELEASED (value=%d)\n", value);
								sendKey(SDL_AXIS_TRIGGERRIGHT,false);
							}
						
							axis_name = "Right Trigger";
							break;
						}
						default:
							axis_name = "Unknown Axis";
					}
					
					//printf("axis_name: %s value:%d\n", axis_name, e.caxis.value);
					
					break;
                }
				

                case SDL_CONTROLLERBUTTONDOWN:
                case SDL_CONTROLLERBUTTONUP: {
					const char* button_name = "";
                    SDL_GameControllerButton button = static_cast<SDL_GameControllerButton>(e.cbutton.button);
                    const char* state = (e.cbutton.state == SDL_PRESSED) ? " 按下 " : " 释放 ";
                    
					int key=0;
					switch (e.cbutton.button) {
						case SDL_CONTROLLER_BUTTON_A:
							button_name="SDL_CONTROLLER_BUTTON_A";
							break;
						case SDL_CONTROLLER_BUTTON_B:
							button_name="SDL_CONTROLLER_BUTTON_B";
							break;
						case SDL_CONTROLLER_BUTTON_X:
							button_name="SDL_CONTROLLER_BUTTON_X";
							break;
						case SDL_CONTROLLER_BUTTON_Y://在鼠标模式下模拟鼠标点击
							button_name="SDL_CONTROLLER_BUTTON_Y";
							if(use_mouse){
								sendKey_Mouse(e.cbutton.state == SDL_PRESSED);
								e.cbutton.button=0;
							}
							break;
						case SDL_CONTROLLER_BUTTON_BACK://select
							button_name="SDL_CONTROLLER_BUTTON_BACK";
							break;
						case SDL_CONTROLLER_BUTTON_START://start
							button_name="SDL_CONTROLLER_BUTTON_START";
							break;
						case SDL_CONTROLLER_BUTTON_LEFTSHOULDER://L1
							button_name="SDL_CONTROLLER_BUTTON_LEFTSHOULDER";
							break;
						case SDL_CONTROLLER_BUTTON_RIGHTSHOULDER://R1
							button_name="SDL_CONTROLLER_BUTTON_RIGHTSHOULDER";
							break;
						case SDL_CONTROLLER_BUTTON_LEFTSTICK://左摇杆按下模拟鼠标点击
							button_name="SDL_CONTROLLER_BUTTON_LEFTSTICK";
							sendKey_Mouse(e.cbutton.state == SDL_PRESSED);
							break;
						case SDL_CONTROLLER_BUTTON_RIGHTSTICK://右摇杆按下退出
							button_name="SDL_CONTROLLER_BUTTON_RIGHTSTICK";
							capturing = false;
							sendKey(-1, e.cbutton.state == SDL_PRESSED);
							break;
						case SDL_CONTROLLER_BUTTON_DPAD_UP:
						case SDL_CONTROLLER_BUTTON_DPAD_DOWN:
						case SDL_CONTROLLER_BUTTON_DPAD_LEFT:
						case SDL_CONTROLLER_BUTTON_DPAD_RIGHT:
							sendKey_Direct(e.cbutton.button, e.cbutton.state == SDL_PRESSED);
							break;
						case SDL_CONTROLLER_BUTTON_GUIDE:
							button_name="SDL_CONTROLLER_BUTTON_GUIDE";
							capturing = false;
							sendKey(-1, e.cbutton.state == SDL_PRESSED);
							break;
						default:
							button_name = "Unknown button_name";
							capturing = false;
							sendKey(-1, e.cbutton.state == SDL_PRESSED);
							break;	
						
					}
					//std::cout << "按钮 [" << static_cast<int>(button) << "] " << button_name << state << "\n";
					//按住select键
					if(e.cbutton.button == SDL_CONTROLLER_BUTTON_BACK && e.cbutton.state == SDL_PRESSED){mod=1;}
					else if(e.cbutton.button == SDL_CONTROLLER_BUTTON_BACK && e.cbutton.state == SDL_RELEASED){mod=0;}

					key = joy2emu(e.cbutton.button);

					if(mod)
					{
						switch (e.cbutton.button) {
							case SDL_CONTROLLER_BUTTON_X://切换鼠标模式
								mod=0;
								use_mouse=1-use_mouse;//切换鼠标
								key=0;
								break;
							case SDL_CONTROLLER_BUTTON_A://旋转
								mod=0;
								rotate=(1+rotate)%3;//连续旋转
								key=0;
								break;
							case SDL_CONTROLLER_BUTTON_START://切换按键输入模式
								mod=0;
								key=SDLK_c;//英文c,发送到java,作为切换按键模式的信号
								break;
						}
					}

					if(key && !use_mouse)
					{
						sendKey(key, e.cbutton.state == SDL_PRESSED);
					}

					break;
                }
				
				case SDL_JOYAXISMOTION:
				case SDL_JOYHATMOTION:
				case SDL_JOYBUTTONDOWN:
    		    case SDL_JOYBUTTONUP:
					break;
				
				default:
					printf("未知事件:event type 0x%x \n",e.type);
					fflush(stdout);
				
				
			}//switch
		}//while
		
		//tick();
		if(use_mouse)
		{
			updateMouse_xy();
		}
		
		LimitFrameRate();
		
	}//while
	fflush(stderr);
	pthread_exit(NULL);
}

void setEmuKey(const char * name, int key)//再加个参数为模拟器需要的键值，如左键key_left
{
	if(strcmp(name,"X")==0)
	{
		BUTTON_X=key;
	}
	else if(strcmp(name,"B")==0)
	{
		BUTTON_B=key;
	}
	else if(strcmp(name,"A")==0)
	{
		BUTTON_A=key;
	}
	else if(strcmp(name,"SELECT")==0)
	{
		BUTTON_BACK=key;
	}
	else if(strcmp(name,"START")==0)
	{
		BUTTON_START=key;
	}
	else if(strcmp(name,"Y")==0)
	{
		BUTTON_Y=key;
	}
	else if(strcmp(name,"L")==0)
	{
		BUTTON_LEFTSHOULDER=key;
	}
	else if(strcmp(name,"R")==0)
	{
		BUTTON_RIGHTSHOULDER=key;
	}
	else if(strcmp(name,"L2")==0)
	{
		TRIGGERLEFT=key;
	}
	else if(strcmp(name,"R2")==0)
	{
		TRIGGERRIGHT=key;
	}
	
}

void defaultKeymap()
{
    //默认映射
    BUTTON_Y=KEY_LEFT;
    BUTTON_A=KEY_RIGHT;
    BUTTON_X=KEY_OK;
    BUTTON_BACK=KEY_STAR;
    BUTTON_START=KEY_POUND;
    BUTTON_LEFTSHOULDER=KEY_1;
    BUTTON_RIGHTSHOULDER=KEY_3;
    TRIGGERLEFT=KEY_7;
    TRIGGERRIGHT=KEY_9;
    BUTTON_B=KEY_0;
}

int loadConfig() {
    // 打开文件
    FILE *file = fopen("keymap.cfg", "r");
    if (file == NULL) {
		std::cout  << "打开文件失败" << std::endl;
        return -1;
    }
 
    // 确定文件长度
    fseek(file, 0, SEEK_END);
    long length = ftell(file);
    fseek(file, 0, SEEK_SET);
 
    // 读取文件内容到字符串
    char *data = (char*)malloc(length + 1);
    fread(data, 1, length, file);
    data[length] = '\0';
    fclose(file);
 
    // 解析JSON字符串
    cJSON *json = cJSON_Parse(data);
    if (json == NULL) {
        const char *error_ptr = cJSON_GetErrorPtr();
        if (error_ptr != NULL) {
            std::cout  << "解析json错误:" << error_ptr << std::endl;
        }
        cJSON_Delete(json);
        free(data);
        return -1;
    }
	
	try{
		// 使用cJSON对象
		cJSON *name = cJSON_GetObjectItem(json, "左键");
		setEmuKey(name->valuestring, KEY_LEFT);
		
		name = cJSON_GetObjectItem(json, "右键");
		setEmuKey(name->valuestring, KEY_RIGHT);
		
		name = cJSON_GetObjectItem(json, "OK");
		setEmuKey(name->valuestring, KEY_OK);
		
		name = cJSON_GetObjectItem(json, "*");
		setEmuKey(name->valuestring, KEY_STAR);
		
		name = cJSON_GetObjectItem(json, "#");
		setEmuKey(name->valuestring, KEY_POUND);
		
		name = cJSON_GetObjectItem(json, "0");
		setEmuKey(name->valuestring, KEY_0);
		
		name = cJSON_GetObjectItem(json, "1");
		setEmuKey(name->valuestring, KEY_1);
		
		name = cJSON_GetObjectItem(json, "3");
		setEmuKey(name->valuestring, KEY_3);
		
		name = cJSON_GetObjectItem(json, "7");
		setEmuKey(name->valuestring, KEY_7);
		
		name = cJSON_GetObjectItem(json, "9");
		setEmuKey(name->valuestring, KEY_9);
	}
	catch(std::exception& e)
	{
		
		defaultKeymap();
		
		std::cout << "解析json出错:"<<e.what() << std::endl;
		return -1;
	}
 
 
    // 清理工作
    cJSON_Delete(json);
    free(data);
 
    return 0;
}


/*********************************************************************** Main */
int main(int argc, char* argv[])
{
	if(argc==3)
	{
		source_width = atoi(argv[1]);
		source_height = atoi(argv[2]);
	}
	else{
		exit(0);
	}
	
	loadConfig();
	init();
	SDL_ShowCursor(false);
	
	if (pthread_create(&t_capturing, 0, &startCapturing, NULL))
	{
		std::cout << "Unable to start thread, exiting ..." << std::endl;
		SDL_Quit();
		return 1;
	}

	startStreaming();
	pthread_join(t_capturing, NULL);
	
	SDL_Quit();
	return 0;
}
