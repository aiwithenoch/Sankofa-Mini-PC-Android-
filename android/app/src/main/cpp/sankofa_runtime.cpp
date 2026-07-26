#include <jni.h>
#include <sstream>
#include <string>

namespace {
std::string architecture_name() {
#if defined(__aarch64__)
    return "arm64-v8a";
#elif defined(__arm__)
    return "armeabi-v7a";
#elif defined(__x86_64__)
    return "x86_64";
#elif defined(__i386__)
    return "x86";
#else
    return "unknown";
#endif
}

bool neon_available() {
#if defined(__ARM_NEON) || defined(__ARM_NEON__)
    return true;
#else
    return false;
#endif
}
}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_sankofa_minipc_NativeRuntime_runtimeInfo(
    JNIEnv* env,
    jobject /* this */
) {
    std::ostringstream out;
    out << "native=ready"
        << ";arch=" << architecture_name()
        << ";neon=" << (neon_available() ? "yes" : "no")
        << ";colibri=not-linked";
    const std::string result = out.str();
    return env->NewStringUTF(result.c_str());
}
