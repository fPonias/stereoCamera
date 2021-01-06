//
// Created by remote on 1/4/2021.
//

#ifndef ANDROID_SPLITCOMPOSITEIMAGESTREAM_H
#define ANDROID_SPLITCOMPOSITEIMAGESTREAM_H

#include "CompositeImage.h"

struct SideData
{
    size_t dim;
    Pixel* buf;
    FILE* file;
    float ratio;
    size_t jsampSz;
    size_t curRow;

    SideData(size_t dim, FILE* file)
    {
        this->dim = dim;
        buf = new Pixel[dim];
        this->file = file;
        jsampSz = sizeof(Pixel);
        ratio = 0;
        curRow = 0;
    }

    ~SideData()
    {
        delete[] buf;
    }
};

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
    void copyRow(int row, Side side);
public:
    SplitCompositeImageStream();
    void combineImages(bool growToMaxDim, bool flip, const char* path);
    CompositeImageType getType() { return SPLIT; }
};



#endif //ANDROID_SPLITCOMPOSITEIMAGESTREAM_H