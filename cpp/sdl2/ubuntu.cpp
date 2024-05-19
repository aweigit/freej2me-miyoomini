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

#include <stdio.h>
#include <iostream>
#include <cstring>
#include <cmath>

#include <pthread.h>
#include <SDL2/SDL.h>

#define BYTES 2

pthread_t t_capturing;

int angle = 0;
//原始游戏画面大小
int source_width = 0, source_height = 0;
//miyoo的屏幕大小
int display_width = 640, display_height = 480;


unsigned int last_time = 0;

bool capturing = true;

int rotate=0;

int overlay_scale=4;

SDL_Renderer *mRenderer;
SDL_Texture *mBackground;
SDL_Texture *mTexture;
SDL_Texture *romTexture;
SDL_Texture *mOverlay;
SDL_Window *mWindow;


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
		bytes[1] = (char) (key >> 24);
		bytes[2] = (char) (key >> 16);
		bytes[3] = (char) (key >> 8);
		bytes[4] = (char) (key);
	}
	
	fwrite(&bytes, sizeof(char), 5, stdout);
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

/* void saveFrame(unsigned char* frame)
{
	int width = source_width;
	int height = source_height;
	int scan_width = source_width * sizeof(unsigned char) * 3;
	FIBITMAP *dst = FreeImage_ConvertFromRawBits(frame, width, height, scan_width, 32, FI_RGBA_RED_MASK, FI_RGBA_GREEN_MASK, FI_RGBA_BLUE_MASK, FALSE);
	string name = to_string(image_index) + ".jpg";
	image_index++;
	FreeImage_Save(FIF_JPEG, dst, name.c_str(), 0);
} */
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
	SDL_RenderCopy(mRenderer, mBackground, NULL, NULL);
	SDL_UpdateTexture(mTexture, NULL, frame, pitch);
	//SDL_RenderCopy(mRenderer, mTexture, NULL, dest);
	SDL_RenderCopyEx(mRenderer, mTexture, NULL, dest, angle, NULL, SDL_FLIP_NONE);
	
	SDL_RenderCopyEx(mRenderer, mOverlay, NULL, dest, angle, NULL, SDL_FLIP_NONE);
	//更新到屏幕
	SDL_RenderPresent(mRenderer);
	
}

void loadBackground()
{
	
	SDL_Surface *bmp = NULL;
	bmp = SDL_LoadBMP("bg.bmp");
    if (bmp == NULL) {
        std::cerr<<"SDL_LoadBMP failed: "<<SDL_GetError()<<std::endl;
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
			bytes[c] = 32;
			bytes[c+1] = 32;
			bytes[c+2] = 32;
			bytes[c+3] = w % psize == 0 || h % psize == 0 ? 32 : 0;
		}

	mOverlay = SDL_CreateTexture(mRenderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STATIC, rect.w, rect.h);
	SDL_SetTextureBlendMode(mOverlay, SDL_BLENDMODE_BLEND);
	SDL_UpdateTexture(mOverlay, NULL, bytes, rect.w * sizeof(unsigned char) * 4);
	delete[] bytes;
}

/******************************************************** Processing Function */
void init()
{
	if (source_width == 0 || source_height == 0)
	{
		std::cerr << "anbu: Neither width nor height parameters can be 0." << std::endl;
		exit(0);
	}
	//if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_JOYSTICK) < 0 )
	if(SDL_Init(SDL_INIT_VIDEO) < 0) 
	{
		std::cerr<<"SDL无法初始化! SDL_Error:"<<SDL_GetError()<<std::endl;
		exit(0);
	}
	
	/* if(SDL_InitSubSystem(SDL_INIT_JOYSTICK) == 0 && SDL_JoystickOpen(0) != NULL) {
        std::cerr<<"Initialize JOYSTICK\n";
    } */

	// Set scaling propertieis
	/*SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 8);
	SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 8);
	SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 8);
	SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, 8);
	SDL_GL_SetAttribute(SDL_GL_BUFFER_SIZE, 32);
	SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 0);*/
	SDL_SetHint(SDL_HINT_RENDER_SCALE_QUALITY, "linear");


	//int window_flag = SDL_WINDOW_OPENGL | SDL_WINDOW_SHOWN;
	int window_flag =  SDL_WINDOW_SHOWN;

/* #if SDL_VERSION_ATLEAST(2,0,1)
    window_flag |= SDL_WINDOW_ALLOW_HIGHDPI;
#endif */
	
	int window_x = SDL_WINDOWPOS_UNDEFINED, window_y = SDL_WINDOWPOS_UNDEFINED;

	try{
		mWindow = SDL_CreateWindow(NULL, window_x, window_y, display_width, display_height, window_flag);
		
	}
	catch(std::exception& e)
	{
		std::cerr << "SDL_CreateWindow err:"<<e.what() << std::endl;
		exit(-1);
	}
	

	if(mWindow == NULL) {
        std::cerr<<"Could not create window: "<<SDL_GetError()<<std::endl;
        exit(-1);
    }

	Uint32 render_flag = SDL_RENDERER_ACCELERATED;
	bool vsync = true;
	if(vsync) {
        render_flag |= SDL_RENDERER_PRESENTVSYNC;
    }
    mRenderer = SDL_CreateRenderer(mWindow, -1, render_flag);
	

    SDL_RenderSetLogicalSize(mRenderer, display_width, display_height);
    SDL_SetRenderDrawColor(mRenderer, 0, 0, 0, 255);
	
	SDL_RenderClear(mRenderer);
	SDL_RenderPresent(mRenderer);
	
	
	static int display_in_use = 0; /* Only using first display */

	int i, display_mode_count;
	SDL_DisplayMode mode;
	Uint32 f;

	SDL_Log("SDL_GetNumVideoDisplays(): %i", SDL_GetNumVideoDisplays());

	display_mode_count = SDL_GetNumDisplayModes(display_in_use);
	if (display_mode_count < 1) {
		SDL_Log("SDL_GetNumDisplayModes failed: %s", SDL_GetError());
	}
	SDL_Log("SDL_GetNumDisplayModes: %i", display_mode_count);

	for (i = 0; i < display_mode_count; ++i) {
		if (SDL_GetDisplayMode(display_in_use, i, &mode) != 0) {
			SDL_Log("SDL_GetDisplayMode failed: %s", SDL_GetError());
			break;
		}
		f = mode.format;

		SDL_Log("Mode %i\tbpp %i\t%s\t%i x %i",
				i, SDL_BITSPERPIXEL(f),
				SDL_GetPixelFormatName(f),
				mode.w, mode.h);
	}
	
	
}

/* void CONVERT_24to32 (unsigned char *image_in, unsigned char *image_out, int w, int h)
{
	for (int i = 0; i < h; i++)
	{
		for (int j = 0; j < w; j++)
		{
			if (SDL_BYTEORDER == SDL_LIL_ENDIAN)
			{
				image_out[(i * w + j) * 4 + 0] = image_in[(i * w + j) * 3 + 2];
				image_out[(i * w + j) * 4 + 1] = image_in[(i * w + j) * 3 + 1];
				image_out[(i * w + j) * 4 + 2] = image_in[(i * w + j) * 3 + 0];
				image_out[(i * w + j) * 4 + 3] = '\0';
				
			}
			else 
			{
				image_out[(i * w + j) * 4] = 0;
				memcpy(image_out + (i * w + j) * 4 + 1, image_in + (i * w + j) * 3, 3);
			}
		}
	}
} */

/* void CONVERT_24to32 (unsigned char *image_in, unsigned char *image_out, int w, int h)
{
	for (int i = 0; i < h; i++)
	{
		for (int j = 0; j < w; j++)
		{
			image_out[(i * w + j) * 4 + 0] = 0;
			image_out[(i * w + j) * 4 + 1] = 255;
			image_out[(i * w + j) * 4 + 2] = 0;
			image_out[(i * w + j) * 4 + 3] = 0;
		}
	}
} */


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

	loadBackground();
	loadOverlay(dest);

	 
	size_t num_chars = source_width * source_height * BYTES;
	unsigned char* frame = new unsigned char[num_chars];
	unsigned char* tmp_frame = new unsigned char[num_chars];
	

	//从输入流里读取到frame
	while (capturing && updateFrame(num_chars, frame) || !sendQuitEvent())
	{
		//CONVERT_24to32 (frame, image_out, source_width, source_height);
		if(rotate==1)
		{
			
			rot(frame,tmp_frame,source_width, source_height);
			drawFrame(tmp_frame, romTexture,ropitch, &rodest);
			
			//drawFrame(frame, mTexture,pitch, &dest, 270);
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
	

	SDL_DestroyTexture(mTexture);
	SDL_DestroyTexture(romTexture);
	SDL_DestroyTexture(mBackground);
	SDL_DestroyTexture(mOverlay);
	delete[] frame;
	delete[] tmp_frame;
}

void *startCapturing(void *args)
{
	
	//SDL_JoystickEventState(SDL_ENABLE);
	int mod=0;
	int ignore=0;
	int isFirst=1;
	//SDL_EnableKeyRepeat(200, 20);
	while (capturing)
	{
		SDL_Event event;
		if (SDL_WaitEvent(&event))
		// while(SDL_PollEvent(&event)!= 0)
		{
			switch (event.type)
			{
			case SDL_QUIT:
				capturing = false;
				sendKey(-1, true);
				continue;

			case SDL_KEYDOWN:
			case SDL_KEYUP:
				if(event.key.repeat && isFirst)//重复按键
				{
					isFirst=0;
					continue;
				}
				
				if(event.key.repeat ==0 || event.type==SDL_KEYUP)
				{
					isFirst=1;
				}
			
			
				int key = event.key.keysym.sym;
				
				if (key == SDLK_ESCAPE) {
					capturing = false;
					sendKey(-1, true);
					continue;
				}
				if (key == SDLK_HOME) {
					capturing = false;
					sendKey(-1, true);
					continue;
				}
				
				else if(key==SDLK_z)//z旋转画面
				{
					if(ignore)
					{
						key=SDLK_x;//英文x，这里只要不冲突就行
						ignore=0;
					}
					
					
					if(mod && event.type == SDL_KEYDOWN)
					{
						key=SDLK_x;//英文x，这里只要不冲突就行
						mod=0;
						rotate=(1+rotate)%3;//连续旋转
						ignore=1;//忽略下次z键的释放
					}
					
				}
				else if(key==SDLK_RETURN) //enter=ok
				{	
					if(use_mouse)
					{
						sendKey(key, event.key.state == SDL_PRESSED);
						fflush(stdout);
						continue;
					}
					
				}
				else if(key==SDLK_a) //a切换鼠标
				{
					
					if(ignore)
					{
						key=SDLK_x;//英文x，这里只要不冲突就行
						ignore=0;
					}
					
					
					if(mod && event.type == SDL_KEYDOWN)
					{
						key=SDLK_x;//英文x，这里只要不冲突就行
						mod=0;
						use_mouse=1-use_mouse;//切换鼠标
						ignore=1;//忽略下次a键的释放
					}
				}
				else if(key==SDLK_UP)//上
				{
					if(rotate==1)
					{
						key=SDLK_RIGHT;
					}
					else if(rotate==2)
					{
						key=SDLK_LEFT;
					}
					else
					{
						if(use_mouse && event.key.state == SDL_PRESSED)
						{
							if(joymouseY<6)
							{
								joymouseY=0;
							}
							else
							{
								joymouseY-=6;
							}
						}
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
					else
					{
						if(use_mouse && event.key.state == SDL_PRESSED)
						{
							if(joymouseY+6>=source_height-11)
							{
								joymouseY=source_height-11;
							}
							else
							{
								joymouseY+=6;
							}
						}
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
					else
					{
						if(use_mouse && event.key.state == SDL_PRESSED)
						{
							if(joymouseX<6)
							{
								joymouseX=0;
							}
							else
							{
								joymouseX-=6;
							}
						}
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
					else
					{
						if(use_mouse && event.key.state == SDL_PRESSED)
						{
							if(joymouseX+6>=source_width-8)
							{
								joymouseX=source_width-8;
							}
							else
							{
								joymouseX+=6;
							}
						}
					}
				}
				else if(key==SDLK_LSHIFT)//切换按键模式
				{
					if(ignore)
					{
						key=SDLK_x;//英文x，这里只要不冲突就行
						ignore=0;
					}
					
					
					if(mod && event.type == SDL_KEYDOWN)
					{
						key=SDLK_c;//英文c，这里只要不冲突就行,但是这个c要发送到java
						mod=0;
						ignore=1;
					}
					
				}
				
				
				//按住select键
				if(event.type == SDL_KEYDOWN && event.key.keysym.sym == SDLK_LCTRL){mod=1;}
				else if(event.type == SDL_KEYUP && event.key.keysym.sym == SDLK_LCTRL){mod=0;}
				
				
				
				/* Uint8 *keystates = SDL_GetKeyState( NULL );
				key_states[SDL_SCANCODE_UP]; */
				//std::cerr<<event.key.keysym.sym<<SDL_GetKeyName(event.key.keysym.sym)<<std::endl;
				
				if(!use_mouse)
				{
					sendKey(key, event.key.state == SDL_PRESSED);
				}
				break;
			}
			fflush(stdout);
			
			//SDL_Delay(20);
		}
	}
	fflush(stdout);
	pthread_exit(NULL);
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
	
	init();

	
	if (pthread_create(&t_capturing, 0, &startCapturing, NULL))
	{
		std::cerr << "Unable to start thread, exiting ..." << std::endl;
		SDL_Quit();
		return 1;
	}

	startStreaming();
	pthread_join(t_capturing, NULL);
	SDL_ShowCursor(false);
	SDL_Quit();
	return 0;
}
