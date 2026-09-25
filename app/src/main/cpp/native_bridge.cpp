#include "crash_handler.h"
#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <atomic>
#include <array>
#include <mutex>
#include <string>

namespace {
constexpr const char* TAG = "MK64Native";
std::atomic<bool> g_initialized{false};
std::mutex g_state_mutex;
ANativeWindow* g_window = nullptr;
int g_width = 0;
int g_height = 0;
float g_stick_x = 0.0f;
float g_stick_y = 0.0f;
std::array<bool, 16> g_buttons{};

void log_info(const char* message) {
    __android_log_write(ANDROID_LOG_INFO, TAG, message);
}

void release_window_locked() {
    if (g_window != nullptr) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }
}
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_nativeVersion(
        JNIEnv* env,
        jobject) {
    return env->NewStringUTF("mk64android-host-0.2");
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_initialize(
        JNIEnv* env,
        jobject,
        jstring rom_path) {
    if (rom_path == nullptr) return -1;
    const char* raw_path = env->GetStringUTFChars(rom_path, nullptr);
    if (raw_path == nullptr) return -2;
    std::string path(raw_path);
    env->ReleaseStringUTFChars(rom_path, raw_path);
    if (path.empty()) return -3;

    mk64::crash::install();
    g_initialized.store(true);
    log_info("Native MK64 Android host initialized");
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_attachSurface(
        JNIEnv* env,
        jobject,
        jobject surface) {
    if (!g_initialized.load()) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "attachSurface before initialize");
        return -10;
    }
    if (surface == nullptr) return -11;

    std::lock_guard<std::mutex> lock(g_state_mutex);
    release_window_locked();
    g_window = ANativeWindow_fromSurface(env, surface);
    if (g_window == nullptr) return -12;

    g_width = ANativeWindow_getWidth(g_window);
    g_height = ANativeWindow_getHeight(g_window);
    log_info("Android native game surface attached");
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_resizeSurface(
        JNIEnv*,
        jobject,
        jint width,
        jint height) {
    std::lock_guard<std::mutex> lock(g_state_mutex);
    g_width = width;
    g_height = height;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_detachSurface(
        JNIEnv*,
        jobject) {
    std::lock_guard<std::mutex> lock(g_state_mutex);
    release_window_locked();
    g_width = 0;
    g_height = 0;
    log_info("Android native game surface detached");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_setStick(
        JNIEnv*,
        jobject,
        jfloat x,
        jfloat y) {
    std::lock_guard<std::mutex> lock(g_state_mutex);
    g_stick_x = x;
    g_stick_y = y;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_setButton(
        JNIEnv*,
        jobject,
        jint button,
        jboolean pressed) {
    if (button < 0 || static_cast<size_t>(button) >= g_buttons.size()) return;
    std::lock_guard<std::mutex> lock(g_state_mutex);
    g_buttons[static_cast<size_t>(button)] = pressed == JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_releaseAllButtons(
        JNIEnv*,
        jobject) {
    std::lock_guard<std::mutex> lock(g_state_mutex);
    g_buttons.fill(false);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_shutdown(
        JNIEnv*,
        jobject) {
    {
        std::lock_guard<std::mutex> lock(g_state_mutex);
        release_window_locked();
        g_buttons.fill(false);
        g_stick_x = 0.0f;
        g_stick_y = 0.0f;
    }
    if (g_initialized.exchange(false)) {
        log_info("Native MK64 Android host shutdown");
    }
}
