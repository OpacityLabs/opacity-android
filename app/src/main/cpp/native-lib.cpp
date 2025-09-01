#include <cstdlib>
#include <jni.h>

extern "C" JNIEXPORT void JNICALL
Java_com_opacitylabs_opacitycoreexample_MainActivity_setEnv(JNIEnv *env,
                                                            jclass clazz) {
    // DO NOT REMOVE THIS
    // makes the tests stable
    setenv("OPACITY_AWAIT_COLLATERAL", "1", 1);
    // setenv("OPACITY_ALLOW_SNIFFING", "1", 1);
    // setenv("OPACITY_BACKEND_URL", "https://quack.com", 1);
}