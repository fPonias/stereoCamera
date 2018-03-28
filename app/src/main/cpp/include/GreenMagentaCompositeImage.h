//
// Created by hallmarklabs on 3/23/18.
//

#ifndef STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGE_H
#define STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGE_H

#include "CompositeImage.h"
#include "jpegCtrl.h"

class GreenMagentaCompositeImage : public CompositeImage
{

private:
    const static Pixel LEFT_MASK;
    const static Pixel RIGHT_MASK;

    Pixel* data;
    size_t targetDim;

    void straightCopy(Image* target, const Pixel* mask);
    void scaledCopy(Image* target, const Pixel* mask);
    void saveFinal(const char* path);
public:
    GreenMagentaCompositeImage();
    ~GreenMagentaCompositeImage();

    void copyTmp(Image* target, Side side);
    void combineImages(bool growToMaxDim, bool flip, const char* path);
    CompositeImageType getType() { return GREEN_MAGENTA; }
};

#endif //STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGE_H
