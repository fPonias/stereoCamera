//
// Created by hallmarklabs on 3/17/18.
//

#ifndef STEREOCAMERA_COMPOSITEIMAGE_H
#define STEREOCAMERA_COMPOSITEIMAGE_H

#include "jpegCtrl.h"

enum Side
{
    LEFT,
    RIGHT
};

class CompositeImage
{
private:
    size_t targetDim;

    Image left;
    Image right;
    Pixel* data;

    void straightCopy(Image* target, size_t offset);
    void scaledCopy(Image* target, size_t offset);
    void copyTmp(Image* target, size_t offset);
    void saveFinal(const char* path);
public:
    CompositeImage();
    ~CompositeImage();

    Image* getImage(Side side);
    void combineImages(bool growToMaxDim, const char* path);
};

#endif //STEREOCAMERA_COMPOSITEIMAGE_H
