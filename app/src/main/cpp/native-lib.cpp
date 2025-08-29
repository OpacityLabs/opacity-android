#include <cstdlib>
#include <jni.h>

extern "C" JNIEXPORT void JNICALL
Java_com_opacitylabs_opacitycoreexample_MainActivity_setEnv(JNIEnv *env,
                                                            jclass clazz) {
    // setenv("OPACITY_ALLOW_SNIFFING", "1", 1);
    // setenv("OPACITY_BACKEND_URL", "https://quack.com", 1);
}