//
// Created by hallmarklabs on 3/23/18.
//

#ifndef STEREOCAMERA_SPLITCOMPOSITEIMAGE_H
#define STEREOCAMERA_SPLITCOMPOSITEIMAGE_H

#include "CompositeImage.h"

class SplitCompositeImage : public CompositeImage
{
private:
    size_t targetDim;

    Pixel* data;

    void straightCopy(Image* target, size_t offset);
    void scaledCopy(Image* target, size_t offset);
    void copyTmp(Image* target, size_t offset);
    void saveFinal(const char* path);
public:
    SplitCompositeImage();
    ~SplitCompositeImage();
    void copyTmp(Image* target, Side side);
    void combineImages(bool growToMaxDim, bool flip, const char* path);
    CompositeImageType getType() { return SPLIT; }
};

#endif //STEREOCAMERA_SPLITCOMPOSITEIMAGE_H
