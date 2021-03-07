#include <cstdlib>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "jpegCtrl.hpp"
#include "util.h"
#include "SplitCompositeImage.h"
#include "GreenMagentaCompositeImage.h"
#include "RedCyanCompositeImage.h"
#include "Image.h"

using namespace std;

CompositeImage* image;
const char* cachePath;


void init(const char* path)
{
    cachePath = path;
    image = 0;
}

void setProcessorType(CompositeImageType type)
{
    if (image != 0)
    {
        delete image;
        image = 0;
    }

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

void setImage(bool isRight, const char* jpegpath, int orientation, float zoom)
{
    Side side = (isRight) ? RIGHT : LEFT;
    Image* target = image->getImage(side);
    target->init(orientation, zoom, jpegpath, cachePath);
}

void process(bool growToMaxDim, bool flip, const char* outpath)
{
    image->combineImages(growToMaxDim, flip, outpath);
}

void cleanUp()
{
    delete image;
}


/*
 * 
 */
int main(int argc, char** argv) 
{
    if (argc != 10)
    {
        printf("correct executable usage is stereoCameraImageProcess cachePath processorType leftPath leftRotation leftZoom rightPath rightRotation rightZoom outPath");
        return 0;
    }
    
    const char* cachePath = argv[1];
    const char* processorTypeStr = argv[2];
    CompositeImageType processorType = SPLIT;
    
    if (strcmp(processorTypeStr, "SPLIT") == 0)
        processorType = SPLIT;
    else if (strcmp(processorTypeStr, "GREEN_MAGENTA") == 0)
        processorType = GREEN_MAGENTA;
    else
        processorType = RED_CYAN;
    
    const char* leftPath = argv[3];
    int leftRotation = atoi(argv[4]);
    float leftZoom = atof(argv[5]);
    
    const char* rightPath = argv[6];
    int rightRotation = atoi(argv[7]);
    float rightZoom = atof(argv[8]);
    
    const char* outPath = argv[9];
    
    init(cachePath);
    setProcessorType(processorType);
    setImage(false, leftPath, leftRotation, leftZoom);
    setImage(true, rightPath, rightRotation, rightZoom);
    process(true, false, outPath);
    cleanUp();
    
    return 0;
}