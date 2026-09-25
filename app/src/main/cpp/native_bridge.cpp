#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <string>

namespace {
constexpr const char* TAG = "MK64Native";
std::atomic<bool> g_initialized{false};

void log_info(const char* message) {
    __android_log_write(ANDROID_LOG_INFO, TAG, message);
}
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_nativeVersion(
        JNIEnv* env,
        jobject /* thiz */) {
    return env->NewStringUTF("mk64android-bootstrap-0.1");
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_initialize(
        JNIEnv* env,
        jobject /* thiz */,
        jstring rom_path) {
    if (rom_path == nullptr) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "ROM path is null");
        return -1;
    }

    const char* raw_path = env->GetStringUTFChars(rom_path, nullptr);
    if (raw_path == nullptr) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "Unable to read ROM path");
        return -2;
    }

    std::string path(raw_path);
    env->ReleaseStringUTFChars(rom_path, raw_path);

    if (path.empty()) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "ROM path is empty");
        return -3;
    }

    log_info("Native MK64 runtime bootstrap initialized");
    g_initialized.store(true);
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_shutdown(
        JNIEnv* /* env */,
        jobject /* thiz */) {
    if (g_initialized.exchange(false)) {
        log_info("Native MK64 runtime bootstrap shutdown");
    }
}
