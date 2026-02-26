package org.recompile.mobile;
import org.recompile.mobile.PlatformPlayer;

public class SdlMixerManager{
    // ========== Native方法声明 ==========
    
    /**
     * 初始化SDL2和SDL2_mixer
     * @param frequency 采样率 (如44100)
     * @param format 音频格式 (如SDL_AUDIO_S16SYS)
     * @param channels 声道数 (2=立体声)
     * @param chunksize 音频块大小 (如2048)
     * @return 0=成功, -1=失败
     */
    public static native int sdlMixerInit(int frequency, int format, int channels, int chunksize);
    
    /**
     * 加载MIDI文件
     * @param filePath MIDI文件路径
     * @return 音乐句柄ID (>=0), -1=失败
     */
    public native long sdlMixerLoadMidi(String filePath, PlatformPlayer.sdlPlayer player);
	
	public native long sdlMixerLoadWav(String filePath);
    
    /**
     * 播放音乐
     * @param musicHandle 音乐句柄
     * @param loops 循环次数 (-1=无限循环)
     * @return 0=成功, -1=失败
     */
    public native int sdlMixerPlayMusic(long musicHandle, int loops);
	
	public native int sdlMixerPlayWav(long musicHandle, int loops);
    
    /**
     * 暂停音乐
     */
    public native void sdlMixerPauseMusic();//对应stop
	
	public native void sdlMixerPauseWav();
    
    /**
     * 恢复播放
     */
    public native void sdlMixerResumeMusic();
	
	public native void sdlMixerResumeWav();
    
    /**
     * 停止音乐
     */
    public native void sdlMixerStopMusic();
	
	public native void sdlMixerStopWav();
    
    /**
     * 检查是否正在播放
     * @return true=正在播放
     */
    public native boolean sdlMixerIsPlaying();
	public native boolean sdlMixerIsPlayingWav();
    
    /**
     * 设置音量 (0-128)
     * @param volume 音量值
     */
    public native void sdlMixerSetVolume(int volume);
	public native void sdlMixerSetVolumeWav(int volume);
    
    /**
     * 获取当前音量
     * @return 音量值 (0-128)
     */
    public native int sdlMixerGetVolume();
    
    /**
     * 卸载音乐资源
     * @param musicHandle 音乐句柄
     */
    public native void sdlMixerFreeMusic(long musicHandle);
	
	public native void sdlMixerFreeWav(long musicHandle);
    
    /**
     * 关闭SDL2_mixer
     */
    public static native void sdlMixerQuit();
    
    // ========== Java封装方法 ==========
    
    private static boolean initialized = false;
    private long currentMusicHandle = 0;
	private int loops = 0;
    
    /**
     * 全局初始化音频系统
     */
    public static boolean init() {
        if (initialized) {
            System.out.println("⚠ [java]已经初始化过了");
            return true;
        }
        
        // SDL_AUDIO_S16SYS = 32784
        int result = sdlMixerInit(44100, 32784, 2, 4096);
        if (result == 0) {
            initialized = true;
            System.out.println("✓ [java]SDL2_mixer初始化成功");
            return true;
        } else {
            System.err.println("❌ [java]SDL2_mixer初始化失败");
            return false;
        }
    }
    
    /**
     * 播放MIDI文件
     */
    public boolean playMidi(String filePath, int loops) {//对应start
        if (!initialized) {
            System.err.println("❌ [java]未初始化，请先调用init()");
            return false;
        }
        
        if(currentMusicHandle==0)
		{
			currentMusicHandle = sdlMixerLoadMidi(filePath, null);
		}
        
        if (currentMusicHandle == -1) {
            System.err.println("❌ [java]加载MIDI文件失败: " + filePath);
            return false;
        }
        
        int result = sdlMixerPlayMusic(currentMusicHandle, loops);
        if (result == 0) {
            System.out.println("🎵 开始播放: " + filePath);
            return true;
        } else {
            System.err.println("❌ 播放失败");
            return false;
        }
    }
	
	public void deallocate()//释放内存
	{
		if (currentMusicHandle != 0) {
			sdlMixerStopMusic();//先close
            sdlMixerFreeMusic(currentMusicHandle);//再释放
            currentMusicHandle = 0;
        }
	}
    
    /**
     * 全局清理退出
     */
    public static void shutdown() {
		if(initialized)
		{
			sdlMixerQuit();
			initialized = false;
			
		}
        System.out.println("✓ [java]SDL2_mixer已关闭");
    }
    
    // ========== 测试主方法 ==========
    
    public static void main(String[] args) {
        SdlMixerManager manager = new SdlMixerManager();
        
        if (!manager.init()) {
            System.exit(1);
        }
        
        // 播放测试
        if (args.length > 0) {
            manager.playMidi(args[0], 0);
            
            // 等待播放完成
            while (manager.sdlMixerIsPlaying()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        manager.shutdown();
    }
}