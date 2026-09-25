#include "crash_handler.h"

#include <android/log.h>
#include <csignal>
#include <cstdio>
#include <cstdlib>
#include <dlfcn.h>
#include <exception>
#include <typeinfo>
#include <unistd.h>
#include <unwind.h>

namespace mk64::crash {
namespace {

constexpr int kSignals[] = {SIGSEGV, SIGBUS, SIGFPE, SIGILL, SIGABRT};
struct sigaction g_previous[32];

struct Backtrace {
    static constexpr int kMax = 24;
    int count = 0;
    uintptr_t pc[kMax] = {};
};

_Unwind_Reason_Code unwind_cb(_Unwind_Context* ctx, void* data) {
    auto* bt = static_cast<Backtrace*>(data);
    if (bt->count >= Backtrace::kMax) return _URC_END_OF_STACK;
    uintptr_t pc = _Unwind_GetIP(ctx);
    if (pc != 0) bt->pc[bt->count++] = pc;
    return _URC_NO_REASON;
}

const char* signal_name(int sig) {
    switch (sig) {
        case SIGSEGV: return "SIGSEGV";
        case SIGBUS: return "SIGBUS";
        case SIGFPE: return "SIGFPE";
        case SIGILL: return "SIGILL";
        case SIGABRT: return "SIGABRT";
        default: return "UNKNOWN";
    }
}

void dump_backtrace() {
    Backtrace bt;
    _Unwind_Backtrace(unwind_cb, &bt);
    for (int i = 0; i < bt.count; ++i) {
        Dl_info info{};
        if (dladdr(reinterpret_cast<void*>(bt.pc[i]), &info) && info.dli_fname) {
            __android_log_print(
                ANDROID_LOG_ERROR,
                "MK64Crash",
                "#%02d pc=%p lib=%s base=%p",
                i,
                reinterpret_cast<void*>(bt.pc[i]),
                info.dli_fname,
                info.dli_fbase
            );
        } else {
            __android_log_print(
                ANDROID_LOG_ERROR,
                "MK64Crash",
                "#%02d pc=%p",
                i,
                reinterpret_cast<void*>(bt.pc[i])
            );
        }
    }
}

void handler(int sig, siginfo_t* info, void*) {
    if (sig >= 0 && sig < 32) {
        sigaction(sig, &g_previous[sig], nullptr);
    }

    __android_log_print(
        ANDROID_LOG_ERROR,
        "MK64Crash",
        "FATAL %s code=%d address=%p tid=%d",
        signal_name(sig),
        info ? info->si_code : 0,
        info ? info->si_addr : nullptr,
        gettid()
    );

    dump_backtrace();

    sigset_t mask;
    sigemptyset(&mask);
    sigaddset(&mask, sig);
    sigprocmask(SIG_UNBLOCK, &mask, nullptr);
    raise(sig);
    _exit(127);
}

void terminate_handler() {
    const char* what = "unknown";
    const char* type = "unknown";

    if (auto current = std::current_exception()) {
        try {
            std::rethrow_exception(current);
        } catch (const std::exception& e) {
            what = e.what();
            type = typeid(e).name();
        } catch (...) {
            what = "non-std exception";
            type = "non-std";
        }
    }

    __android_log_print(
        ANDROID_LOG_ERROR,
        "MK64Crash",
        "FATAL std::terminate type=%s what=%s",
        type,
        what
    );
    std::abort();
}

}

void install() {
    static bool installed = false;
    if (installed) return;
    installed = true;

    struct sigaction action{};
    action.sa_sigaction = handler;
    action.sa_flags = SA_SIGINFO;
    sigemptyset(&action.sa_mask);

    for (int sig : kSignals) {
        if (sig >= 0 && sig < 32) {
            sigaction(sig, &action, &g_previous[sig]);
        }
    }

    std::set_terminate(terminate_handler);
    __android_log_write(ANDROID_LOG_INFO, "MK64Crash", "Native crash handler installed");
}

}
