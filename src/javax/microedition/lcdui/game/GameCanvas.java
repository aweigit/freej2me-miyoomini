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
package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

public abstract class GameCanvas extends Canvas
{
	public static final int UP_PRESSED = 1 << Canvas.UP;
	public static final int DOWN_PRESSED = 1 << Canvas.DOWN;
	public static final int LEFT_PRESSED = 1 << Canvas.LEFT;
	public static final int RIGHT_PRESSED = 1 << Canvas.RIGHT;
	public static final int FIRE_PRESSED = 1 << Canvas.FIRE;
	public static final int GAME_A_PRESSED = 1 << Canvas.GAME_A;
	public static final int GAME_B_PRESSED = 1 << Canvas.GAME_B;
	public static final int GAME_C_PRESSED = 1 << Canvas.GAME_C;
	public static final int GAME_D_PRESSED = 1 << Canvas.GAME_D;

	private boolean suppressKeyEvents;

	protected GameCanvas(boolean suppressKeyEvents)
	{
		super();
		this.suppressKeyEvents = suppressKeyEvents;
		Mobile.getPlatform().suppressKeyEvents = suppressKeyEvents;
		
		//System.out.println("suppressKeyEvents is:"+suppressKeyEvents);

		width = Mobile.getPlatform().lcdWidth;
		height = Mobile.getPlatform().lcdHeight;

		platformImage = new PlatformImage(width, height);
	}

	protected Graphics getGraphics()
	{
		return platformImage.getGraphics();
	}

	@Override
	public void paint(Graphics g) {
		//System.out.println("[GameCanvas this is]");
		//g.drawImage(platformImage, 0, 0, Graphics.LEFT | Graphics.TOP);
	}

	public void flushGraphics(int x, int y, int width, int height)
	{
		Mobile.getPlatform().flushGraphics(platformImage, x, y, width, height);
	}

	public void flushGraphics()
	{
		flushGraphics(0, 0, width, height);
	}

	public int getKeyStates() // found in use
	{
		/* // 获取当前线程对象
		Thread currentThread = Thread.currentThread();
		
		// 获取当前线程的ID
		long threadId = currentThread.getId();
		
		// 打印线程ID
		System.out.println("获取keystate线程的ID是：" + threadId); */
		//当前线程跟更新keystate的不是一个线程
		//这个函数不能运行太快，快了还没来得及检测就无了
		// int t = Mobile.getPlatform().keyState;
		// Mobile.getPlatform().keyState = 0;
		int t=Mobile.getPlatform().getKeyState();
		//System.out.println("获取按键状态");
		return t;
	}
	
}
