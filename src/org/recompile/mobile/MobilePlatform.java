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
package org.recompile.mobile;

import java.net.URL;
import java.io.InputStream;

import java.awt.event.KeyEvent;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.Image;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.lcdui.TextField;

import java.awt.image.BufferedImage;


/*

	Mobile Platform

*/

public class MobilePlatform
{

	private PlatformImage lcd;
	private PlatformGraphics gc;
	public int lcdWidth;
	public int lcdHeight;

	public MIDletLoader loader;

	public Runnable painter;

	public String dataPath = "";
	public String rootPath = "";

	private int keyState = 0;
	
	private int[] keyStateArr=new int[6];
	private int s=0;
	private int e=0;

	private static int chr=-1;
	private static int numchr=-1;
	
	public boolean suppressKeyEvents=false;

	public MobilePlatform(int width, int height)
	{
		lcdWidth = width;
		lcdHeight = height;

		lcd = new PlatformImage(width, height);
		gc = lcd.getGraphics();

		//Mobile.setGraphics3D(Graphics3D.getInstance());

		painter = new Runnable()
		{
			public void run()
			{
				// Placeholder //
			}
		};
	}
	
	public void push(int state)
	{
		if((e+1)%6==s)
		{
			return;
		}
		
		keyStateArr[e]=state;
		e=(e+1)%6;
		
	}
	
	public int pop()
	{
		if(s==e)
		{
			return 0;
		}
		int state=keyStateArr[s];
		s=(s+1)%6;
		return state;
		
	}

	public void resizeLCD(int width, int height)
	{
		lcdWidth = width;
		lcdHeight = height;

		lcd = new PlatformImage(width, height);
		gc = lcd.getGraphics();
	}

	public BufferedImage getLCD()
	{
		return lcd.getCanvas();
	}

	public void setPainter(Runnable r)
	{
		painter = r;
	}

	public void keyPressed(int keycode)
	{
		TextField tf=Mobile.getTextField();
		if(tf!=null)
		{
			if(keycode==Mobile.KEY_NUM6 || keycode==Mobile.NOKIA_RIGHT)//替换a-z字符
			{
				chr=(chr+1)%26;
				tf.replace((char)(chr+0x61));
			}
			else if(keycode==Mobile.KEY_NUM4 || keycode==Mobile.NOKIA_LEFT)//替换a-z字符
			{
				chr=(chr+25)%26;
				tf.replace((char)(chr+0x61));
			}
			if(keycode==Mobile.KEY_NUM3)//替换0-9字符
			{
				numchr=(numchr+1)%10;
				tf.replace((char)(numchr+0x30));
			}
			else if(keycode==Mobile.KEY_NUM1)//替换0-9字符
			{
				numchr=(numchr+9)%10;
				tf.replace((char)(numchr+0x30));
			}
			else if(keycode==Mobile.KEY_STAR)//按*删除
			{
				tf.delete();
			}
			else if(keycode==Mobile.KEY_POUND)//按#添加新字符
			{
				tf.append((char)(chr+0x61));
			}
			
		}

		updateKeyState(keycode, 1);
		if(!suppressKeyEvents)
			Mobile.getDisplay().getCurrent().keyPressed(keycode);
	}

	public void keyReleased(int keycode)
	{
		updateKeyState(keycode, 0);
		if(!suppressKeyEvents)
			Mobile.getDisplay().getCurrent().keyReleased(keycode);
		
	}

	public void keyRepeated(int keycode)
	{
		//updateKeyState(keycode, 1);
		if(!suppressKeyEvents)
			Mobile.getDisplay().getCurrent().keyRepeated(keycode);
		
	}

	public void pointerDragged(int x, int y)
	{
		Mobile.getDisplay().getCurrent().pointerDragged(x, y);
	}

	public void pointerPressed(int x, int y)
	{
		Mobile.getDisplay().getCurrent().pointerPressed(x, y);
	}

	public void pointerReleased(int x, int y)
	{
		Mobile.getDisplay().getCurrent().pointerReleased(x, y);
	}
	

	public int getKeyState()
	{
		//System.out.println("keyState:" + keyState);
		
		int ks=0;
		
		synchronized (this)
		{
			ks=keyState;
		}
		
		return ks;
	}

	private void updateKeyState(int key, int val)
	{
		/* // 获取当前线程对象
		Thread currentThread = Thread.currentThread();
		
		// 获取当前线程的ID
		long threadId = currentThread.getId();
		
		// 打印线程ID
		System.out.println("更新keystate线程的ID是：" + threadId); */
		
		
		int mask=0;
		switch (key)
		{
			case Mobile.KEY_NUM2: mask = GameCanvas.UP_PRESSED; break;
			case Mobile.KEY_NUM4: mask = GameCanvas.LEFT_PRESSED; break;
			case Mobile.KEY_NUM6: mask = GameCanvas.RIGHT_PRESSED; break;
			case Mobile.KEY_NUM8: mask = GameCanvas.DOWN_PRESSED; break;
			case Mobile.KEY_NUM5:
			case Mobile.NOKIA_SOFT3:
				mask = GameCanvas.FIRE_PRESSED; break;
			//case Mobile.KEY_NUM1: mask = GameCanvas.GAME_A_PRESSED; break;
			case Mobile.KEY_NUM7: mask = GameCanvas.GAME_A_PRESSED; break;
			//case Mobile.KEY_NUM3: mask = GameCanvas.GAME_B_PRESSED; break;
			case Mobile.KEY_NUM9: mask = GameCanvas.GAME_B_PRESSED; break;
			//case Mobile.KEY_NUM7: mask = GameCanvas.GAME_C_PRESSED; break;
			case Mobile.KEY_STAR: mask = GameCanvas.GAME_C_PRESSED; break;
			//case Mobile.KEY_NUM9: mask = GameCanvas.GAME_D_PRESSED; break;
			case Mobile.KEY_POUND: mask = GameCanvas.GAME_D_PRESSED; break;
			case Mobile.NOKIA_UP: mask = GameCanvas.UP_PRESSED; break;
			case Mobile.NOKIA_LEFT: mask = GameCanvas.LEFT_PRESSED; break;
			case Mobile.NOKIA_RIGHT: mask = GameCanvas.RIGHT_PRESSED; break;
			case Mobile.NOKIA_DOWN: mask = GameCanvas.DOWN_PRESSED; break;
		}
		
		if(mask==0)
		{
			return;
		}
		
		synchronized (this)
		{
			if(val==1)
			{
				keyState |= mask;
			}
			else
			{
				keyState &= ~mask;
			}
		}
		
		/* keyState ^= mask;
		if(val==1) { keyState |= mask; } */
	}
	
	private int convertGameKeyCode(int keyCode) {
		switch (keyCode) {
			case Mobile.NOKIA_LEFT:
			case Mobile.KEY_NUM4:
				return GameCanvas.LEFT_PRESSED;
			case Mobile.NOKIA_UP:
			case Mobile.KEY_NUM2:
				return GameCanvas.UP_PRESSED;
			case Mobile.NOKIA_RIGHT:
			case Mobile.KEY_NUM6:
				return GameCanvas.RIGHT_PRESSED;
			case Mobile.NOKIA_DOWN:
			case Mobile.KEY_NUM8:
				return GameCanvas.DOWN_PRESSED;
			case Mobile.NOKIA_SOFT3:
			case Mobile.KEY_NUM5:
				return GameCanvas.FIRE_PRESSED;
			case Mobile.KEY_NUM7:
				return GameCanvas.GAME_A_PRESSED;
			case Mobile.KEY_NUM9:
				return GameCanvas.GAME_B_PRESSED;
			case Mobile.KEY_STAR:
				return GameCanvas.GAME_C_PRESSED;
			case Mobile.KEY_POUND:
				return GameCanvas.GAME_D_PRESSED;
			default:
				return 0;
		}
	}

/*
	******** Jar Loading ********
*/

	public boolean loadJar(String jarurl)
	{
		try
		{
			URL jar;
			if(jarurl.startsWith("/"))
			{
				jar = new URL("file:"+jarurl);
			}
			else
			{
				jar = new URL(jarurl);
			}
			
			System.out.println("[jar file url] "+jar);
			
			String appname="";
			String[] js=jarurl.split("/");
			if(js.length>0)
			{
				if(js[js.length-1].endsWith(".jar"))
				{
					appname=js[js.length-1].substring(0,js[js.length-1].length()-4);
					System.out.println("jar file name:"+appname);
				}
			}
			
			
			//loader = new MIDletLoader(new URL[]{jar},path+"_"+lcdWidth+lcdHeight);
			loader = new MIDletLoader(new URL[]{jar},jarurl,appname+lcdWidth+lcdHeight);
			return true;
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}

	}
	

	public void runJar()
	{
		try
		{
			loader.start();
		}
		catch (Exception e)
		{
			System.out.println("Error Running Jar");
			e.printStackTrace();
		}
	}

/*
	********* Graphics ********
*/

	public void flushGraphics(Image img, int x, int y, int width, int height)
	{
		gc.flushGraphics(img, x, y, width, height);

		painter.run();

		//System.gc();
	}

	public void repaint(Image img, int x, int y, int width, int height)
	{
		gc.flushGraphics(img, x, y, width, height);

		painter.run();

		//System.gc();
	}

}
