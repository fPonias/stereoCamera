
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
    
    long r = random();
    
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
    long margin = (maxDim - unzoomedTarget) / 2;
    long zoomMargin = (unzoomedTarget - targetDim) / 2;
    
    FILE* file = fopen(rawpath, "rb");
    size_t buf_sz = (size_t) width;
    Pixel buffer[buf_sz];
    size_t jsamp_sz = sizeof(Pixel);
    long xMin = margin + zoomMargin;
    long xMax = xMin + targetDim;
    long dstX, dstY, idx;
    for (long y = 0; y < height; y++)
    {
        size_t read = fread(buffer, jsamp_sz, buf_sz, file);
        
        if (read < buf_sz)
        {
            fclose(file);
            return NULL;
        }
        
        for (long x = xMin; x < xMax; x++)
        {
            switch (orientation)
            {
                case 0:
                    dstX = unzoomedTarget - y - zoomMargin;
                    dstY = x - margin - zoomMargin;
                    break;
                case 1:
                    dstX = unzoomedTarget - x + margin - zoomMargin;
                    dstY = unzoomedTarget - y - zoomMargin;
                    break;
                case 2:
                    dstX = y - zoomMargin;
                    dstY = unzoomedTarget - x + margin - zoomMargin;
                    break;
                case 3:
                default:
                    dstX = x - margin - zoomMargin;
                    dstY = y - zoomMargin;
                    break;
            }
            
            if (dstX >= 0 && dstX < targetDim && dstY >= 0 && dstY < targetDim)
            {
                idx = dstY * targetDim + dstX;
                memcpy(data + idx, buffer + x, jsamp_sz);
            }
        }
    }
    
    fclose(file);
    return data;
}

void Image::processJpeg()
{
    if (!iniited)
        return;
    
    JpegCtrl::ImageData data;
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
    this->zoom = zoom;
    
    setJpegPath(jpegSrc);
    setCachePath(cachePath);
}

Image::~Image()
{
    remove(cachepath);
    remove(rawpath);
    remove(procpath);
}
