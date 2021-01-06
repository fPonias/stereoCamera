
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <string.h>
#include <math.h>
#include "jpegCtrl.hpp"

#include "Image.h"
#include "util.h"

const char* Image::getJpegPath()
{
    return jpegpath;
}

const char* Image::getRawPath()
{
    return rawpath;
}

const char* Image::getProcPath()
{
    return procpath;
}

int Image::getOrientation()
{
    return orientation;
}

void Image::setOrientation(int orient)
{
    orient %= 4;
    orientation = orient;
}

float Image::getZoom()
{
    return zoom;
}

long Image::getWidth()
{
    return width;
}

long Image::getHeight()
{
    return height;
}

long Image::getTargetDim()
{
    return targetDim;
}

void Image::setJpegPath(const char* path)
{
    strcpy(this->jpegpath, path);
    
}

void Image::setCachePath(const char* cachePath)
{
    strcpy(this->cachepath, cachePath);
    
    long r = rand();
    
    Util::getPath(cachePath, r, "-raw", rawpath);
    Util::getPath(cachePath, r, "-proc", procpath);
}

void Image::calcTargetDim()
{
    targetDim = (long) fminl(width, height);
    targetDim /= zoom;
}

struct Pixel* Image::process()
{
    size_t sz = (size_t) targetDim * (size_t) targetDim;
    Pixel * data = new Pixel[sz];
    
    long unzoomedTarget = (long) (targetDim * zoom);
    long maxDim = (height > width) ? height : width;
    long cmargin = (width - unzoomedTarget) / 2;
    long rmargin = (height - unzoomedTarget) / 2;
    long zoomMargin = (unzoomedTarget - targetDim) / 2;
    
    FILE* file = fopen(rawpath, "rb");
    size_t buf_sz = (size_t) width;
    //Pixel buffer[buf_sz];
    Pixel* buffer = (Pixel*) malloc(sizeof(Pixel) * buf_sz);
    size_t jsamp_sz = sizeof(Pixel);

    long cMin = cmargin + zoomMargin;
    long cMax = cMin + targetDim;
    long rMin = rmargin + zoomMargin;
    long rMax = rMin + targetDim;

    long dstC, dstR, idx;
    for (long r = 0; r < height; r++)
    {
        size_t read = fread(buffer, jsamp_sz, buf_sz, file);
        
        if (read < buf_sz)
        {
            fclose(file);
            return NULL;
        }
        
        for (long c = cMin; c < cMax; c++)
        {
            dstC = c - zoomMargin - cmargin;
            dstR = r - zoomMargin - rmargin;

            if (dstR < 0 || dstR >= targetDim)
                break;

            if (dstC < 0 || dstC >= targetDim)
                continue;

            rotateDestination(orientation, targetDim, &dstC, &dstR);
            idx = dstR * targetDim + dstC;
            memcpy(data + idx, buffer + c, jsamp_sz);
        }
    }
    
    fclose(file);
    free(buffer);
    return data;
}

void Image::rotateDestination(int orientation, long dim, long* r, long* c)
{
    dim--;
    long tmp;
    switch (orientation)
    {
        case 0:
            return;
        case 2:
            *r = dim - *r;
            *c = dim - *c;
            return;
        case 1:
            tmp = *r;
            *r = *c;
            *c = tmp;
            return;
        case 3:
        default:
            tmp = *r;
            *r = dim - *c;
            *c = dim - tmp;
            return;
    }
}

void Image::processJpeg()
{
    if (!iniited)
        return;
    
    ImageData data;
    JpegCtrl::read_JPEG_file(jpegpath, &data);
    
    width = data.width;
    height = data.height;
    
    printf("processing photo with dims %ldx%ld\n", width, height);
    
    Util::writeToCache(rawpath, data.data, width, height);
    delete[] data.data;
    
    calcTargetDim();
    Pixel* newdata = process();
    
    if (newdata == NULL)
        return;
    
    Util::writeToCache(procpath, newdata, targetDim, targetDim);
    delete[] newdata;
}

Image::Image()
{
    iniited = false;
}

void Image::init(int orientation, float zoom, const char* jpegSrc, const char* cachePath)
{
    iniited = true;
    
    this->orientation = orientation;
    this->zoom = fmax(1.0f, zoom); //no zooms outside the image bounds
    
    setJpegPath(jpegSrc);
    setCachePath(cachePath);
}

Image::~Image()
{
    remove(cachepath);
    remove(rawpath);
    remove(procpath);
}
