#include <jni.h>
#include <malloc.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#include "com_munger_stereocamera_utility_PhotoProcessor.h"

struct Image
{
    const char* path;
    char* tmpPath;
    jint* data;
    jboolean isRight;
    int orientation;
    float zoom;
    long width;
    long height;
    long targetDim;
};

const char* cachePath;
struct Image left;
struct Image right;
long targetDim;
char* targetPath;

JNIEXPORT void JNICALL Java_com_munger_stereocamera_utility_PhotoProcessor_initN
        (JNIEnv * env, jobject jthis, jstring jpath)
{
    cachePath = (*env)->GetStringUTFChars(env, jpath, NULL);
}

/*
 * Class:     com_munger_stereocamera_utility_PhotoProcessor
 * Method:    setImageN
 * Signature: (Z[IIFII)V
 */
JNIEXPORT void JNICALL Java_com_munger_stereocamera_utility_PhotoProcessor_setImageN
        (JNIEnv * env, jobject jthis, jboolean isRight, jstring path, jint orientation, jfloat zoom, jint width, jint height)
{
    struct Image* target;
    if (isRight)
        target = &right;
    else
        target = &left;

    target->isRight = isRight;
    target->path = (*env)->GetStringUTFChars(env, path, NULL);
    target->orientation = orientation;
    target->zoom = zoom;
    target->width = width;
    target->height = height;
}

void calculateTargetDim(jboolean growToMaxDim)
{
    long unzoomedTarget = (left.height > left.width) ? left.width : left.height;
    left.targetDim = (long)((float) unzoomedTarget / left.zoom);
    unzoomedTarget = (right.height > right.width) ? right.width : right.height;
    right.targetDim = (long)((float) unzoomedTarget / right.zoom);

    if (growToMaxDim)
        targetDim = (left.targetDim > right.targetDim) ? left.targetDim : right.targetDim;
    else
        targetDim = (left.targetDim < right.targetDim) ? left.targetDim : right.targetDim;
}

void setTargetPath()
{
    const char* postfix = "-final";
    size_t pathSz = strlen(left.path);
    size_t postSz = strlen(postfix);
    size_t sz = pathSz + postSz;
    targetPath = malloc(sz + 1);
    memcpy(targetPath, left.path, pathSz);
    memcpy(targetPath + pathSz, postfix, postSz);
    targetPath[sz] = 0;
}

void extractSubImage(struct Image* img)
{
    size_t sz = (size_t) img->targetDim * (size_t) img->targetDim;
    img->data = malloc(sizeof(jint) * sz);

    long unzoomedTarget = (long) (img->targetDim * img->zoom);
    long maxDim = (img->height > img->width) ? img->height : img->width;
    long margin = (maxDim - unzoomedTarget) / 2;
    long zoomMargin = (unzoomedTarget - img->targetDim) / 2;

    FILE* file = fopen(img->path, "r");
    size_t buf_sz = (size_t) img->width;
    jint buffer[buf_sz];
    size_t jint_sz = sizeof(jint);
    long xMin = margin + zoomMargin;
    long xMax = xMin + img->targetDim;
    long dstX, dstY, idx, targetidx;
    for (long y = 0; y < img->height; y++)
    {
        size_t read = fread(buffer, jint_sz, buf_sz, file);

        if (read < buf_sz)
        {
            fclose(file);
            return;
        }

        for (long x = xMin; x < xMax; x++)
        {
            switch (img->orientation)
            {
                case 0:
                    dstX = unzoomedTarget - y - zoomMargin;
                    dstY = x - margin - zoomMargin;
                    break;
                case 1:
                    dstX = unzoomedTarget - x - margin - zoomMargin;
                    dstY = unzoomedTarget - y - zoomMargin;
                    break;
                case 2:
                    dstX = y - zoomMargin;
                    dstY = unzoomedTarget - x - margin - zoomMargin;
                    break;
                case 3:
                default:
                    dstX = x - margin - zoomMargin;
                    dstY = y - zoomMargin;
                    break;
            }

            if (dstX >= 0 && dstX < img->targetDim && dstY >= 0 && dstY < img->targetDim)
            {
                idx = dstY * img->targetDim + dstX;
                img->data[idx] = buffer[x];
            }
        }
    }

    fclose(file);
}

void writeTmpToCache(struct Image* img, const char* postfix)
{
    size_t pathSz = strlen(img->path);
    size_t postSz = strlen(postfix);
    size_t sz = pathSz + postSz;
    img->tmpPath = malloc(sz + 1);
    memcpy(img->tmpPath, img->path, pathSz);
    memcpy(img->tmpPath + pathSz, postfix, postSz);
    img->tmpPath[sz] = 0;

    FILE* file = fopen(img->tmpPath, "w");
    size_t dataSz = (size_t) img->targetDim * (size_t) img->targetDim;
    size_t jintSz = sizeof(jint);
    size_t total = 0;
    size_t written = 1;

    while (total < dataSz && written > 0)
    {
        written = fwrite(img->data + total, jintSz, dataSz, file);
        total += written;
    }

    fclose(file);
}

void straightCopy(jint** out, struct Image* target, size_t offset)
{
    size_t rowWidth = (size_t) targetDim * 2;
    FILE* file = fopen(target->tmpPath, "r");

    size_t leftW = (size_t) target->targetDim;
    jint buffer[leftW];
    size_t jintSz = sizeof(jint);

    size_t idx = 0;
    jint* out_ptr = *out;

    for (size_t dstRow = 0; dstRow < target->targetDim; dstRow++)
    {
        size_t read = fread(buffer, jintSz, (size_t) target->targetDim, file);

        if (read < target->targetDim)
        {
            fclose(file);
            return;
        }

        idx = dstRow * rowWidth + offset;
        out_ptr = *out + idx;


        memcpy(out_ptr, buffer, leftW * jintSz);
    }

    fclose(file);
}

void scaledCopy(jint** out, struct Image* target, size_t offset)
{
    size_t rowWidth = (size_t) targetDim * 2;
    FILE* file = fopen(target->tmpPath, "r");

    size_t leftW = (size_t) target->targetDim;
    jint buffer[leftW];
    size_t jintSz = sizeof(jint);

    float ratio = (float) targetDim / (float) target->targetDim;
    size_t srcRow = 0;
    char first = 1;

    size_t idx = 0;
    jint* out_ptr = *out;
    size_t read;


    for (size_t dstRow = 0; dstRow < targetDim; dstRow++)
    {
        size_t newRow = (size_t) fmax(0, fmin(lround(dstRow / ratio), target->targetDim - 1));
        if (first || newRow > srcRow)
        {
            first = 0;
            read = fread(buffer, jintSz, (size_t) target->targetDim, file);
            srcRow = newRow;

            if (read < target->targetDim)
            {
                fclose(file);
                return;
            }
        }

        idx = dstRow * rowWidth + offset;
        out_ptr = *out + idx;
        size_t srcCol = 0;

        for (size_t dstCol = 0; dstCol < targetDim; dstCol++)
        {
            srcCol = (size_t) fmax(0, fmin(lround(dstCol / ratio), target->targetDim - 1));
            *(out_ptr + dstCol) = buffer[srcCol];
        }
    }

    fclose(file);
}

void copyTmp(jint** out, struct Image* target, size_t offset)
{
    float ratio = (float) targetDim / (float) target->targetDim;
    if (ratio > 0.995f && ratio < 1.005f)
        straightCopy(out, target, offset);
    else
        scaledCopy(out, target, offset);
}

void saveFinal(jint** out)
{
    FILE* file = fopen(targetPath, "w");
    size_t jintSz = sizeof(jint);
    size_t totalSz = (size_t) targetDim * 2 * (size_t) targetDim;
    size_t total = 0;
    size_t written = 1;

    while (total < totalSz && written > 0)
    {
        written = fwrite(*out + total, sizeof(jint), totalSz, file);
        total += written;
    }

    fclose(file);
}

void combineImages()
{
    size_t finalSz = (size_t) targetDim * (size_t) targetDim * 2;
    jint* out = malloc(sizeof(jint) * finalSz);

    copyTmp(&out, &left, 0);
    copyTmp(&out, &right, (size_t) targetDim);
    saveFinal(&out);

    free(out);
}

/*
 * Class:     com_munger_stereocamera_utility_PhotoProcessor
 * Method:    processN
 * Signature: (ZZ)V
 */
JNIEXPORT void JNICALL Java_com_munger_stereocamera_utility_PhotoProcessor_processN
        (JNIEnv * env, jobject jthis, jboolean growToMaxDim, jboolean flip)
{
    setTargetPath();
    calculateTargetDim(growToMaxDim);
    extractSubImage(&left);
    writeTmpToCache(&left, "-L");
    free(left.data);
    left.data = 0;

    extractSubImage(&right);
    writeTmpToCache(&right, "-R");
    free(right.data);
    right.data = 0;

    combineImages();
}


/*
 * Class:     com_munger_stereocamera_utility_PhotoProcessor
 * Method:    getProcessedPathN
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_munger_stereocamera_utility_PhotoProcessor_getProcessedPathN
        (JNIEnv * env, jobject jthis)
{
    return (*env)->NewStringUTF(env, targetPath);
}

/*
 * Class:     com_munger_stereocamera_utility_PhotoProcessor
 * Method:    getProcessedDimension
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_munger_stereocamera_utility_PhotoProcessor_getProcessedDimension
        (JNIEnv * env, jobject jthis)
{
    return (jint) targetDim;
}

/*
 * Class:     com_munger_stereocamera_utility_PhotoProcessor
 * Method:    cleanUpN
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_munger_stereocamera_utility_PhotoProcessor_cleanUpN
        (JNIEnv * env, jobject jthis)
{
    free(targetPath);

    if (left.data != 0)
    {
        free(left.data);
        left.data = 0;
    }

    if (left.tmpPath != 0)
    {
        remove(left.tmpPath);
        free(left.tmpPath);
        left.tmpPath = 0;
    }

    if (right.data != 0)
    {
        free(right.data);
        right.data = 0;
    }

    if (right.tmpPath != 0)
    {
        remove(right.tmpPath);
        free(right.tmpPath);
        right.tmpPath = 0;
    }
}
