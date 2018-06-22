//
//  ImageProcessor.cpp
//  stereoCamera
//
//  Created by hallmarklabs on 6/20/18.
//  Copyright © 2018 cody. All rights reserved.
//

#include "ImageProcessor.hpp"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>

#include "jpegCtrl.hpp"
#include "util.h"
#include "SplitCompositeImage.h"
#include "GreenMagentaCompositeImage.h"
#include "RedCyanCompositeImage.h"
#include "Image.h"

CompositeImage* image;
const unsigned char* cachePath;

void initN(const unsigned char* jcachePath)
{
    srand(time(NULL));
    cachePath = jcachePath;
    image = 0;
}

void setProcessorType(int jtype)
{
    if (image != 0)
    {
        delete image;
        image = 0;
    }
    
    CompositeImageType type = (CompositeImageType) jtype;
    switch (type)
    {
        case SPLIT:
        default:
            image = new SplitCompositeImage();
            break;
        case GREEN_MAGENTA:
            image = new GreenMagentaCompositeImage();
            break;
        case RED_CYAN:
            image = new RedCyanCompositeImage();
            break;
    }
}

void setImageN(int isRight, const unsigned char* jpegpath, int orientation, float zoom)
{
    Side side = (isRight > 0) ? RIGHT : LEFT;
    Image* target = image->getImage(side);
    target->init(orientation, zoom, (const char*) jpegpath, (const char*) cachePath);
}

void processN(int growToMaxDim, int flip, const unsigned char* outpath)
{
    image->combineImages((bool) growToMaxDim, (bool)flip, (const char*) outpath);
    delete[] outpath;
}

void cleanUpN()
{
    delete image;
    image = 0;
}

extern "C"
{
    void imageProcessor_initN(const unsigned char* cachePath)
    {
        initN(cachePath);
    }
    
    void imageProcessor_setProcessorType(int type)
    {
        setProcessorType(type);
    }
    
    void imageProcessor_setImageN(int isRight, const unsigned char* jpegpath, int orientation, float zoom)
    {
        setImageN(isRight, jpegpath, orientation, zoom);
    }
    
    void imageProcessor_processN(int growToMaxDim, int flip, const unsigned char* outpath)
    {
        processN(growToMaxDim, flip, outpath);
    }
    
    void imageProcessor_cleanUpN()
    {
        cleanUpN();
    }
    
}
