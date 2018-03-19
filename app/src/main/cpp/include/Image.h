//
// Created by hallmarklabs on 3/17/18.
//

#ifndef STEREOCAMERA_PROCESSOR_H
#define STEREOCAMERA_PROCESSOR_H

#include "jpegCtrl.h"

class Image
{
private:
    char* cachepath;
    char* jpegpath;
    char* rawpath;
    char* procpath;

    int orientation;
    float zoom;
    long width;
    long height;
    long targetDim;

    bool iniited;

    void setJpegPath(const char* path);
    void setCachePath(const char* path);
    void calcTargetDim();
    struct Pixel* process();
public:
    Image();
    ~Image();

    const char* getJpegPath();
    const char* getRawPath();
    const char* getProcPath();

    int getOrientation();
    float getZoom();
    long getWidth();
    long getHeight();
    long getTargetDim();

    void init(int orientation, float zoom, const char* jpegSrc, const char* cachePath);
    void processJpeg();
};

#endif //STEREOCAMERA_PROCESSOR_H
