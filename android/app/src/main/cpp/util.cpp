//
// Created by hallmarklabs on 3/17/18.
//

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "util.h"

void Util::getPath(const char* root, long id, const char* postfix, char* out)
{
    size_t pathSz = strlen(root);
    size_t postSz = strlen(postfix);
    size_t sz = pathSz + 18 + postSz;
    sprintf(out, "%s/%lx%s", root, id, postfix);
}

void Util::getPath(const char* root, const char* postfix, char* out)
{
    size_t pathSz = strlen(root);
    size_t postSz = strlen(postfix);
    size_t sz = pathSz + postSz;
    sprintf(out, "%s%s", root, postfix);
}

void Util::writeToCache(const char* path, Pixel* data, long width, long height)
{
    FILE* file = fopen(path, "wb");
    size_t dataSz = (size_t) width * (size_t) height;
    size_t jsampSz = sizeof(Pixel);
    size_t total = 0;
    size_t written = 1;
    Pixel* row_ptr;

    while (total < dataSz && written > 0)
    {
        row_ptr = data + total;
        written = fwrite(row_ptr, jsampSz, dataSz, file);
        total += written;
    }

    fclose(file);
}
