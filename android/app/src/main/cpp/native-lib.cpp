#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>

#include "jpegCtrl.hpp"
#include "com_munger_stereocamera_service_PhotoProcessor.h"
#include "util.h"
#include "SplitCompositeImageStream.h"
#include "AnaglyphCompositeImageStream.h"
#include "GreenMagentaCompositeImageStream.h"
#include "RedCyanCompositeImageStream.h"
#include "Image.h"
#include "PreProcessor.h"

PreProcessor* preProcessor;
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

JNIEXPORT void JNICALL Java_com_munger_stereocamera_service_PhotoProcessor_initN
        (JNIEnv * env, jobject jthis, jstring jcachePath)
{
    srand(time(NULL));
    cachePath = getStringFromNative(env, jcachePath);
    preProcessor = new PreProcessor();
    image = 0;
}

JNIEXPORT void JNICALL Java_com_munger_stereocamera_service_PhotoProcessor_setProcessorType
        (JNIEnv * env, jobject jthis, jint jtype)
{
    if (image != nullptr)
    {
        delete image;
        image = 0;
    }

    auto type = (CompositeImageType) jtype;
    switch (type)
    {
        case SPLIT:
        default:
            //image = new SplitCompositeImage();
            image = new SplitCompositeImageStream();
            break;
        case GREEN_MAGENTA:
            image = new GreenMagentaCompositeImageStream();
            break;
        case RED_CYAN:
            image = new RedCyanCompositeImageStream();
            break;
    }
}

/*
 * Class:     com_munger_stereocamera_service_PhotoProcessor
 * Method:    setImageN
 * Signature: (Z[IIFII)V
 */
JNIEXPORT void JNICALL Java_com_munger_stereocamera_service_PhotoProcessor_setImageN
        (JNIEnv * env, jobject jthis, jboolean isRight, jstring jpath, jint orientation, jfloat zoom)
{
    Side side = (isRight) ? RIGHT : LEFT;
    Image* target = preProcessor->getImage(side);
    const char* jpegpath = getStringFromNative(env, jpath);
    target->init(orientation, zoom, jpegpath, cachePath);
}


JNIEXPORT void JNICALL Java_com_munger_stereocamera_service_PhotoProcessor_preProcessN
        (JNIEnv *env, jobject jthis, jboolean flip)
{
    preProcessor->process(flip);
}

/*
 * Class:     com_munger_stereocamera_service_PhotoProcessor
 * Method:    processN
 * Signature: (ZZ)V
 */
JNIEXPORT void JNICALL Java_com_munger_stereocamera_service_PhotoProcessor_processN
        (JNIEnv * env, jobject jthis, jboolean growToMaxDim, jstring jtargetPath)
{
    image->setImages(preProcessor);
    const char* outpath = getStringFromNative(env, jtargetPath);
    image->combineImages(growToMaxDim, outpath);
    delete[] outpath;
}

/*
 * Class:     com_munger_stereocamera_service_PhotoProcessor
 * Method:    cleanUpN
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_munger_stereocamera_service_PhotoProcessor_cleanUpN
        (JNIEnv * env, jobject jthis)
{
    delete image;
    delete preProcessor;
    delete[] cachePath;
}
