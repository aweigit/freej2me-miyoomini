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

#include <iostream>
#include <pthread.h>
#include <SDL2/SDL.h>
#include "cJSON.h"

#define BYTES 2


#define M_B 0
#define M_A 1
#define M_Y 2
#define M_X 3
#define M_L 4
#define M_R 5
#define M_select 6
#define M_start 7

#define M_L2 20
#define M_R2 21
 
#define M_UP 13
#define M_DOWN 14
#define M_LEFT 15
#define M_RIGHT 16

#define M_MEMU 8
#define M_QUIT1 9
#define M_QUIT2 10



int KEY_LEFT=M_Y;
int KEY_RIGHT=M_A;
int KEY_OK=M_X;
int KEY_STAR=M_select;
int KEY_POUND=M_start;
int KEY_1=M_L;
int KEY_3=M_R;
int KEY_7=M_L2;
int KEY_9=M_R2;
int KEY_0=M_B;

pthread_t t_capturing;

int angle = 0;
//原始游戏画面大小
int source_width = 0, source_height = 0;
//miyoo的屏幕大小
int display_width = 1024, display_height = 768;


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

#define KEYHOLD_TIMER_FIRST   12
#define KEYHOLD_TIMER         3

#define REPEAT_INTERVAL 200 // 毫秒，按键重复间隔
#define SDL_JOYSTICK_BUTTON_MAX 20  
  
typedef struct {  
    bool isPressed;  
    //uint32_t m_timer; // 帧为单位  
} JoyButtonState;  
  
// 假设你有足够的JoyButtonState数组来追踪所有按钮  
JoyButtonState joyButtons[SDL_JOYSTICK_BUTTON_MAX];  
  
// 初始化所有按钮为未按下状态  
void initButtonStates() {  
    for (int i = 0; i < SDL_JOYSTICK_BUTTON_MAX; ++i) {  
        joyButtons[i].isPressed = false;  
        //joyButtons[i].m_timer = 0;  
    }  
}  	

uint32_t frameDeadline = 0;
uint32_t m_timer=0;
uint32_t j_btn=0;

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

uint32_t btn2keycode(uint32_t btn)
{
	switch(btn)
	{
		case M_UP:
			return SDLK_UP;
		case M_DOWN:
			return SDLK_DOWN;
		case M_LEFT:
			return SDLK_LEFT;
		case M_RIGHT:
			return SDLK_RIGHT;
	}
	return 0;
}

void tick()
{
	if(use_mouse && j_btn>=M_UP && j_btn<=M_RIGHT)
	{
		const bool held = joyButtons[j_btn].isPressed;
		if (held)
		{
			if (m_timer)
			{
				--m_timer;
				if (!m_timer)
				{
					// Timer continues
					m_timer = KEYHOLD_TIMER;
					// Trigger!
					SDL_Event event;
					event.type = SDL_KEYDOWN;
					event.key.keysym.sym=btn2keycode(j_btn);
					
					event.key.state = SDL_PRESSED;
					
					SDL_PushEvent(&event);
					
				}
			}
			else
			{
				// Start timer
				m_timer = KEYHOLD_TIMER_FIRST;
			}
		}
		else
		{
			// Stop timer if running
			if (m_timer)
				m_timer = 0;
		}
	}
	
	
}

void sendKey(int key, bool pressed)
{
	unsigned char bytes [5];
	bytes[0] = (char) (use_mouse << 4) | pressed;
	
	if(use_mouse)
	{
		bytes[1] = (char) (joymouseX >> 8 & 0xFF);
		bytes[2] = (char) (joymouseX & 0xFF);
		bytes[3] = (char) (joymouseY >> 8 & 0xFF);
		bytes[4] = (char) (joymouseY & 0xFF);
	}
	else
	{
		bytes[1] = (char) (key >> 24 & 0xFF);
		bytes[2] = (char) (key >> 16 & 0xFF);
		bytes[3] = (char) (key >> 8 & 0xFF);
		bytes[4] = (char) (key & 0xFF);
	}
	
	//std::cout<<"sendkey:"<<key<<" pressed:"<<pressed<<std::endl;
	
	fwrite(&bytes, sizeof(char), 5, stderr);
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


void rot(unsigned char* src,unsigned char*dest,int w,int h)
{
	for (int i = 0; i < w; i++)
	{
		for (int j = 0; j < h; j++)
		{
			dest[i*h*BYTES+j*BYTES]=src[j*w*BYTES+(w-i)*BYTES];
			dest[i*h*BYTES+j*BYTES+1]=src[j*w*BYTES+(w-i)*BYTES+1];
		}
	}
}

void rot2(unsigned char* src,unsigned char*dest,int w,int h)
{
	for (int i = 0; i < w; i++)
	{
		for (int j = 0; j < h; j++)
		{
			dest[(w-i-1)*h*BYTES+j*BYTES]=src[(h-j-1)*w*BYTES+(w-i)*BYTES];
			dest[(w-i-1)*h*BYTES+j*BYTES+1]=src[(h-j-1)*w*BYTES+(w-i)*BYTES+1];
			
		}
	}
}

void drawFrame(unsigned char *frame, SDL_Texture *mTexture,size_t pitch, SDL_Rect *dest, int interFrame = 16)
{
	//Cutoff rendering at 60fps，毫秒
	if (SDL_GetTicks() - last_time < interFrame) {
		return;
	}

	last_time = SDL_GetTicks();

	SDL_RenderClear(mRenderer);
	//SDL_RenderCopy(mRenderer, mBackground, NULL, NULL);
	
	SDL_UpdateTexture(mTexture, NULL, frame, pitch);
	SDL_RenderCopy(mRenderer, mTexture, NULL, dest);
	
	SDL_RenderCopy(mRenderer, mOverlay, NULL, dest);

	//更新到屏幕
	SDL_RenderPresent(mRenderer);
	
}

/******************************************************** Processing Function */
void init()
{
	if (source_width == 0 || source_height == 0)
	{
		std::cout << "anbu: Neither width nor height parameters can be 0." << std::endl;
		exit(0);
	}
	//if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_JOYSTICK) < 0 )
	if(SDL_Init(SDL_INIT_VIDEO | SDL_INIT_JOYSTICK) < 0) 
	{
		std::cout<<"SDL无法初始化! SDL_Error: "<<SDL_GetError()<<std::endl;
		exit(0);
	}
	
	if (SDL_NumJoysticks() >= 1)
	{
		g_joystick = SDL_JoystickOpen(0);
		if (g_joystick == NULL)
		{
			std::cout<<"Unable to open joystick."<<std::endl;
			exit(0);
		}
	}


	SDL_SetHint(SDL_HINT_RENDER_SCALE_QUALITY, "linear");

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
			bytes[c] = 64;
			bytes[c+1] = 64;
			bytes[c+2] = 64;
			bytes[c+3] = w % psize == 0 || h % psize == 0 ? 32 : 0;
		}

	mOverlay = SDL_CreateTexture(mRenderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STATIC, rect.w, rect.h);
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
	loadOverlay(dest);
	
	size_t num_chars = source_width * source_height * BYTES;
	unsigned char* frame = new unsigned char[num_chars];
	unsigned char* tmp_frame = new unsigned char[num_chars];
	
	//从输入流里读取到frame
	while (capturing && updateFrame(num_chars, frame) || !sendQuitEvent())
	{
		if(rotate==1)
		{
			
			rot(frame,tmp_frame,source_width, source_height);
			drawFrame(tmp_frame, romTexture,ropitch, &rodest);
		}
		else if(rotate==2)
		{
			rot2(frame,tmp_frame,source_width, source_height);
			drawFrame(tmp_frame, romTexture,ropitch, &rodest);
		}
		else
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
								break;
							case 2: 
								frame[t] = 0xFF; 
								frame[t+1] = 0xFF; 
								break;
						}
						
					}
				}
			}
			
			
			drawFrame(frame, mTexture,pitch, &dest);
		}
		
	}

	if(g_joystick)
	{
		SDL_JoystickClose(g_joystick);
	}
	SDL_DestroyTexture(mTexture);
	SDL_DestroyTexture(romTexture);
	
	//SDL_DestroyTexture(mBackground);
	SDL_DestroyTexture(mOverlay);
	
	SDL_DestroyRenderer(mRenderer);
	
    SDL_DestroyWindow(mWindow);
	delete[] frame;
	delete[] tmp_frame;
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

int jaxis2key(SDL_Event event)
{
	if(event.jaxis.axis==2)
	{
		return M_L2;
	}
	else if(event.jaxis.axis==5)
	{
		return M_R2;
	}
	
	return SDLK_x;
}

void *startCapturing(void *args)
{
	int mod=0;
	int ignore=0;
	//int isFirst=1;
	while (capturing)
	{
		SDL_Event event;
		//if (SDL_WaitEvent(&event))
		while(SDL_PollEvent(&event))
		{
			switch (event.type)
			{
				case SDL_QUIT:
					capturing = false;
					sendKey(-1, true);
					fflush(stderr);
					//continue;
					break;
					
					
				case SDL_JOYHATMOTION:
					//printf("jhat.hat:%x jhat.value:%x event.key.state:%d \n",event.jhat.hat, event.jhat.value, event.key.state);
					//fflush(stdout);
					
					if ( event.jhat.value == SDL_HAT_UP )
					{
						event.type=SDL_JOYBUTTONDOWN;
						event.jbutton.button=M_UP;
						event.jbutton.state = SDL_PRESSED;
						
					}
					
					else if ( event.jhat.value == SDL_HAT_DOWN )
					{
						
						event.type=SDL_JOYBUTTONDOWN;
						event.jbutton.button=M_DOWN;
						event.jbutton.state = SDL_PRESSED;
					}
					
					else if ( event.jhat.value == SDL_HAT_LEFT )
					{
						
						event.type=SDL_JOYBUTTONDOWN;
						event.jbutton.button=M_LEFT;
						event.jbutton.state = SDL_PRESSED;
					}
					
					else if ( event.jhat.value == SDL_HAT_RIGHT )
					{
						
						event.type=SDL_JOYBUTTONDOWN;
						event.jbutton.button=M_RIGHT;
						event.jbutton.state = SDL_PRESSED;
					}
					
					else
					{
						event.type=SDL_JOYBUTTONUP;
						event.jbutton.button=j_btn;
						event.jbutton.state = SDL_RELEASED;
						
						
						joyButtons[M_UP].isPressed=false;
						joyButtons[M_DOWN].isPressed=false;
						joyButtons[M_LEFT].isPressed=false;
						joyButtons[M_RIGHT].isPressed=false;
						
					}

				case SDL_JOYBUTTONDOWN:
				case SDL_JOYBUTTONUP:
				{
					//printf("joybtn:%x joybtn.state:%d\n",event.jbutton.button, event.jbutton.state);
					//fflush(stdout);
					
					if(event.jbutton.button>=M_UP && event.jbutton.button<=M_RIGHT)
					{
						j_btn=event.jbutton.button;
						joyButtons[j_btn].isPressed = (event.jbutton.state == SDL_PRESSED); 
						//joyButtons[j_btn].m_timer = 0;
					}
				
				
					int key = event.jbutton.button;
					
					if (key == M_QUIT1) {
						capturing = false;
						sendKey(-1, true);
						fflush(stderr);
						//continue;
						break;
					}
					if (key == M_QUIT2) {
						capturing = false;
						sendKey(-1, true);
						fflush(stderr);
						//continue;
						break;
					}
					
					else if(key==KEY_RIGHT)//A=右键
					{
						key=SDLK_w;
					}
					else if(key==KEY_0)//B=0
					{
						if(!ignore)
						{
							key=SDLK_0;
						}
						else
						{
							key=SDLK_x;//英文x，这里只要不冲突就行
							ignore=0;
						}
						
						
						if(mod && event.type == SDL_JOYBUTTONDOWN)
						{
							key=SDLK_x;//英文x，这里只要不冲突就行
							mod=0;
							rotate=(1+rotate)%3;//连续旋转
							ignore=1;//忽略下次0键的释放
						}
						
					}
					else if(key==KEY_OK) //X=ok
					{
						key=SDLK_RETURN;
						
						if(use_mouse)
						{
							sendKey(key, event.jbutton.state == SDL_PRESSED);
							fflush(stderr);
							//continue;
							break;
						}
						
					}
					else if(key==KEY_LEFT) //Y=左键
					{
						
						if(!ignore)
						{
							key=SDLK_q;
						}
						else
						{
							key=SDLK_x;//英文x，这里只要不冲突就行
							ignore=0;
						}
						
						
						if(mod && event.type == SDL_JOYBUTTONDOWN)
						{
							key=SDLK_x;//英文x，这里只要不冲突就行
							mod=0;
							use_mouse=1-use_mouse;//切换鼠标
							ignore=1;//忽略下次y键的释放
						}
						
						
					}
					else if(key==M_UP)//上
					{
						key=SDLK_UP;
						if(rotate==1)
						{
							key=SDLK_RIGHT;
						}
						else if(rotate==2)
						{
							key=SDLK_LEFT;
						}
						if(use_mouse && event.jbutton.state == SDL_PRESSED)
						{
							updateMouse(key);
						}
					}
					else if(key==M_DOWN)//下
					{
						key=SDLK_DOWN;
						if(rotate==1)
						{
							key=SDLK_LEFT;
						}
						else if(rotate==2)
						{
							key=SDLK_RIGHT;
						}
						if(use_mouse && event.jbutton.state == SDL_PRESSED)
						{
							updateMouse(key);
						}
					}
					else if(key==M_LEFT) //左
					{
						key=SDLK_LEFT;
						if(rotate==1)
						{
							key=SDLK_UP;
						}
						else if(rotate==2)
						{
							key=SDLK_DOWN;
						}
						if(use_mouse && event.jbutton.state == SDL_PRESSED)
						{
							updateMouse(key);
						}
					}
					else if(key==M_RIGHT) //右
					{
						key=SDLK_RIGHT;
						if(rotate==1)
						{
							key=SDLK_DOWN;
						}
						else if(rotate==2)
						{
							key=SDLK_UP;
						}
						if(use_mouse && event.jbutton.state == SDL_PRESSED)
						{
							updateMouse(key);
						}
					}
					else if(key==KEY_POUND) //start=#
					{
						if(!ignore)
						{
							key=SDLK_r;
						}
						else
						{
							key=SDLK_x;//英文x，这里只要不冲突就行
							ignore=0;
						}
						
						
						
						if(mod && event.type == SDL_JOYBUTTONDOWN)
						{
							key=SDLK_c;//英文c,发送到java,作为切换按键模式的信号
							mod=0;
							ignore=1;
						}
						
					}
					else if(key==KEY_STAR) //select=*
					{
						key=SDLK_e;
					}
					else if(key==KEY_1) //L1=1
					{
						key=SDLK_1;
					}
					else if(key==KEY_7) //L2=7
					{
						key=SDLK_7;
					}
					else if(key==KEY_3) //R1=3
					{
						key=SDLK_3;
					}
					else if(key==KEY_9) //R2=9
					{
						key=SDLK_9;
					}
					
					
					//按住select键
					if(event.type == SDL_JOYBUTTONDOWN && event.jbutton.button == M_select){mod=1;}
					else if(event.type == SDL_JOYBUTTONUP && event.jbutton.button == M_select){mod=0;}
					
					if(!use_mouse)
					{
						sendKey(key, event.jbutton.state == SDL_PRESSED);
					}
				}
				break;
				
				case SDL_JOYAXISMOTION:
				{
					printf("SDL_JOYAXISMOTION:event type 0x%x axis:%d value:%d\n",event.type,event.jaxis.axis, event.jaxis.value);
					fflush(stdout);
					
					int key=jaxis2key(event);
					if(key==KEY_7) //L2=7
					{
						key=SDLK_7;
					}
					else if(key==KEY_9) //R2=9
					{
						key=SDLK_9;
					}
					
					if(!use_mouse)
					{
						sendKey(key, event.jaxis.value>0 ? true : false);
					}
				}
					
					break;
				
				case SDL_KEYDOWN:
				case SDL_KEYUP:
				{
					printf("keycode: 0x%x name: %s state: %d\n",event.key.keysym.sym,SDL_GetKeyName(event.key.keysym.sym),event.key.state);
					fflush(stdout);
					
					int key = event.key.keysym.sym;
					if(key==SDLK_UP)//上
					{
						if(rotate==1)
						{
							key=SDLK_RIGHT;
						}
						else if(rotate==2)
						{
							key=SDLK_LEFT;
						}
						
						if(use_mouse && event.key.state == SDL_PRESSED)
						{
							updateMouse(key);
						}
						
					}
					else if(key==SDLK_DOWN)//下
					{
						if(rotate==1)
						{
							key=SDLK_LEFT;
						}
						else if(rotate==2)
						{
							key=SDLK_RIGHT;
						}
						if(use_mouse && event.key.state == SDL_PRESSED)
						{
							updateMouse(key);
						}
					}
					else if(key==SDLK_LEFT) //左
					{
						if(rotate==1)
						{
							key=SDLK_UP;
						}
						else if(rotate==2)
						{
							key=SDLK_DOWN;
						}
						if(use_mouse && event.key.state == SDL_PRESSED)
						{
							updateMouse(key);
						}
					}
					else if(key==SDLK_RIGHT) //右
					{
						if(rotate==1)
						{
							key=SDLK_DOWN;
						}
						else if(rotate==2)
						{
							key=SDLK_UP;
						}
						if(use_mouse && event.key.state == SDL_PRESSED)
						{
							updateMouse(key);
						}
					}
					
					if(!use_mouse)
					{
						sendKey(key, event.key.state == SDL_PRESSED);
					}
				}
				break;
				
				
				default:
					printf("未知事件:event type 0x%x \n",event.type);
					fflush(stdout);
				
				
			}//switch
			fflush(stderr);
		}//while
		
		if(!capturing)
		{
			break;
		}
		
		tick();
		
		LimitFrameRate();
		
	}//while
	fflush(stderr);
	pthread_exit(NULL);
}

int keyname2keycode(const char * name)
{
	if(strcmp(name,"X")==0)
	{
		return M_X;
	}
	else if(strcmp(name,"B")==0)
	{
		return M_B;
	}
	else if(strcmp(name,"A")==0)
	{
		return M_A;
	}
	else if(strcmp(name,"SELECT")==0)
	{
		return M_select;
	}
	else if(strcmp(name,"START")==0)
	{
		return M_start;
	}
	else if(strcmp(name,"Y")==0)
	{
		return M_Y;
	}
	else if(strcmp(name,"L")==0)
	{
		return M_L;
	}
	else if(strcmp(name,"R")==0)
	{
		return M_R;
	}
	else if(strcmp(name,"L2")==0)
	{
		return M_L2;
	}
	else if(strcmp(name,"R2")==0)
	{
		return M_R2;
	}
	
	return 0;
	
}

void defaultKeymap()
{
	KEY_LEFT=M_Y;
	KEY_RIGHT=M_A;
	KEY_OK=M_X;
	KEY_STAR=M_select;
	KEY_POUND=M_start;
	KEY_1=M_L;
	KEY_3=M_R;
	KEY_7=M_L2;
	KEY_9=M_R2;
	KEY_0=M_B;
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
		KEY_LEFT=keyname2keycode(name->valuestring);
		
		name = cJSON_GetObjectItem(json, "右键");
		KEY_RIGHT=keyname2keycode(name->valuestring);
		
		name = cJSON_GetObjectItem(json, "OK");
		KEY_OK=keyname2keycode(name->valuestring);
		
		name = cJSON_GetObjectItem(json, "*");
		KEY_STAR=keyname2keycode(name->valuestring);
		
		name = cJSON_GetObjectItem(json, "#");
		KEY_POUND=keyname2keycode(name->valuestring);
		
		name = cJSON_GetObjectItem(json, "0");
		KEY_0=keyname2keycode(name->valuestring);
		
		name = cJSON_GetObjectItem(json, "1");
		KEY_1=keyname2keycode(name->valuestring);
		
		name = cJSON_GetObjectItem(json, "3");
		KEY_3=keyname2keycode(name->valuestring);
		
		name = cJSON_GetObjectItem(json, "7");
		KEY_7=keyname2keycode(name->valuestring);
		
		name = cJSON_GetObjectItem(json, "9");
		KEY_9=keyname2keycode(name->valuestring);
		
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
	
	initButtonStates();
	
	if (pthread_create(&t_capturing, 0, &startCapturing, NULL))
	{
		std::cout << "Unable to start thread, exiting ..." << std::endl;
		SDL_Quit();
		return 1;
	}

	startStreaming();
	pthread_join(t_capturing, NULL);
	SDL_ShowCursor(false);
	SDL_Quit();
	return 0;
}
