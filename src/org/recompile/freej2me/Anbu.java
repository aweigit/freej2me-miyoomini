/*
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
package org.recompile.freej2me;

import org.recompile.mobile.*;

import java.awt.Image;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;

//import javax.sound.midi.*;
import org.recompile.mobile.SdlMixerManager;

public class Anbu
{
	public static void main(String[] args)
	{
		
		System.loadLibrary("audio");
		
		Anbu app = new Anbu(args);
		
	}

	private final SDL sdl;
	//public Audio au;

	private final int  lcdWidth;
	private final int  lcdHeight;

	private final boolean[]  pressedKeys = new boolean[128];

	private final Runnable  painter;
	
	private long pretime=0;

	private int useFlag=0;
	private int soundLevel=100;
	
	
	private final SDLConfig  config;
	private int fps=16;
	private int showfps=0;
	
	private static java.awt.Font globalFont=null;

	//private BufferedImage canvas;
	//private Graphics2D gc;
	
	private final byte []  keyPix={
		//p
		0,0,0,0,0,0,0,0,0,0,0,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,0,0,0,0,0,0,1,1,0,
		0,1,1,0,0,0,0,0,0,1,1,0,
		0,1,1,0,0,0,0,0,0,1,1,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,0,0,0,0,0,0,0,0,0,
		0,1,1,0,0,0,0,0,0,0,0,0,
		0,1,1,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,
		
		//n;
		0,0,0,0,0,0,0,0,0,0,0,0,
		0,1,1,0,0,0,0,0,0,1,1,0,
		0,1,1,1,0,0,0,0,0,1,1,0,
		0,1,1,1,1,0,0,0,0,1,1,0,
		0,1,1,0,1,1,0,0,0,1,1,0,
		0,1,1,0,0,1,1,0,0,1,1,0,
		0,1,1,0,0,0,1,1,0,1,1,0,
		0,1,1,0,0,0,0,1,1,1,1,0,
		0,1,1,0,0,0,0,0,1,1,1,0,
		0,1,1,0,0,0,0,0,0,1,1,0,
		0,1,1,0,0,0,0,0,0,1,1,0,
		0,0,0,0,0,0,0,0,0,0,0,0,

		//e
		0,0,0,0,0,0,0,0,0,0,0,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,0,0,0,0,0,0,0,0,0,
		0,1,1,0,0,0,0,0,0,0,0,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,0,0,0,0,0,0,0,0,0,
		0,1,1,0,0,0,0,0,0,0,0,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,0,0,0,0,0,0,0,0,0,0,0,

		//s
		0,0,0,0,0,0,0,0,0,0,0,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,0,0,0,0,0,0,0,0,0,
		0,1,1,0,0,0,0,0,0,0,0,0,
		0,0,0,1,1,1,1,1,1,1,0,0,
		0,0,0,1,1,1,1,1,1,1,0,0,
		0,0,0,0,0,0,0,0,0,1,1,0,
		0,0,0,0,0,0,0,0,0,1,1,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,0,0,0,0,0,0,0,0,0,0,0,
		
		//m
		0,0,0,0,0,0,0,0,0,0,0,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,1,1,1,1,1,1,1,1,0,
		0,1,1,0,0,1,1,0,0,1,1,0,
		0,1,1,0,0,1,1,0,0,1,1,0,
		0,1,1,0,0,1,1,0,0,1,1,0,
		0,1,1,0,0,1,1,0,0,1,1,0,
		0,1,1,0,0,1,1,0,0,1,1,0,
		0,1,1,0,0,1,1,0,0,1,1,0,
		0,1,1,0,0,1,1,0,0,1,1,0,
		0,1,1,0,0,1,1,0,0,1,1,0,
		0,0,0,0,0,0,0,0,0,0,0,0,

	};
	
	public static java.awt.Font getFont()
	{
		if(globalFont!=null)
		{
			return globalFont;
		}
		try{
			// 加载字体
			java.awt.Font tmpFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new File("./font.ttf"));
				
			// 字体大小设置
			globalFont = tmpFont.deriveFont(java.awt.Font.PLAIN,12f); // 12f 表示12号大小
		}
		catch (Exception e) {
            System.err.println("字体文件加载失败,使用默认字体: " + e.getMessage());
			globalFont = new java.awt.Font("MiSans Normal", java.awt.Font.PLAIN,  12);
        }
		return globalFont;
	}
	

	public Anbu(String args[])
	{
		sdl = new SDL();
		//au=new Audio();

		if (args.length < 3)
		{
			System.out.println("参数数量不一致");
			System.exit(0);
		}
		
		lcdWidth = Integer.parseInt(args[1]);
		lcdHeight = Integer.parseInt(args[2]);
		
		
		String appname="";
		String[] js=args[0].split("/");
		if(js.length>0)
		{
			if(js[js.length-1].endsWith(".jar"))
			{
				appname=js[js.length-1].substring(0,js[js.length-1].length()-4);
				System.out.println("jar file name:"+appname);
			}
		}
		
		
		

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight));
		Mobile.getPlatform().dataPath="./";
		Mobile.getPlatform().rootPath="/storage/roms/j2me/";
		
		
		soundLevel=Integer.parseInt(args[3]);
		//Audio.setVol(soundLevel);
		
		config = new SDLConfig();
		config.init(appname+lcdWidth+lcdHeight);
		settingsChanged();
		
		//au.start(soundLevel);
		//canvas=new BufferedImage(lcdWidth, lcdHeight, BufferedImage.TYPE_INT_ARGB);
		//gc=canvas.createGraphics();

		painter = new Runnable()
		{
			
			public void run()
			{
				try
				{
					long last=fps-System.currentTimeMillis()-pretime;
					if(last>0)
					{
						Thread.sleep(last);
					}
					
					pretime=System.currentTimeMillis();
					//System.out.println("cur time2：" + pretime);
					
					
					/*String output;
					//System.out.println(">>>sdl interface err");
					while (sdl.processErrorOutput.ready() && (output = sdl.processErrorOutput.readLine()) != null) {
						System.out.println(output);
					}
					//System.out.println("sdl interface err<<<"); */
					
					
					/*
					while (au.processErrorOutput.ready() && (output = au.processErrorOutput.readLine()) != null) {
						System.out.println(output);
					} */
					

					
					// Send Frame to SDL interface
					
					
					
					int[] data= new int[lcdWidth* lcdHeight];
					byte[] frame = new byte[lcdWidth* lcdHeight * 2];
					
					Mobile.getPlatform().getLCD().getRGB(0, 0, lcdWidth, lcdHeight, data, 0, lcdWidth);
					
					//gc.drawImage(Mobile.getPlatform().getLCD(), 0, 0, lcdWidth, lcdHeight, null);
					//int[] data=canvas.getRGB(0, 0, lcdWidth, lcdHeight, null, 0, lcdWidth);
					
					
					
					
					byte R,G,B;
					short tmp;
					
					for(int i = 0; i < data.length; i++)
					{
						R = (byte)(data[i] >> 16);
						G = (byte)(data[i] >> 8);
						B = (byte)(data[i]);
						
						R = (byte)(R>>3);
						G = (byte)(G>>2);
						B = (byte)(B>>3);
						tmp = (short) ( R << 11 & 0xF800
								| G << 5 & 0x07E0
								| B & 0x001f);

						frame[2*i]= (byte) (tmp&0x00FF);
						frame[2*i+1]= (byte) ((tmp>>8)&0x00FF);
					}
					
					
					if(showfps<60)
					{
						int index=useFlag*144;
						int t ;
						for(int i=0;i<12;i++)
						{
							for(int j=0;j<12;j++)
							{
								
								t= ((10 + i)*lcdWidth+(10 + j))*2;
								switch (keyPix[index+(i*12)+j])
								{
									case 0: 
										frame[t] = (byte)0x00; 
										frame[t+1] = (byte)0x00;
										
										break;
									case 1: 
										frame[t] = (byte)0x78; 
										frame[t+1] = (byte)0x0F;
										break;
								}
							}
						}
						
						showfps+=1;
					}
					
					sdl.frame.write(frame);
					sdl.frame.flush();
					
					
					 
					
				}
				catch (Exception e) { 
					System.out.println("Failed to write sdl_interface");
					System.out.println(e.getMessage());
					
					new Thread(new Runnable() {  
						@Override  
						public void run() {  
							// 线程执行的代码  
							try {  
								Thread.sleep(2000); // 休眠2秒  
							} catch (InterruptedException e) {  
								e.printStackTrace();  
							}
							
							System.exit(0);
						}  
					}).start();
					
					Mobile.destroy();
					sdl.stop();
					SdlMixerManager.shutdown();
					System.exit(0);
				
				}
			}
		};

		Mobile.getPlatform().setPainter(painter);
		
		
		System.out.println("jar文件:"+args[0]);

		

		if(Mobile.getPlatform().loadJar(args[0]))
		{
			
			
			// Start SDL
			
			sdl.start();
			
			
			
			//render();
			
			// Run jar
			Mobile.getPlatform().runJar();
		}
		else
		{
			System.out.println("Couldn't load jar...");
			System.exit(0);
		}
	}
	
	
	
	private void settingsChanged()
	{
		fps = Integer.parseInt(config.settings.get("fps"));
		if(fps>0) { fps = 1000 / fps; }

		String phone = config.settings.get("phone");
		/* useNokiaControls = false;
		useSiemensControls = false;
		useMotorolaControls = false;
		Mobile.nokia = false;
		Mobile.siemens = false;
		Mobile.motorola = false; */
		useFlag=0;
		if(phone.equals("n")) { /* Mobile.nokia = true;  useNokiaControls = true; */ useFlag=1;}
		else if(phone.equals("e")) { /* Mobile.nokia = true;  useNokiaControls = true; */ useFlag=2;}
		else if(phone.equals("s")) { /* Mobile.siemens = true; useSiemensControls = true; */ useFlag=3;}
		else if(phone.equals("m")) { /* Mobile.motorola = true; useMotorolaControls = true; */ useFlag=4;}
	}
	

	private class SDL
	{
		private Timer keytimer;
		private TimerTask keytask;

		private Process proc;
		private InputStream keys;
		public OutputStream frame;
		
		//public BufferedReader processErrorOutput;
		

		public void start()
		{
			try
			{
				String[] args={"./sdl_interface",String.valueOf(lcdWidth),String.valueOf(lcdHeight)};
				
				ProcessBuilder processBuilder = new ProcessBuilder(args);
				
				proc=processBuilder.start();

				//标准输出流，从接口获取用户的按键输入
				//keys = proc.getInputStream(); //  miyoo mini/x64-linux
				keys = proc.getErrorStream(); //gkdminiplus
				//先把错误流里的数据清空
				InputStreamReader eisr = new InputStreamReader(keys);
				BufferedReader procErrorOutput = new BufferedReader(eisr);
				String output;
				while (procErrorOutput.ready() && (output = procErrorOutput.readLine()) != null) {
					//System.out.println(output);
				}
				
				//输入流，把图像传给接口
				frame = proc.getOutputStream();
				
				
				//标准错误流
				//InputStream errorStream = proc.getErrorStream();
				/* InputStream errorStream = proc.getInputStream();
				InputStreamReader eisr = new InputStreamReader(errorStream);
				processErrorOutput = new BufferedReader(eisr);
				
				String output;
				System.out.println(">>>sdl interface err");
				while (processErrorOutput.ready() && (output = processErrorOutput.readLine()) != null) {
					System.out.println(output);
				}
				System.out.println("sdl interface err<<<");  */

				keytimer = new Timer();
				keytask = new SDLKeyTimerTask();
				keytimer.schedule(keytask, 0, 5); //5ms刷新
				
			}
			catch (Exception e)
			{
				System.out.println("Failed to start sdl_interface");
				System.out.println(e.getMessage());
				System.exit(0);
			}
		}

		public void stop()
		{
			keytimer.cancel();
			proc.destroy();
			
		}

		private class SDLKeyTimerTask extends TimerTask
		{
			private int bin;
			private byte[] din = new byte[6];
			private int count = 0;
			private int code=0;
			private int mobikey;
			private int mobikeyN;
			private int x,y;
			private boolean press=false;

			public void run()
			{
				try // to read keys
				{
					while(true)
					{
						bin = keys.read();
						if(bin==-1) { return; }
						//~ System.out.print(" "+bin);
						din[count] = (byte)(bin & 0xFF);
						count++;
						if (count==5)
						{
							count = 0;
							//System.out.println(" ("+din[0]+") <- din[0]");

							switch(din[0] >>> 4)
							{
								case 0: //按键模式
									code = (din[1]<<24) | (din[2]<<16) | (din[3]<<8) | din[4];
									
									//System.out.println(" ("+code+") =============== Key");
									
									mobikey = getMobileKey(code); 
									break;
								case 1://触屏模式
									
									x=((din[1]<<8)&0xFF00) | (din[2]&0x00FF);
									y=((din[3]<<8)&0xFF00) | (din[4]&0x00FF);
									
									
									if(din[0] % 2 == 0)//释放
									{
										
										//System.out.println("鼠标释放x:"+x+" y:"+y);
										Mobile.getPlatform().pointerReleased(x, y);
										
										press=false;
									}
									else
									{
										if(press)
											return;
										//System.out.println("鼠标按下x:"+x+" y:"+y);
										Mobile.getPlatform().pointerPressed(x, y);
										
										press=true;
									}
									return;
								/* case 1: mobikey = getMobileKeyPad(code); break;
								case 2: mobikey = getMobileKeyJoy(code); break; */
								default: continue;
							}
							
							if (mobikey == 0) //Ignore events from keys not mapped to a phone keypad key
							{
								return; 
							}
							
							
							mobikeyN = (mobikey + 64) & 0x7F; //Normalized value for indexing the pressedKeys array
							
							
							if (din[0] % 2 == 0)
							{
								//Key released
								//System.out.println("keyReleased:  " + Integer.toString(mobikey));
								Mobile.getPlatform().keyReleased(mobikey);
								pressedKeys[mobikeyN] = false;
								
								//System.out.println("按键释放");
							}
							else
							{
								//Key pressed or repeated
								if (pressedKeys[mobikeyN] == false)
								{
									//System.out.println("keyPressed:  " + Integer.toString(mobikey));
									Mobile.getPlatform().keyPressed(mobikey);
									
									//System.out.println("按键按下");
								}
								else
								{
									//System.out.println("keyRepeated:  " + Integer.toString(mobikey));
									Mobile.getPlatform().keyRepeated(mobikey);
									//System.out.println("按键重复");
								}
								pressedKeys[mobikeyN] = true;
							}
							
						}
					}
				}
				catch (Exception e) { }
			}
		} // timer
	} // sdl
	
	private int getMobileKey(int keycode)
	{
		//System.out.println("按键码:"+keycode);
		if(useFlag==1)
		{
			switch(keycode)
			{
				case 0x40000052: return Mobile.NOKIA_UP;
				case 0x40000051: return Mobile.NOKIA_DOWN;
				case 0x40000050: return Mobile.NOKIA_LEFT;
				case 0x4000004F: return Mobile.NOKIA_RIGHT;
				case 0x0D: return Mobile.NOKIA_SOFT3;
				case 0x6F: return Mobile.NOKIA_SOFT3;//专为右摇杆L3预留的ok键
				
			}
		}
		
		else if(useFlag==2)
		{
			switch(keycode)
			{
				case 0x40000052: return Mobile.NOKIA_UP;
				case 0x40000051: return Mobile.NOKIA_DOWN;
				case 0x40000050: return Mobile.NOKIA_LEFT;
				case 0x4000004F: return Mobile.NOKIA_RIGHT;
				case 0x0D: return Mobile.NOKIA_SOFT3;
				case 0x6F: return Mobile.NOKIA_SOFT3;
				
				case 0x30: return 109;//m=0
				case 0x31: return 114;//r=1
				case 0x33: return 121;//y=3
				case 0x37: return 118;//v=7
				case 0x39: return 110;//n=9
				case 0x65: return 117;//* u
				case 0x72: return 106;//# j
			}
		}
		
		else if(useFlag==3)
		{
			switch(keycode)
			{
				case 0x40000052: return Mobile.SIEMENS_UP;
				case 0x40000051: return Mobile.SIEMENS_DOWN;
				case 0x40000050: return Mobile.SIEMENS_LEFT;
				case 0x4000004F: return Mobile.SIEMENS_RIGHT;
				case 0x71: return Mobile.SIEMENS_SOFT1;
				case 0x77: return Mobile.SIEMENS_SOFT2;
				case 0x0D: return Mobile.SIEMENS_FIRE;
				case 0x6F: return Mobile.SIEMENS_FIRE;
			}
		}
		
		else if(useFlag==4)
		{
			switch(keycode)
			{
				case 0x40000052: return Mobile.MOTOROLA_UP;
				case 0x40000051: return Mobile.MOTOROLA_DOWN;
				case 0x40000050: return Mobile.MOTOROLA_LEFT;
				case 0x4000004F: return Mobile.MOTOROLA_RIGHT;
				case 0x71: return Mobile.MOTOROLA_SOFT1;
				case 0x77: return Mobile.MOTOROLA_SOFT2;
				case 0x0D: return Mobile.MOTOROLA_FIRE;
				case 0x6F: return Mobile.MOTOROLA_FIRE;
			}
		}
		
		
		
		//keycode是SDL对应的键盘码
		switch(keycode)
		{
			case 0x30: return Mobile.KEY_NUM0;
			case 0x31: return Mobile.KEY_NUM1;
			case 0x32: return Mobile.KEY_NUM2;
			case 0x33: return Mobile.KEY_NUM3;
			case 0x34: return Mobile.KEY_NUM4;
			case 0x35: return Mobile.KEY_NUM5;
			case 0x36: return Mobile.KEY_NUM6;
			case 0x37: return Mobile.KEY_NUM7;
			case 0x38: return Mobile.KEY_NUM8;
			case 0x39: return Mobile.KEY_NUM9;
			case 0x2A: return Mobile.KEY_STAR;//*
			case 0x23: return Mobile.KEY_POUND;//#

			case 0x40000052: return Mobile.KEY_NUM2;
			case 0x40000051: return Mobile.KEY_NUM8;
			case 0x40000050: return Mobile.KEY_NUM4;
			case 0x4000004F: return Mobile.KEY_NUM6;

			case 0x0D: return Mobile.KEY_NUM5;
			case 0x6F: return Mobile.KEY_NUM5;

			case 0x71: return Mobile.NOKIA_SOFT1; //SDLK_q
			case 0x77: return Mobile.NOKIA_SOFT2;  //SDLK_w
			case 0x65: return Mobile.KEY_STAR;  //SDLK_e
			case 0x72: return Mobile.KEY_POUND;  ////SDLK_r
			
			case 0x64:
				if(soundLevel>20)
					soundLevel-=20;
				else
					soundLevel=0;
				//System.out.println("音量:"+soundLevel);
				//Audio.setVol(soundLevel);
				config.settings.put("sound", String.valueOf(soundLevel));
				config.saveConfig();
				break;
			case 0x75:
				if(soundLevel<80)
					soundLevel+=20;
				else
					soundLevel=100;
				//Audio.setVol(soundLevel);
				config.settings.put("sound", String.valueOf(soundLevel));
				config.saveConfig();
				//System.out.println("音量:"+soundLevel);
				break;

			// Inverted Num Pad 数字小键盘
			case 0x40000059: return Mobile.KEY_NUM7; // SDLK_KP_1
			case 0x4000005A: return Mobile.KEY_NUM8; // SDLK_KP_2
			case 0x4000005B: return Mobile.KEY_NUM9; // SDLK_KP_3
			case 0x4000005C: return Mobile.KEY_NUM4; // SDLK_KP_4
			case 0x4000005D: return Mobile.KEY_NUM5; // SDLK_KP_5
			case 0x4000005E: return Mobile.KEY_NUM6; // SDLK_KP_6
			case 0x4000005F: return Mobile.KEY_NUM1; // SDLK_KP_7
			case 0x40000060: return Mobile.KEY_NUM2; // SDLK_KP_8
			case 0x40000061: return Mobile.KEY_NUM3; // SDLK_KP_9
			case 0x40000062: return Mobile.KEY_NUM0; // SDLK_KP_0
			
			case 0x63://c
				//切换按键模式
				
				useFlag=(useFlag+1)%5;
				if(useFlag==0)
				{
					config.settings.put("phone", "p");
				}
				else if(useFlag==1)
				{
					config.settings.put("phone", "n");
				}
				else if(useFlag==2)
				{
					config.settings.put("phone", "e");
				}
				else if(useFlag==3)
				{
					config.settings.put("phone", "s");
				}
				else if(useFlag==4)
				{
					config.settings.put("phone", "m");
				}
				showfps=0;
				config.saveConfig();
				
				break;

			// F4 - Quit
			case -1: 
				//au.stop();
				Mobile.destroy();
				sdl.stop();
				SdlMixerManager.shutdown();
				
				System.exit(0);
				break;

			// ESC - Quit
			case 0x1B: 
				//au.stop();
				Mobile.destroy();
				sdl.stop();
				SdlMixerManager.shutdown();
				
				System.exit(0);
				break;
				
			// HOME - Quit
			case 0x4000004a: 
				//au.stop();
				Mobile.destroy();
				sdl.stop();
				SdlMixerManager.shutdown();

				System.exit(0);
				break;
		}
		return 0;
	}
}
