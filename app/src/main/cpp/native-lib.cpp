#include <jni.h>
#include <stdio.h>
#include <string.h>
#include "jpegCtrl.h"

#include "com_munger_stereocamera_utility_PhotoProcessor.h"
#include "util.h"
#include "CompositeImage.h"
#include "Image.h"

CompositeImage* image;
const char* cachePath;

const char* getStringFromNative(JNIEnv* env, jstring jstr)
{
    const char* str = env->GetStringUTFChars(jstr, NULL);
    size_t sz = strlen(str);
    char* ret = new char[sz];
    strcpy(ret, str);
    env->ReleaseStringUTFChars(jstr, str);

    return ret;
}

JNIEXPORT void JNICALL Java_com_munger_stereocamera_utility_PhotoProcessor_initN
        (JNIEnv * env, jobject jthis, jstring jcachePath)
{
    cachePath = getStringFromNative(env, jcachePath);
    image = new CompositeImage();
}

/*
 * Class:     com_munger_stereocamera_utility_PhotoProcessor
 * Method:    setImageN
 * Signature: (Z[IIFII)V
 */
JNIEXPORT void JNICALL Java_com_munger_stereocamera_utility_PhotoProcessor_setImageN
        (JNIEnv * env, jobject jthis, jboolean isRight, jstring jpath, jint orientation, jfloat zoom)
{
    Side side = (isRight) ? RIGHT : LEFT;
    Image* target = image->getImage(side);
    const char* jpegpath = getStringFromNative(env, jpath);
    target->init(orientation, zoom, jpegpath, cachePath);
}

/*
 * Class:     com_munger_stereocamera_utility_PhotoProcessor
 * Method:    processN
 * Signature: (ZZ)V
 */
JNIEXPORT void JNICALL Java_com_munger_stereocamera_utility_PhotoProcessor_processN
        (JNIEnv * env, jobject jthis, jboolean growToMaxDim, jboolean flip, jstring jtargetPath)
{
    const char* outpath = getStringFromNative(env, jtargetPath);
    image->combineImages(growToMaxDim, outpath);
    delete[] outpath;
}

/*
 * Class:     com_munger_stereocamera_utility_PhotoProcessor
 * Method:    cleanUpN
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_munger_stereocamera_utility_PhotoProcessor_cleanUpN
        (JNIEnv * env, jobject jthis)
{
    delete image;
    delete[] cachePath;
}
