#include <jni.h>
#include <stdio.h>
#include <SDL2/SDL.h>
#include <SDL2/SDL_mixer.h>
#include <unordered_map>
#include "org_recompile_mobile_SdlMixerManager.h"

#define DEFAULT_BGM_FREQ    44100
#define DEFAULT_BGM_FORMAT  AUDIO_S16LSB
#define DEFAULT_BGM_CHAN    2
#define DEFAULT_BGM_CHUNK   4096

// 全局状态
static Mix_Music* currentMusic = NULL;
static jint currentVolume = 128;

// --- 全局变量用于保存 Java 引用 ---
static JavaVM *g_jvm = NULL; // 保存 JavaVM 引用

// Native 上下文（每个 Java PlatformPlayer 对应一个）
struct NativeContext {
    jobject jobj;           // Java 对象的全局引用
    jmethodID onCallBack;   // 回调方法 ID
    
    NativeContext() : jobj(nullptr), onCallBack(nullptr){}
};


// 全局映射表：Mix_Music指针 → NativeContext指针
static std::unordered_map<jlong, NativeContext*> g_contextMap;

// 错误处理宏
#define CHECK_SDL_ERROR(msg) \
    do { \
        const char* err = SDL_GetError(); \
        if (err && err[0] != '\0') { \
            fprintf(stderr, "SDL Error [%s]: %s\n", msg, err); \
            SDL_ClearError(); \
        } \
    } while(0)

#define CHECK_MIX_ERROR(msg) \
    do { \
        const char* err = Mix_GetError(); \
        if (err && err[0] != '\0') { \
            fprintf(stderr, "SDL_mixer Error [%s]: %s\n", msg, err); \
            Mix_ClearError(); \
        } \
    } while(0)
		
JNIEXPORT jint JNICALL  JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;
	
	g_jvm = vm;
 
    if(vm -> GetEnv((void**) &env, JNI_VERSION_10) != JNI_OK) {
       return -1;
    }
 
	if(env == NULL)
	{
		return -1;
	}

    return JNI_VERSION_10;
}

// JNI_OnUnload 中清理
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_10) == JNI_OK) {
        // 清理所有上下文
        for (auto& pair : g_contextMap) {
            NativeContext* ctx = pair.second;
            if (ctx->jobj != nullptr) {
                env->DeleteGlobalRef(ctx->jobj);
            }
            delete ctx;
        }
        g_contextMap.clear();
    }
    g_jvm=NULL;
}

bool isAttachedCurrentThread(JNIEnv** env)
{
	if(g_jvm->GetEnv((void**)env, JNI_VERSION_10) != JNI_OK) {
	    g_jvm->AttachCurrentThread((void**)env, NULL);
	    return true;
	}
	
	return false;
}


void musicFinished() {

	Mix_HookMusicFinished(NULL);

	JNIEnv *env = NULL;
	bool isAttached = isAttachedCurrentThread(&env);
	
	
	jlong musicHandle = (jlong)(intptr_t)currentMusic;
	auto it = g_contextMap.find(musicHandle);
	if(it != g_contextMap.end())
	{
		// 检查对象是否还有效
		if (env->IsSameObject(it->second->jobj, nullptr) == JNI_FALSE) {
			env->CallVoidMethod(it->second->jobj, it->second->onCallBack);
		}
	}
	
	if (isAttached) {
		g_jvm->DetachCurrentThread();
	}
}
		

/**
 * 初始化SDL2和SDL2_mixer
 */
JNIEXPORT jint JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerInit
  (JNIEnv *env, jclass cls, jint frequency, jint format, jint channels, jint chunksize) {
    
    // 初始化SDL2音频子系统
    if (SDL_Init(SDL_INIT_AUDIO) < 0) {
        CHECK_SDL_ERROR("SDL_Init");
        return -1;
    }
    
    // 打开音频设备
    if (Mix_OpenAudio(DEFAULT_BGM_FREQ, DEFAULT_BGM_FORMAT, DEFAULT_BGM_CHAN, DEFAULT_BGM_CHUNK) < 0) {
        CHECK_MIX_ERROR("Mix_OpenAudio");
        SDL_Quit();
        return -1;
    }
    
    printf("✓ SDL2_mixer已初始化 (freq=%d, format=%d, ch=%d, chunk=%d)\n", 
           frequency, format, channels, chunksize);
    return 0;
}

/**
 * 加载MIDI文件
 */
JNIEXPORT jlong JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerLoadMidi
  (JNIEnv *env, jobject obj, jstring filePath, jobject player) {
    
    if (filePath == NULL) {
        fprintf(stderr, "文件路径为空\n");
        return -1;
    }
    
    // 转换jstring为C字符串
    const char* path = env->GetStringUTFChars(filePath, NULL);
    if (path == NULL) {
        return -1;
    }
    
    printf("📂 加载MIDI文件: %s\n", path);
    
    // 加载音乐
	Mix_Music* music = Mix_LoadMUS(path);
	// 释放Java字符串
    env->ReleaseStringUTFChars(filePath, path);
	if (music == NULL) {
		CHECK_MIX_ERROR("Mix_LoadMUS");
		return -1;
	}
	jlong musicHandle = (jlong)(intptr_t)music;
    
    printf("✓ MIDI文件加载成功\n");
	
	
	
	// 检查是否已存在
    auto it = g_contextMap.find( musicHandle );
    if (it == g_contextMap.end()) {//不存在
        // 创建新的上下文
		NativeContext* ctx = new NativeContext();
		ctx->jobj = env->NewGlobalRef(player);  // 全局引用
		
		if (ctx->jobj == nullptr) {
			delete ctx;
			return -1;
		}
		
		// 获取回调方法 ID
		jclass localCls = env->GetObjectClass(player);
		if (localCls != nullptr) {
			ctx->onCallBack = env->GetMethodID(localCls, "onPlaybackComplete", "()V");
			if (ctx->onCallBack == nullptr) {
				env->ExceptionClear();  // 方法不存在，清空异常
			}
			env->DeleteLocalRef(localCls);
		}
		
		// 存入映射表（使用 jobj 作为 key）
		g_contextMap[musicHandle] = ctx;
    }
    
    return musicHandle;
}

/**
 * 加载WAV文件
 */
JNIEXPORT jlong JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerLoadWav
  (JNIEnv *env, jobject obj, jstring filePath) {
    
    if (filePath == NULL) {
        fprintf(stderr, "文件路径为空\n");
        return -1;
    }
    
    // 转换jstring为C字符串
    const char* path = env->GetStringUTFChars(filePath, NULL);
    if (path == NULL) {
        return -1;
    }
    
    printf("📂 加载WAV文件: %s\n", path);
    
    // 加载音乐
	Mix_Chunk* music = Mix_LoadWAV(path);
	// 释放Java字符串
    env->ReleaseStringUTFChars(filePath, path);
	if (music == NULL) {
		CHECK_MIX_ERROR("Mix_LoadWAV");
		return -1;
	}
	jlong musicHandle = (jlong)(intptr_t)music;
    
    printf("✓ WAV文件加载成功\n");
    
    return musicHandle;
}

/**
 * 播放音乐
 */
JNIEXPORT jint JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerPlayMusic
  (JNIEnv *env, jobject obj, jlong musicHandle, jint loops) {
    
    if (musicHandle == 0) {
        fprintf(stderr, "无效的音乐句柄\n");
        return -1;
    }
    
    Mix_Music* music = (Mix_Music*)(intptr_t)musicHandle;
	currentMusic = music;
    
    // 停止当前播放
    Mix_HaltMusic();
	
	if(loops!=-1)
	{
		Mix_HookMusicFinished(musicFinished);
	}
	else
	{
		Mix_HookMusicFinished(NULL);
	}
    
    // 播放新音乐
    if (Mix_PlayMusic(music, loops) < 0) {
		Mix_HookMusicFinished(NULL);
        CHECK_MIX_ERROR("Mix_PlayMusic");
        return -1;
    }
    
    printf("🎵 播放开始 (loops=%d)\n", loops);
    return 0;
}

/**
 * 播放音效
 */
JNIEXPORT jint JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerPlayWav
  (JNIEnv *env, jobject obj, jlong musicHandle, jint loops) {
    
    if (musicHandle == 0) {
        fprintf(stderr, "无效的音效句柄\n");
        return -1;
    }
    
    Mix_Chunk* music = (Mix_Chunk*)(intptr_t)musicHandle;
    
    // 停止当前播放
    Mix_HaltChannel(0);
	
    
    // 播放新音乐
    if (Mix_PlayChannel(0, music, loops) < 0) {
        CHECK_MIX_ERROR("Mix_PlayWav");
        return -1;
    }
    
    printf("🎵 音效播放开始 (loops=%d)\n", loops);
    return 0;
}

/**
 * 暂停音乐
 */
JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerPauseMusic
  (JNIEnv *env, jobject obj) {
    Mix_PauseMusic();
    printf("⏸ 音乐已暂停\n");
}

JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerPauseWav
  (JNIEnv *env, jobject obj) {
    Mix_Pause(0);
    printf("⏸ 音效已暂停\n");
}


/**
 * 恢复播放
 */
JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerResumeMusic
  (JNIEnv *env, jobject obj) {
    Mix_ResumeMusic();
    printf("▶ 音乐已恢复\n");
}

JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerResumeWav
  (JNIEnv *env, jobject obj) {
    Mix_Resume(0);
    printf("▶ 音效已恢复\n");
}


/**
 * 停止音乐
 */
JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerStopMusic
  (JNIEnv *env, jobject obj) {
    Mix_HaltMusic();
    printf("⏹ 音乐已停止\n");
}

JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerStopWav
  (JNIEnv *env, jobject obj) {
    Mix_HaltChannel(0);
    printf("⏹ 音效已停止\n");
}

/**
 * 检查是否正在播放
 */
JNIEXPORT jboolean JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerIsPlaying
  (JNIEnv *env, jobject obj) {
    return (Mix_PlayingMusic() == 1) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerIsPlayingWav
  (JNIEnv *env, jobject obj) {
    return Mix_Playing(0) ? JNI_TRUE : JNI_FALSE;
}

/**
 * 设置音量
 */
JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerSetVolume
  (JNIEnv *env, jobject obj, jint volume) {
	volume = 128*volume/100;
    
    if (volume < 0) volume = 0;
    if (volume > 128) volume = 128;
    
    Mix_VolumeMusic(volume);
    currentVolume = volume;
    printf("🔊 音量设置为: %d/128\n", volume);
}

JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerSetVolumeWav
  (JNIEnv *env, jobject obj, jint volume) {
	volume = 128*volume/100;
    
    if (volume < 0) volume = 0;
    if (volume > 128) volume = 128;
    
    Mix_Volume(0, volume); 
    currentVolume = volume;
    printf("🔊 音效音量设置为: %d/128\n", volume);
}

/**
 * 获取当前音量
 */
JNIEXPORT jint JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerGetVolume
  (JNIEnv *env, jobject obj) {
    return Mix_VolumeMusic(-1);  // -1表示只查询不设置
}

/**
 * 卸载音乐资源
 */
JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerFreeMusic
  (JNIEnv *env, jobject obj, jlong musicHandle) {
    
    if (musicHandle == 0) {
        return;
    }
	
	auto it = g_contextMap.find(musicHandle);
    if (it != g_contextMap.end()) {//存在
        NativeContext* ctx = it->second;
        
        // 释放全局引用
        if (ctx->jobj != nullptr) {
            env->DeleteGlobalRef(ctx->jobj);
        }
        
        delete ctx;
        g_contextMap.erase(it);
    }
    
    Mix_Music* music = (Mix_Music*)(intptr_t)musicHandle;
    Mix_FreeMusic(music);
    printf("🗑 音乐资源已释放\n");
}

JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerFreeWav
  (JNIEnv *env, jobject obj, jlong musicHandle) {
    
    if (musicHandle == 0) {
        return;
    }
    
    Mix_Chunk* music = (Mix_Chunk*)(intptr_t)musicHandle;
    Mix_FreeChunk(music);
    printf("🗑 音效资源已释放\n");
}

/**
 * 关闭SDL2_mixer
 */
JNIEXPORT void JNICALL Java_org_recompile_mobile_SdlMixerManager_sdlMixerQuit
  (JNIEnv *env, jclass cls) {
    
    // 停止所有播放
    Mix_HaltMusic();
    
    // 关闭mixer
    Mix_CloseAudio();
    
    // 退出mixer扩展
    Mix_Quit();
    
    // 退出SDL2
    SDL_Quit();
    
    printf("✓ SDL2_mixer已关闭\n");
}