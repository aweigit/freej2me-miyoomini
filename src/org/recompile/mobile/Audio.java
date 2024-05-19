package org.recompile.mobile;

public class Audio
{
	static public void start(String bgmfile, int loop)
	{
		_start(bgmfile,loop);
	}
	
	static public void stop(int type)
	{
		_stop(type);
	}
	
	static public void setVol(int level)
	{
		_setVol(level);
	}
	
	static public void destroy()
	{
		_destroy();
	}
	
	
	private native static void _start(String bgmfile,int loop);
	
	private native static void _stop(int type);
	
	private native static void _setVol(int level);
	
	private native static void _destroy();

}