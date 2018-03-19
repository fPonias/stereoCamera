//
// Created by hallmarklabs on 3/17/18.
//

#include "CompositeImage.h"
#include <stdio.h>
#include <memory.h>
#include <math.h>

CompositeImage::CompositeImage()
{
    data = 0;
}

CompositeImage::~CompositeImage()
{
}

void CompositeImage::straightCopy(Image* target, size_t offset)
{
    size_t rowWidth = (size_t) targetDim * 2;
    FILE* file = fopen(target->getProcPath(), "rb");

    size_t rows = (size_t) target->getTargetDim();
    size_t dim = (size_t) target->getTargetDim();
    Pixel buffer[dim];
    Pixel* buf_ptr = &(buffer[0]);
    size_t jsampSz = sizeof(Pixel);
    offset *= jsampSz;

    size_t idx;
    Pixel* data_ptr;

    for (size_t dstRow = 0; dstRow < rows; dstRow++)
    {
        size_t read = fread(buffer, jsampSz, dim, file);

        if (read < dim)
        {
            fclose(file);
            return;
        }

        idx = dstRow * rowWidth + (offset);
        data_ptr = data + idx;

        memcpy(data_ptr, buf_ptr, dim * jsampSz);
    }

    fclose(file);
}

void CompositeImage::scaledCopy(Image* target, size_t offset)
{
    size_t rowWidth = (size_t) targetDim * 2;
    FILE* file = fopen(target->getProcPath(), "rb");

    size_t dim = (size_t) target->getTargetDim();
    Pixel buffer[dim];
    size_t jsampSz = sizeof(Pixel);
    offset *= jsampSz;

    float ratio = (float) targetDim / (float) dim;
    size_t srcRow = 0;
    size_t srcCol = 0;
    size_t dstCol = 0;
    bool first = true;

    size_t idx = 0;
    Pixel* data_ptr = data;
    Pixel* buf_ptr = 0;
    size_t read;


    for (size_t dstRow = 0; dstRow < targetDim; dstRow++)
    {
        size_t newRow = (size_t) fmax(0, fmin(lround(dstRow / ratio), dim - 1));
        if (first || newRow > srcRow)
        {
            first = false;
            read = fread(buffer, jsampSz, (size_t) dim, file);
            srcRow = newRow;

            if (read < dim)
            {
                fclose(file);
                return;
            }
        }

        idx = dstRow * rowWidth + (offset);

        for (dstCol = 0; dstCol < targetDim; dstCol++)
        {
            srcCol = (size_t) fmax(0, fmin(lround(dstCol / ratio), dim - 1));
            data_ptr = data + idx + dstCol;
            buf_ptr = buffer + srcCol;

            memcpy(data_ptr, buf_ptr, jsampSz);
        }
    }

    fclose(file);
}

void CompositeImage::copyTmp(Image* target, size_t offset)
{
    float ratio = (float) targetDim / (float) target->getTargetDim();
    if (ratio > 0.995f && ratio < 1.005f)
        straightCopy(target, offset);
    else
        scaledCopy(target, offset);
}

void CompositeImage::saveFinal(const char* path)
{
    JpegCtrl::ImageData imgData;
    imgData.height = (JDIMENSION) targetDim;
    imgData.width = (JDIMENSION) (2 * targetDim);
    imgData.data = data;

    JpegCtrl::write_JPEG_file(path, 85, &imgData);
}

Image* CompositeImage::getImage(Side side)
{
    return (side == LEFT) ? &left : &right;
}

void CompositeImage::combineImages(bool growToMaxDim, const char* path)
{
    left.processJpeg();
    right.processJpeg();

    size_t leftDim = (size_t) left.getTargetDim();
    size_t rightDim = (size_t) right.getTargetDim();
    if (growToMaxDim)
        targetDim = (leftDim > rightDim) ? leftDim : rightDim;
    else
        targetDim = (leftDim < rightDim) ? leftDim : rightDim;

    size_t sz = targetDim * 2 * targetDim;
    data = new Pixel[sz];
    bzero(data, sz * sizeof(Pixel));

    copyTmp(&left, 0);
    copyTmp(&right, (size_t) targetDim);

    saveFinal(path);

    delete[] data;
}