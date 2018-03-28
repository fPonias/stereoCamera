//
// Created by hallmarklabs on 3/23/18.
//

#include "AnaglyphCompositeImage.h"
#include "jpegCtrl.h"
#include <memory.h>
#include <math.h>

AnaglyphCompositeImage::AnaglyphCompositeImage()
{
}

void AnaglyphCompositeImage::straightCopy(Image* target, const Pixel* mask)
{
    size_t rowWidth = (size_t) targetDim;
    FILE* file = fopen(target->getProcPath(), "rb");

    size_t rows = (size_t) target->getTargetDim();
    size_t dim = (size_t) target->getTargetDim();
    Pixel buffer[dim];
    Pixel* buf_ptr = &(buffer[0]);
    size_t jsampSz = sizeof(Pixel);

    char* maskChar = (char*) mask;
    char* bufChar;
    char* dataChar;

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

        idx = dstRow * rowWidth;
        data_ptr = data + idx;

        for (size_t x = 0; x < dim; x++)
        {
            bufChar = (char*) (buf_ptr + x);
            dataChar = (char*) (data_ptr + x);
            maskChar = (char*) mask;

            for (size_t z = 0; z < jsampSz; z++)
            {
                *dataChar = *dataChar | (*maskChar & *bufChar);
                dataChar++;
                maskChar++;
                bufChar++;
            }
        }
    }

    fclose(file);

}

void AnaglyphCompositeImage::scaledCopy(Image* target, const Pixel* mask)
{
    size_t rowWidth = (size_t) targetDim;
    FILE* file = fopen(target->getProcPath(), "rb");

    size_t rows = (size_t) target->getTargetDim();
    size_t dim = (size_t) target->getTargetDim();
    Pixel buffer[dim];
    size_t jsampSz = sizeof(Pixel);

    float ratio = (float) targetDim / (float) dim;
    size_t srcRow = 0;
    size_t srcCol = 0;
    size_t dstCol = 0;
    bool first = true;

    size_t idx = 0;
    Pixel* data_ptr = data;
    Pixel* buf_ptr = 0;
    char* bufChar = 0;
    char* dataChar = 0;
    char* maskChar = 0;
    size_t read;


    for (size_t dstRow = 0; dstRow < rows; dstRow++)
    {
        double newRowD = (double) dstRow / ratio;
        size_t newRow = (size_t) fmax(0, fmin(newRowD, (double) dim - 1.0));
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

        idx = dstRow * rowWidth;

        for (dstCol = 0; dstCol < targetDim; dstCol++)
        {
            srcCol = (size_t) fmax(0, fmin(lround(dstCol / ratio), dim - 1));
            data_ptr = data + idx + dstCol;
            dataChar = (char*) data_ptr;
            buf_ptr = buffer + srcCol;
            bufChar = (char*) buf_ptr;
            maskChar = (char*) mask;

            for (size_t z = 0; z < jsampSz; z++)
            {
                *dataChar = *dataChar | (*maskChar & *bufChar);
                dataChar++;
                maskChar++;
                bufChar++;
            }
        }
    }

    fclose(file);
}

void AnaglyphCompositeImage::saveFinal(const char* path)
{
    JpegCtrl::ImageData imgData;
    imgData.height = (JDIMENSION) targetDim;
    imgData.width = (JDIMENSION) (targetDim);
    imgData.data = data;

    JpegCtrl::write_JPEG_file(path, 85, &imgData);
}

AnaglyphCompositeImage::~AnaglyphCompositeImage()
{

}

void AnaglyphCompositeImage::copyTmp(Image *target, Side side)
{
    const Pixel* mask = (side == LEFT) ? &LEFT_MASK : &RIGHT_MASK;
    float ratio = (float) targetDim / (float) target->getTargetDim();
    double diff = fabs(1.0f - ratio);

    if (diff < 0.005)
        straightCopy(target, mask);
    else
        scaledCopy(target, mask);
}

void AnaglyphCompositeImage::combineImages(bool growToMaxDim, bool flip, const char* path)
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

    size_t leftDim = (size_t) left.getTargetDim();
    size_t rightDim = (size_t) right.getTargetDim();
    if (growToMaxDim)
        targetDim = (leftDim > rightDim) ? leftDim : rightDim;
    else
        targetDim = (leftDim < rightDim) ? leftDim : rightDim;

    size_t sz = targetDim * targetDim;
    data = new Pixel[sz];
    bzero(data, sz * sizeof(Pixel));

    copyTmp(&left, LEFT);
    copyTmp(&right, RIGHT);

    saveFinal(path);

    delete[] data;
}