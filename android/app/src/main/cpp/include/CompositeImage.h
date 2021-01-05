//
// Created by hallmarklabs on 3/17/18.
//

#ifndef STEREOCAMERA_COMPOSITEIMAGE_H
#define STEREOCAMERA_COMPOSITEIMAGE_H

#include "jpegCtrl.hpp"

enum Side
{
    LEFT,
    RIGHT
};

enum CompositeImageType
{
    SPLIT,
    GREEN_MAGENTA,
    RED_CYAN
};

class CompositeImage
{
protected:
    Image left;
    Image right;
public:
    virtual ~CompositeImage() {};
    Image* getImage(Side side) { return (side == LEFT) ? &left : &right; }
    virtual void combineImages(bool growToMaxDim, bool flip, const char* path) = 0;
    virtual CompositeImageType getType() = 0;
};

#endif //STEREOCAMERA_COMPOSITEIMAGE_H
