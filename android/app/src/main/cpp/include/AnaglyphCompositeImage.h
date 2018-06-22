//
// Created by hallmarklabs on 3/27/18.
//

#ifndef STEREOCAMERA_ANALGLYPHCOMPOSITEIMAGE_H
#define STEREOCAMERA_ANALGLYPHCOMPOSITEIMAGE_H

#include "CompositeImage.h"
#include "jpegCtrl.hpp"

class AnaglyphCompositeImage : public CompositeImage
{

protected:
    Pixel LEFT_MASK;
    Pixel RIGHT_MASK;

    const char* file;
    size_t targetDim;

    void straightCopy(Image* target, const Pixel* mask);
    void scaledCopy(Image* target, const Pixel* mask);
    void saveFinal(const char* path);
public:
    AnaglyphCompositeImage();
    virtual ~AnaglyphCompositeImage() = 0;
    virtual CompositeImageType getType() = 0;

    void copyTmp(Image* target, Side side);
    void combineImages(bool growToMaxDim, bool flip, const char* path);
};

#endif //STEREOCAMERA_ANALGLYPHCOMPOSITEIMAGE_H
