//
// Created by remote on 1/6/2021.
//

#ifndef ANDROID_SIDEDATA_H
#define ANDROID_SIDEDATA_H

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

    size_t readLine()
    {
        curRow++;
        return fread(buf, jsampSz, dim, file);
    }
};

#endif //ANDROID_SIDEDATA_H
