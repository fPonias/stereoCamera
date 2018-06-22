//
// Created by hallmarklabs on 3/17/18.
//

#include "SplitCompositeImage.h"
#include <stdio.h>
#include <memory.h>
#include <math.h>

SplitCompositeImage::SplitCompositeImage()
{
    data = 0;
}

SplitCompositeImage::~SplitCompositeImage()
{
}

void SplitCompositeImage::straightCopy(Image* target, size_t offset)
{
    size_t rowWidth = (size_t) targetDim * 2;
    FILE* file = fopen(target->getProcPath(), "rb");
    
    size_t rows = (size_t) target->getTargetDim();
    size_t dim = (size_t) target->getTargetDim();
    Pixel* buf_ptr = new Pixel[dim];
    size_t jsampSz = sizeof(Pixel);
    
    size_t idx;
    Pixel* data_ptr;
    
    for (size_t dstRow = 0; dstRow < rows; dstRow++)
    {
        size_t read = fread(buf_ptr, jsampSz, dim, file);
        
        if (read < dim)
        {
            fclose(file);
            return;
        }
        
        idx = dstRow * rowWidth + (offset);
        data_ptr = data + idx;
        
        memcpy(data_ptr, buf_ptr, dim * jsampSz);
    }
    
    delete[] buf_ptr;
    fclose(file);
}

void SplitCompositeImage::scaledCopy(Image* target, size_t offset)
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

void SplitCompositeImage::copyTmp(Image* target, Side side)
{
    size_t offset = (side == LEFT) ? 0 : targetDim;
    float ratio = (float) targetDim / (float) target->getTargetDim();
    if (ratio > 0.995f && ratio < 1.005f)
        straightCopy(target, offset);
    else
        scaledCopy(target, offset);
}

void SplitCompositeImage::saveFinal(const char* path)
{
    JpegCtrl::ImageData imgData;
    imgData.height = (JDIMENSION) targetDim;
    imgData.width = (JDIMENSION) (2 * targetDim);
    imgData.data = data;
    
    JpegCtrl::write_JPEG_file(path, 85, &imgData);
}

void SplitCompositeImage::combineImages(bool growToMaxDim, bool flip, const char* path)
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
    
    size_t sz = targetDim * 2 * targetDim;
    data = new Pixel[sz];
    bzero(data, sz * sizeof(Pixel));
    
    copyTmp(&left, LEFT);
    copyTmp(&right, RIGHT);
    
    saveFinal(path);
    
    delete[] data;
}
