//
// Created by remote on 1/4/2021.
//

#include "SplitCompositeImageStream.h"
#include <stdio.h>
#include <memory.h>
#include <math.h>
#include <malloc.h>
#include <string.h>

SplitCompositeImageStream::SplitCompositeImageStream()
{
}

void SplitCompositeImageStream::combineStream(bool growToMaxDim, const char* path)
{
    leftData = new SideData(left.getTargetDim(), fopen(left.getProcPath(), "rb"));
    rightData = new SideData(right.getTargetDim(), fopen(right.getProcPath(), "rb"));

    if (growToMaxDim)
        targetDim = (leftData->dim > rightData->dim) ? leftData->dim : rightData->dim;
    else
        targetDim = (leftData->dim < rightData->dim) ? leftData->dim : rightData->dim;

    leftData->ratio = (float) targetDim / (float) leftData->dim;
    rightData->ratio = (float) targetDim / (float) rightData->dim;

    size_t rowSz = 2 * targetDim;
    rowBuf = new Pixel[rowSz];
    JpegStream str(path, 85, rowSz, targetDim);

    for (int i = 0; i < targetDim; i++)
    {
        if (leftData->ratio > 0.995f && leftData->ratio < 1.005f)
            straightCopyRow(i, leftData, 0);
        else
            scaledCopyRow(i, leftData, 0);

        if (rightData->ratio > 0.995f && rightData->ratio < 1.005f)
            straightCopyRow(i, rightData, targetDim);
        else
            scaledCopyRow(i, rightData, targetDim);

        str.writeRow(rowBuf);
    }

    str.finish();
    delete[] rowBuf;

    fclose(leftData->file);
    fclose(rightData->file);

    delete leftData;
    delete rightData;
}

void SplitCompositeImageStream::straightCopyRow(size_t dstRow, SideData* sideData, size_t offset)
{
    size_t read = fread(sideData->buf, sideData->jsampSz, sideData->dim, sideData->file);

    if (read < sideData->dim)
        return;

    Pixel* data_ptr = rowBuf + offset;
    memcpy(data_ptr, sideData->buf, sideData->dim * sideData->jsampSz);
}

void SplitCompositeImageStream::scaledCopyRow(size_t dstRow, SideData* sideData, size_t offset)
{
    offset *= sideData->jsampSz;
    size_t rowWidth = targetDim * 2;

    size_t srcRow = 0;
    size_t srcCol = 0;
    size_t dstCol = 0;

    size_t idx = 0;
    Pixel* data_ptr = rowBuf;
    size_t read;

    size_t newRow = (size_t) fmax(0, fmin(lround(dstRow / sideData->ratio), sideData->dim - 1));
    if (newRow > sideData->curRow)
    {
        while (sideData->curRow < newRow)
        {
            read = fread(sideData->buf, sideData->jsampSz, (size_t) sideData->dim, sideData->file);
            sideData->curRow++;

            if (read < sideData->dim)
            {
                return;
            }
        }
    }

    idx = offset;
    Pixel* buf_ptr;

    for (dstCol = 0; dstCol < targetDim; dstCol++)
    {
        srcCol = (size_t) fmax(0, fmin(lround(dstCol / sideData->ratio), sideData->dim - 1));
        data_ptr = rowBuf + idx + dstCol;
        buf_ptr = sideData->buf + srcCol;

        memcpy(data_ptr, buf_ptr, sideData->jsampSz);
    }
}

void SplitCompositeImageStream::combineImages(bool growToMaxDim, bool flip, const char* path)
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