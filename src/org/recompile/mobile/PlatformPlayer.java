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
/* import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Sequence;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.midi.Receiver;
import javax.sound.midi.MidiDevice; */

import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

import javax.microedition.media.Control;
import javax.microedition.media.Controllable;

import org.recompile.mobile.Audio;

import java.util.Timer;
import java.util.TimerTask;

/* import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer; */



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
	
	//public static boolean customMidi = true;

	//public static File soundfontDir = new File("./customMIDI/");

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
				player = new midiPlayer(stream,".mid");
			}
			else if(type.equalsIgnoreCase("audio/mpeg") || type.equalsIgnoreCase("audio/x-wav") || type.equalsIgnoreCase("audio/wav"))
			{
				player = new midiPlayer(stream,".wav");
			}
			else
			{
				if(type.equalsIgnoreCase("audio/x-wav") || type.equalsIgnoreCase("audio/wav"))
				{
					player = new wavPlayer(stream);
				}
				else /* TODO: Implement a player for amr and mpeg audio types */
				{
					System.out.println("No Player For: "+contentType);
					player = new audioplayer();
				}
			}
		}
		controls[0] = new volumeControl();
		controls[1] = new tempoControl();
		controls[2] = new midiControl();

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
			stop();
			state = Player.CLOSED;
			notifyListeners(PlayerListener.CLOSED, null);	
		}
		catch (Exception e) { }
		state = Player.CLOSED;	
	}

	public int getState()
	{
		/* if(player.isRunning()==false)
		{
			state = Player.PREFETCHED;
		} */
		return state;
	}

	public void start()
	{
		//System.out.println("Play "+contentType);
		try
		{
			player.start();
		}
		catch (Exception e) {  }
	}

	public void stop()
	{
		//System.out.println("Stop "+contentType);
		try
		{
			player.stop();
			notifyListeners(PlayerListener.STOPPED, null);	
			
		}
		catch (Exception e) { }
	}

	public void addPlayerListener(PlayerListener playerListener)
	{
		//System.out.println("Add Player Listener");
		listeners.add(playerListener);
	}

	public void removePlayerListener(PlayerListener playerListener)
	{
		//System.out.println("Remove Player Listener");
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
		
		//notifyListeners(PlayerListener.END_OF_MEDIA, 0);
		//state = Player.UNREALIZED;
		state = Player.REALIZED;
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

	private class audioplayer
	{
		public void start() {  }
		public void stop() {  }
		public void setLoopCount(int count) {  }
		public long setMediaTime(long now) { return now; }
		public long getMediaTime() { return 0; }
		public boolean isRunning() { return false; }
		public void deallocate() {  }
	}
	
	

	
	
	private class midiPlayer extends audioplayer
	{
		//private Sequencer midi;

		private int loops = 1;

		//private long tick = 0L;
		//private boolean isinit=false;
		
		private boolean isrun=false;
		
		private String bgmFileName="";
		
		//private int ts=0;

		public midiPlayer(InputStream stream, String type)
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
				//ByteArrayInputStream bis=(ByteArrayInputStream)stream;
				byte[] buffer = new byte[1024];
				int len;
				String filename="";
				if ((len = stream.read(buffer)) != -1)
				{
					filename=encodeMD5String(buffer);
					filename="./rms/"+Mobile.getPlatform().loader.suitename+"/"+filename;
				}
				
				bgmFileName=filename+type;				
				//System.out.println(bgmFileName);
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
				
				
				// stream.reset();
				// Sequencer midi = MidiSystem.getSequencer(false);
				// midi.open();
				// midi.setSequence(stream);
				// long mlength = midi.getMicrosecondLength();
				
				// ts=(int)Math.ceil((double)mlength/1000000);
				
				// System.out.println("微秒:"+mlength + " 秒:"+ts+" bgm:"+bgmFileName); 
				// midi.close();
				
				
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		

		public void start()
		{	
			if(bgmFileName.equals("")) { return; }
			
			if(isRunning() && bgmFileName.endsWith(".mid")) { return; }
			
			
			try{
				
				//System.out.println(bgmFileName+"开始设置播放");
				
				byte[] frame = new byte[100];
				frame[0]='$';//36
				frame[1]='C';//初始化
					
				frame[2]=(byte)(loops);
				frame[3]=(byte)(loops >> 8);
				frame[4]=(byte)(loops >> 16);
				frame[5]=(byte)(loops >> 24);
				
				byte[] fname=bgmFileName.getBytes("UTF-8");
				
				//byte[] name=new byte[fname.length+1];
				//name[fname.length]=0;
				
				for(int i = 0; i < fname.length; i++)
				{
					frame[i+6] = fname[i];
					
					//name[i]=fname[i];
				}
				
				// if(isinit)//初始化过
				// {
					// frame[1]='R';//继续
					
				// }
				// else
				// {
					// frame[1]='C';//初始化
					
					// frame[2]=(byte)(loops);
					// frame[3]=(byte)(loops >> 8);
					// frame[4]=(byte)(loops >> 16);
					// frame[5]=(byte)(loops >> 24);
					
					// byte[] fname=bgmFileName.getBytes("UTF-8");
					// for(int i = 0; i < fname.length; i++)
					// {
						// frame[i+6] = fname[i];
					// }
					
					
				// }
				
				
				//Anbu.au.frame.write(frame);
				//Anbu.au.frame.flush();
				//byte[] zero={0};
				
				
				//Audio.start(bgmFileName,loops);
				_start(bgmFileName,loops);
				
				
				// if(loops==1)
				// {
					// Timer timer = new Timer();
					// TimerTask task = new TimerTask() {
						// @Override
						// public void run() {
							// System.out.println("定时任务执行一次");
							
							// notifyListeners(PlayerListener.END_OF_MEDIA, 0);	
						// }
					// };
					// timer.schedule(task, ts); // 5秒后执行任务
				// }
				
				
			}
			catch(Exception e){
				System.out.println(e.getMessage());
			}
			isrun=true;
			//isinit=true;
			
			state = Player.STARTED;
			
			//System.out.println("开始:"+bgmFileName+" loop:"+loops);
		}

		public void stop()
		{	
			if(!isRunning())return;
			try{
				byte[] frame = new byte[100];
				frame[0]='$';//36
				frame[1]='S';//83
				//Anbu.au.frame.write(frame);
				//Anbu.au.frame.flush();
				
				if(bgmFileName.endsWith(".mid"))
				{
					Audio.stop(1);
				}
				else if(bgmFileName.endsWith(".wav"))
				{
					Audio.stop(2);
				}
				
				
				//System.out.println(bgmFileName+"停止");
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
			}
			
			isrun=false;
			
			state = Player.PREFETCHED;
			
			//System.out.println("关闭:"+bgmFileName+" loop:"+loops);
		}
		public void deallocate()
		{
			// if(!isinit)
			// {
				// return;
			// }
			
			// try{
				// byte[] frame = new byte[100];
				// frame[0]='$';//36
				// frame[1]='S';//83
				// Anbu.au.frame.write(frame);
				// Anbu.au.frame.flush();
				//System.out.println(bgmFileName+"停止");
			// }
			// catch(Exception e)
			// {
				// System.out.println(e.getMessage());
			// }
			// isinit=false;
			// isrun=false;
		}

		public void setLoopCount(int count)
		{
			
			//System.out.println("设置播放次数");
			//System.out.println(bgmFileName+":"+count);
			loops = count;
			
		}
		public long setMediaTime(long now)
		{
			
			return now;
		}
		public long getMediaTime()
		{
			
			return 0;
		}
		public boolean isRunning()
		{
			return isrun;
			
		}
	}
	

	private class wavPlayer extends audioplayer
	{

		private int loops = 0;

		//private long time = 0L;
		
		private boolean isrun=false;

		public wavPlayer(InputStream stream)
		{
			/* try
			{
				wavStream = AudioSystem.getAudioInputStream(stream);
				wavClip = AudioSystem.getClip();
				wavClip.open(wavStream);
				state = Player.PREFETCHED;
			}
			catch (Exception e) 
			{ 
				System.out.println("Couldn't load wav file: " + e.getMessage());
				wavClip.close();
			} */
			
			state = Player.PREFETCHED;
		}

		public void start()
		{
			if(isRunning()) { return; }
			
			/* if(wavClip.getFramePosition() >= wavClip.getFrameLength())
			{
				wavClip.setFramePosition(0);
			}
			time = wavClip.getMicrosecondPosition();
			wavClip.start();
			state = Player.STARTED;
			notifyListeners(PlayerListener.STARTED, time); */
			
			state = Player.STARTED;
			isrun=true;
		}

		public void stop()
		{
			/* wavClip.stop();
			time = wavClip.getMicrosecondPosition();
			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, time); */
			
			state = Player.PREFETCHED;
			isrun=false;
		}

		public void setLoopCount(int count)
		{
			loops = count;
			//wavClip.loop(count);
		}

		public long setMediaTime(long now)
		{
			//wavClip.setMicrosecondPosition(now);
			return now;
		}
		public long getMediaTime()
		{
			return 0;
		}

		public boolean isRunning()
		{
			return isrun;
		}
	}
	
	/* private class wavPlayer extends audioplayer
	{

		private AudioInputStream wavStream;
		private Clip wavClip;

		private int loops = 0;

		private long time = 0L;

		public wavPlayer(InputStream stream)
		{
			try
			{
				wavStream = AudioSystem.getAudioInputStream(stream);
				wavClip = AudioSystem.getClip();
				wavClip.open(wavStream);
				state = Player.PREFETCHED;
			}
			catch (Exception e) 
			{ 
				System.out.println("Couldn't load wav file: " + e.getMessage());
				wavClip.close();
			}
		}

		public void start()
		{
			if(isRunning()) { return; }
			
			if(wavClip.getFramePosition() >= wavClip.getFrameLength())
			{
				wavClip.setFramePosition(0);
			}
			time = wavClip.getMicrosecondPosition();
			wavClip.start();
			state = Player.STARTED;
			notifyListeners(PlayerListener.STARTED, time);
		}

		public void stop()
		{
			wavClip.stop();
			time = wavClip.getMicrosecondPosition();
			state = Player.PREFETCHED;
			notifyListeners(PlayerListener.STOPPED, time);
		}

		public void setLoopCount(int count)
		{
			loops = count;
			wavClip.loop(count);
		}

		public long setMediaTime(long now)
		{
			wavClip.setMicrosecondPosition(now);
			return now;
		}
		public long getMediaTime()
		{
			return wavClip.getMicrosecondPosition();
		}

		public boolean isRunning()
		{
			return wavClip.isRunning();
		}
	} */

	// Controls //

	private class midiControl implements javax.microedition.media.control.MIDIControl
	{
		public int[] getBankList(boolean custom) { return new int[]{}; }

		public int getChannelVolume(int channel) { return 0; }

		public java.lang.String getKeyName(int bank, int prog, int key) { return ""; }

		public int[] getProgram(int channel) { return new int[]{}; }

		public int[] getProgramList(int bank) { return new int[]{}; }

		public java.lang.String getProgramName(int bank, int prog) { return ""; }

		public boolean isBankQuerySupported() { return false; }

		public int longMidiEvent(byte[] data, int offset, int length) { return 0; }

		public void setChannelVolume(int channel, int volume) {  }

		public void setProgram(int channel, int bank, int program) {  }

		public void shortMidiEvent(int type, int data1, int data2) {  }
	}

	private class volumeControl implements javax.microedition.media.control.VolumeControl
	{
		private int level = 100;
		private boolean muted = false;

		public int getLevel() { return level; }

		public boolean isMuted() { return muted; }

		public int setLevel(int value) { level = value; return level; }

		public void setMute(boolean mute) { muted = mute; }
	}

	private class tempoControl implements javax.microedition.media.control.TempoControl
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
	
	public void musicFinish()
	{
		notifyListeners(PlayerListener.END_OF_MEDIA, 0);
	}
	
	private native void _start(String bgmfile,int loop);
}
