#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>

#include "jpegCtrl.hpp"
#include "util.h"
#include "SplitCompositeImageStream.h"
#include "GreenMagentaCompositeImageStream.h"
#include "RedCyanCompositeImageStream.h"
#include "Image.h"

PreProcessor* preProcessor;
CompositeImage* image;
const char* cachePath;

void initN(const char* jcachePath)
{
    srand(time(NULL));
    cachePath = jcachePath;
    preProcessor = new PreProcessor();
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

void setImageN(int isRight, const char* jpegpath, int orientation, float zoom)
{
    Side side = (isRight > 0) ? RIGHT : LEFT;
    Image* target = preProcessor->getImage(side);
    target->init(orientation, zoom, jpegpath, cachePath);
}

void preProcessN(int flip)
{
    preProcessor->process((bool) flip);
}

void processN(int growToMaxDim, const char* outpath)
{
    image->setImages(preProcessor);
    image->combineImages((bool) growToMaxDim, outpath);
    delete[] outpath;
}

void cleanUpN()
{
    delete image;
    delete preProcessor;
    delete[] cachePath;
}

extern "C"
{
    void imageProcessor_initN(const char* cachePath)
    {
        initN(cachePath);
    }
    
    void imageProcessor_setProcessorType(int type)
    {
        setProcessorType(type);
    }
    
    void imageProcessor_setImageN(int isRight, const char* jpegpath, int orientation, float zoom)
    {
        setImageN(isRight, jpegpath, orientation, zoom);
    }

    void imageProcessor_preProcessN(int flip)
    {
        preProcessN(flip);
    }
    
    void imageProcessor_processN(int growToMaxDim, const char* outpath)
    {
        processN(growToMaxDim, outpath);
    }
    
    void imageProcessor_cleanUpN()
    {
        cleanUpN();
    }
    
}
