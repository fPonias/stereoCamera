//
// Created by hallmarklabs on 3/17/18.
//

#ifndef STEREOCAMERA_COMPOSITEIMAGE_H
#define STEREOCAMERA_COMPOSITEIMAGE_H

#include "jpegCtrl.hpp"
#include "PreProcessor.h"

enum CompositeImageType
{
    SPLIT,
    GREEN_MAGENTA,
    RED_CYAN
};

class CompositeImage
{
protected:
    Image* left;
    Image* right;
public:
    virtual ~CompositeImage() { };

    void setImages(PreProcessor* preProcessor)
    {
        left = preProcessor->getImage(LEFT);
        right = preProcessor->getImage(RIGHT);
    }

    virtual void combineImages(bool growToMaxDim, const char* path) = 0;
    virtual CompositeImageType getType() = 0;
};

#endif //STEREOCAMERA_COMPOSITEIMAGE_H
