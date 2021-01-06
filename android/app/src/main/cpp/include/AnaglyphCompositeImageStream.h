//
// Created by hallmarklabs on 3/27/18.
//

#ifndef STEREOCAMERA_ANALGLYPHCOMPOSITEIMAGE_H
#define STEREOCAMERA_ANALGLYPHCOMPOSITEIMAGE_H

#include "CompositeImage.h"
#include "jpegCtrl.hpp"
#include "SideData.h"

class AnaglyphCompositeImageStream : public CompositeImage
{

protected:
    Pixel LEFT_MASK;
    Pixel RIGHT_MASK;

    const char* file;
    size_t targetDim;

    SideData* leftData;
    SideData* rightData;
    Pixel* rowBuf;

    void straightCopyRow(size_t row, SideData* data, const Pixel* mask);
    void scaledCopyRow(size_t row, SideData* data, const Pixel* mask);
public:
    AnaglyphCompositeImageStream() { }
    virtual CompositeImageType getType() = 0;
    void combineImages(bool growToMaxDim, const char* path);
};

#endif //STEREOCAMERA_ANALGLYPHCOMPOSITEIMAGE_H
