//
// Created by remote on 1/4/2021.
//

#ifndef ANDROID_SPLITCOMPOSITEIMAGESTREAM_H
#define ANDROID_SPLITCOMPOSITEIMAGESTREAM_H

#include "CompositeImage.h"
#include "SideData.h"

class SplitCompositeImageStream : public CompositeImage
{
private:
    size_t targetDim;
    SideData* leftData;
    SideData* rightData;
    Pixel* rowBuf;

    void straightCopyRow(size_t row, SideData* data, size_t offset);
    void scaledCopyRow(size_t row, SideData* data, size_t offset);
    void combineStream(bool growToMaxDim, const char* path);
public:
    SplitCompositeImageStream();
    void combineImages(bool growToMaxDim, bool flip, const char* path);
    CompositeImageType getType() { return SPLIT; }
};



#endif //ANDROID_SPLITCOMPOSITEIMAGESTREAM_H