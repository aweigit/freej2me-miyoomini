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

import java.io.InputStream;
//import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FilenameFilter;
import java.security.MessageDigest;

import java.util.Vector;


import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

import javax.microedition.media.Control;
import javax.microedition.media.Controllable;

import org.recompile.mobile.SdlMixerManager;



/* import org.recompile.freej2me.Anbu;


import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.progress.EncoderProgressListener;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.MultimediaObject; */

/**
所谓的stop的使用,其实是返回prefetched(缓冲读取)的状态,因此并没有所谓的stopped的状态.一般如果播放完毕后会自动回到prefetched(缓冲读取)的状态
unrealized(没有实现)----->realized(实现)----->prefetched(缓冲读取)----->started(播放状态)   
                  realize()            prefetch()                 start()

started(播放状态)----->prefetched(缓冲读取)            realized(实现)----->unrealized(没有实现)
                 stop()                                           deallocate()释放资源      

以上4个状态随时都可以用close()返回到closed状态


**/

public class PlatformPlayer implements Player
{

	private String contentType = "";

	private audioplayer player;

	private int state = Player.UNREALIZED;

	private Vector<PlayerListener> listeners;

	private Control[] controls;
	

	public PlatformPlayer(InputStream stream, String type)
	{
		listeners = new Vector<PlayerListener>();
		controls = new Control[3];

		contentType = type;

		if(Mobile.sound == false)
		{
			player = new audioplayer();
		}
		else
		{
			if(type.equalsIgnoreCase("audio/mid") || type.equalsIgnoreCase("audio/midi") || type.equalsIgnoreCase("sp-midi") 
				|| type.equalsIgnoreCase("audio/spmidi"))
			{
				player = new sdlPlayer(stream,".mid");
			}
			else if(type.equalsIgnoreCase("audio/mpeg") || type.equalsIgnoreCase("audio/x-wav") || type.equalsIgnoreCase("audio/wav"))
			{
				player = new sdlWavPlayer(stream, ".wav");
			}
			else
			{
				player = new sdlPlayer(stream,".audio");
			}
		}
		controls[0] = new volumeControl(player);
		controls[1] = new tempoControl();
		controls[2] = new midiControl(player);

		System.out.println("media type: "+type);
	}

	public PlatformPlayer(String locator)
	{
		player = new audioplayer();
		listeners = new Vector<PlayerListener>();
		controls = new Control[3];
		System.out.println("Player locator: "+locator);
	}
	
	public String encodeHexString(byte[] data,int len) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<len; i++) {
			sb.append(String.format("%02x", data[i]));
		}
		return sb.toString();
	}
	
	final public String encodeMD5String(byte[] data) {
		StringBuilder sb = new StringBuilder();
		try{
			MessageDigest sha256 = MessageDigest.getInstance("MD5");
			byte[] hashed = sha256.digest(data);
			
			for (byte b : hashed) {
				sb.append(String.format("%02x", b));
			}
		}
		catch(Exception e)
		{
			System.out.println("字节数组转md5出错:"+e.getMessage());
		}
        return sb.toString();

	}

	public void close()
	{
		try
		{
			player.stop();
			state = Player.CLOSED;
			notifyListeners(PlayerListener.CLOSED, null);	
		}
		catch (Exception e) { }
		state = Player.CLOSED;	
	}

	public int getState()
	{
		return state;
	}

	public void start()
	{
		try
		{
			player.start();
		}
		catch (Exception e) {  }
	}

	public void stop()
	{
		try
		{
			player.stop();
			
		}
		catch (Exception e) { }
	}

	public void addPlayerListener(PlayerListener playerListener)
	{
		listeners.add(playerListener);
	}

	public void removePlayerListener(PlayerListener playerListener)
	{
		listeners.remove(playerListener);
	}

	private void notifyListeners(String event, Object eventData)
	{
		for(int i=0; i<listeners.size(); i++)
		{
			listeners.get(i).playerUpdate(this, event, eventData);
		}
	}

	public void deallocate()
	{
		stop();
		player.deallocate();
		
		state = Player.UNREALIZED;
	}

	public String getContentType() { return contentType; }

	public long getDuration() { return Player.TIME_UNKNOWN; }

	public long getMediaTime() { return player.getMediaTime(); }

	public void prefetch() { state = Player.PREFETCHED; }

	public void realize() { state = Player.REALIZED; }

	public void setLoopCount(int count) { player.setLoopCount(count); }

	public long setMediaTime(long now) { return player.setMediaTime(now); }

	// Controllable interface //

	public Control getControl(String controlType)
	{
		if(controlType.equals("VolumeControl")) { return controls[0]; }
		if(controlType.equals("TempoControl")) { return controls[1]; }
		if(controlType.equals("MIDIControl")) { return controls[2]; }
		if(controlType.equals("javax.microedition.media.control.VolumeControl")) { return controls[0]; }
		if(controlType.equals("javax.microedition.media.control.TempoControl")) { return controls[1]; }
		if(controlType.equals("javax.microedition.media.control.MIDIControl")) { return controls[2]; }
		return null;
	}

	public Control[] getControls()
	{
		return controls;
	}

	// Players //

	public static class audioplayer
	{
		public void start() {  }
		public void stop() {  }
		public void setLoopCount(int count) {  }
		public long setMediaTime(long now) { return now; }
		public long getMediaTime() { return 0; }
		public boolean isRunning() { return false; }
		public void deallocate() {  }
		public void setVolume(int vol) { };
	}
	
	public class sdlPlayer extends audioplayer
	{
		private int loops = 0;
		
		private String bgmFileName="";
		
		private SdlMixerManager manager;
		
		private long currentMusicHandle = 0;
		
		private boolean isOpen = false;
		
		public sdlPlayer(InputStream stream, String type)
		{
			try
			{
				String rmsPath = "./rms/"+Mobile.getPlatform().loader.suitename;
				try
				{
					Files.createDirectories(Paths.get(rmsPath));
				}
				catch (Exception e)
				{
					System.out.println(e.getMessage());
				}

				byte[] buffer = new byte[1024];
				int len;
				String filename="";
				if ((len = stream.read(buffer)) != -1)
				{
					filename=encodeMD5String(buffer);
					filename="./rms/"+Mobile.getPlatform().loader.suitename+"/"+filename;
				}
				
				bgmFileName=filename+type;				
				File file = new File(bgmFileName);
				
				if(!file.exists())
				{
					try
					{
						file.createNewFile();
						FileOutputStream fos = new FileOutputStream(file);
						fos.write(buffer, 0, len);
						while ((len = stream.read(buffer)) != -1) {
							fos.write(buffer, 0, len);
						}
						fos.close();
					}
					catch (Exception e)
					{
						System.out.println(e.getMessage());
					}
				}
				
				SdlMixerManager.init();
				
				manager = new SdlMixerManager();
				currentMusicHandle = manager.sdlMixerLoadMidi(bgmFileName, this);
				if (currentMusicHandle == -1) {
					System.err.println("❌ 加载MIDI文件失败: " + bgmFileName);
				}
				
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		public void start()
		{
			if(currentMusicHandle==0)
			{
				currentMusicHandle =  manager.sdlMixerLoadMidi(bgmFileName, this);
			}
			
			if (currentMusicHandle == -1) {
				System.err.println("❌ 加载MIDI文件失败: " + bgmFileName);
				return;
			}
			
			manager.sdlMixerResumeMusic();
			
			if(!isOpen || !manager.sdlMixerIsPlaying())
			{
				int result = manager.sdlMixerPlayMusic(currentMusicHandle, loops);
				if (result == 0) {
					isOpen = true;
					System.out.println("🎵 开始播放: " + bgmFileName);
				} else {
					System.err.println("❌ 播放失败");
					return;
				}
			}
			
			state = Player.STARTED;
			notifyListeners(PlayerListener.STARTED, 0);
		}
		
		public void stop()
		{
			manager.sdlMixerPauseMusic();
			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, 0);
		}
		
		public void deallocate()
		{
			if (currentMusicHandle != 0) {
				manager.sdlMixerStopMusic();//先halt
				manager.sdlMixerFreeMusic(currentMusicHandle);//再释放
				currentMusicHandle = 0;
			}
			
			isOpen = false;
		}
		
		public void setLoopCount(int count)
		{
			loops = count;
			System.out.println("midi loop: " + loops);
		}
		
		public void setVolume(int vol)
		{
			manager.sdlMixerSetVolume(vol);
		}
		
		public void onPlaybackComplete()
		{
			notifyListeners(PlayerListener.END_OF_MEDIA, 0);
		}
	}
	
	
	public class sdlWavPlayer extends audioplayer
	{
		private int loops = 0;
		
		private String bgmFileName="";
		
		private SdlMixerManager manager;
		
		private long currentMusicHandle = 0;
		
		private boolean isOpen = false;
		
		public sdlWavPlayer(InputStream stream, String type)
		{
			try
			{
				String rmsPath = "./rms/"+Mobile.getPlatform().loader.suitename;
				try
				{
					Files.createDirectories(Paths.get(rmsPath));
				}
				catch (Exception e)
				{
					System.out.println(e.getMessage());
				}
				byte[] buffer = new byte[1024];
				int len;
				String filename="";
				if ((len = stream.read(buffer)) != -1)
				{
					filename=encodeMD5String(buffer);
					filename="./rms/"+Mobile.getPlatform().loader.suitename+"/"+filename;
				}
				
				bgmFileName=filename+type;				
				File file = new File(bgmFileName);
				
				if(!file.exists())
				{
					try
					{
						file.createNewFile();
						FileOutputStream fos = new FileOutputStream(file);
						fos.write(buffer, 0, len);
						while ((len = stream.read(buffer)) != -1) {
							fos.write(buffer, 0, len);
						}
						fos.close();
					}
					catch (Exception e)
					{
						System.out.println(e.getMessage());
					}
				}
			
				SdlMixerManager.init();
				
				manager = new SdlMixerManager();
				currentMusicHandle = manager.sdlMixerLoadWav(bgmFileName);
				if (currentMusicHandle == -1) {
					System.err.println("❌ 加载WAV文件失败: " + bgmFileName);
				}
				
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		public void start()
		{
			if(currentMusicHandle==0)
			{
				currentMusicHandle =  manager.sdlMixerLoadWav(bgmFileName);
			}
			
			if (currentMusicHandle == -1) {
				System.err.println("❌ 加载WAV文件失败: " + bgmFileName);
				return;
			}
			
			manager.sdlMixerResumeWav();//由于wav只播放一次，所以暂停后已经结束播放了，再恢复也没有声音,先尝试恢复一下
			
			if(!isOpen || !manager.sdlMixerIsPlayingWav())
			{
				int result = manager.sdlMixerPlayWav(currentMusicHandle, loops);
				if (result == 0) {
					isOpen = true;
					System.out.println("🎵 开始播放: " + bgmFileName);
				} else {
					System.err.println("❌ 播放失败");
					return;
				}
			}
			
			
			state = Player.STARTED;
			notifyListeners(PlayerListener.STARTED, 0);
		}
		
		public void stop()
		{
			manager.sdlMixerPauseWav();
			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, 0);
		}
		
		public void deallocate()
		{
			if (currentMusicHandle != 0) {
				manager.sdlMixerStopWav();//先halt
				manager.sdlMixerFreeWav(currentMusicHandle);//再释放
				currentMusicHandle = 0;
			}
			
			isOpen = false;
		}
		
		public void setLoopCount(int count)
		{
			if(count>0)
			{
				count=count-1;
			}
			loops = count;
			System.out.println("wav loop: " + loops);
		}
		
		public void setVolume(int vol)
		{
			manager.sdlMixerSetVolumeWav(vol);
		}
		
	}

	// Controls //

	private class midiControl implements javax.microedition.media.control.MIDIControl
	{
		private audioplayer manager;
		public midiControl(audioplayer m)
		{
			manager = m;
		}
		
		public int[] getBankList(boolean custom) { return new int[]{}; }

		public int getChannelVolume(int channel) { return 0; }

		public java.lang.String getKeyName(int bank, int prog, int key) { return ""; }

		public int[] getProgram(int channel) { return new int[]{}; }

		public int[] getProgramList(int bank) { return new int[]{}; }

		public java.lang.String getProgramName(int bank, int prog) { return ""; }

		public boolean isBankQuerySupported() { return false; }

		public int longMidiEvent(byte[] data, int offset, int length) { return 0; }

		public void setChannelVolume(int channel, int volume) { manager.setVolume(volume); }

		public void setProgram(int channel, int bank, int program) {  }

		public void shortMidiEvent(int type, int data1, int data2) {  }
	}

	private class volumeControl implements javax.microedition.media.control.VolumeControl
	{
		private audioplayer manager;
		public volumeControl(audioplayer m)
		{
			manager = m;
		}
		private int level = 100;
		private boolean muted = false;

		public int getLevel() { return level; }

		public boolean isMuted() { return muted; }

		public int setLevel(int value) { level = value; manager.setVolume(value); return level; }

		public void setMute(boolean mute) { muted = mute; }//静音
	}

	private class tempoControl implements javax.microedition.media.control.TempoControl//设置midi节拍
	{
		int tempo = 5000;
		int rate = 5000;

		public int getTempo() { return tempo; }

		public int setTempo(int millitempo) { tempo = millitempo; return tempo; }

		// RateControl interface
		public int getMaxRate() { return rate; }

		public int getMinRate() { return rate; }

		public int getRate() { return rate; }

		public int setRate(int millirate) { rate=millirate; return rate; }
	}
	
	private native void _start(String bgmfile,int loop);
	
	
}
