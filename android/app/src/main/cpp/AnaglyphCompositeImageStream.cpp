//
// Created by remote on 1/6/2021.
//

#include <math.h>
#include <cstring>
#include "AnaglyphCompositeImageStream.h"
#include "SideData.h"


void AnaglyphCompositeImageStream::straightCopyRow(size_t row, SideData* data, const Pixel* mask)
{
    char* maskChar;
    char* bufChar;
    char* dataChar;

    size_t read = data->readLine();

    if (read < data->dim)
        return;

    for (size_t x = 0; x < data->dim; x++)
    {
        bufChar = (char*) (data->buf + x);
        dataChar = (char*) (rowBuf + x);
        maskChar = (char*) mask;

        for (size_t z = 0; z < data->jsampSz; z++)
        {
            *dataChar = *dataChar | (*maskChar & *bufChar);
            dataChar++;
            maskChar++;
            bufChar++;
        }
    }
}

void AnaglyphCompositeImageStream::scaledCopyRow(size_t row, SideData* data, const Pixel* mask)
{
    size_t srcCol;
    size_t dstCol;

    Pixel* data_ptr;
    Pixel* buf_ptr;
    char* bufChar;
    char* dataChar;
    char* maskChar;
    size_t read;

    double newRowD = (double) row / data->ratio;
    auto newRow = (size_t) fmax(0, fmin(newRowD, (double) data->dim - 1.0));
    while (newRow > data->curRow)
    {
        read = data->readLine();

        if (read < data->dim)
            return;
    }

    for (dstCol = 0; dstCol < targetDim; dstCol++)
    {
        srcCol = (size_t) fmax(0, fmin(lround(dstCol / data->ratio), data->dim - 1));
        data_ptr = rowBuf + dstCol;
        dataChar = (char*) data_ptr;
        buf_ptr = data->buf + srcCol;
        bufChar = (char*) buf_ptr;
        maskChar = (char*) mask;

        for (size_t z = 0; z < data->jsampSz; z++)
        {
            *dataChar = *dataChar | (*maskChar & *bufChar);
            dataChar++;
            maskChar++;
            bufChar++;
        }
    }
}

void AnaglyphCompositeImageStream::combineStream(bool growToMaxDim, const char* path)
{
    leftData = new SideData(left.getTargetDim(), fopen(left.getProcPath(), "rb"));
    rightData = new SideData(right.getTargetDim(), fopen(right.getProcPath(), "rb"));

    if (growToMaxDim)
        targetDim = (leftData->dim > rightData->dim) ? leftData->dim : rightData->dim;
    else
        targetDim = (leftData->dim < rightData->dim) ? leftData->dim : rightData->dim;

    leftData->ratio = (float) targetDim / (float) leftData->dim;
    rightData->ratio = (float) targetDim / (float) rightData->dim;

    rowBuf = new Pixel[targetDim];
    JpegStream str(path, 85, targetDim, targetDim);

    for (int i = 0; i < targetDim; i++)
    {
        memset(rowBuf, 0, sizeof(Pixel) * targetDim);

        if (leftData->ratio > 0.995f && leftData->ratio < 1.005f)
            straightCopyRow(i, leftData, &LEFT_MASK);
        else
            scaledCopyRow(i, leftData, &LEFT_MASK);

        if (rightData->ratio > 0.995f && rightData->ratio < 1.005f)
            straightCopyRow(i, rightData, &RIGHT_MASK);
        else
            scaledCopyRow(i, rightData, &RIGHT_MASK);

        str.writeRow(rowBuf);
    }

    str.finish();
    delete[] rowBuf;

    fclose(leftData->file);
    fclose(rightData->file);

    delete leftData;
    delete rightData;
}

void AnaglyphCompositeImageStream::combineImages(bool growToMaxDim, bool flip, const char* path)
{
    if (flip)
    {
        Image tmp = left;
        left = right;
        right = tmp;

        int lorient = left.getOrientation();
        lorient = (lorient + 2) % 4;
        left.setOrientation(lorient);

        int rorient = right.getOrientation();
        rorient = (rorient + 2) % 4;
        right.setOrientation(rorient);
    }

    left.processJpeg();
    right.processJpeg();

    combineStream(growToMaxDim, path);
}
