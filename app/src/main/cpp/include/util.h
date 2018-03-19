//
// Created by hallmarklabs on 3/17/18.
//

#ifndef STEREOCAMERA_NATIVE_LIB_H
#define STEREOCAMERA_NATIVE_LIB_H

#include <jni.h>
#include "jpeg-9c/jpeglib.h"
#include "Image.h"

class Util
{
public:
    static void getPath(const char* root, long id, const char* postfix, char** out);
    static void getPath(const char *root, const char *postfix, char **out);
    static void writeToCache(const char *path, struct Pixel *data, long width, long height);
};

#endif //STEREOCAMERA_NATIVE_LIB_H
