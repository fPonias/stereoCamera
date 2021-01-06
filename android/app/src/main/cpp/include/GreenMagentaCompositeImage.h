//
// Created by hallmarklabs on 3/23/18.
//

#ifndef STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGE_H
#define STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGE_H

#include "CompositeImage.h"
#include "jpegCtrl.hpp"
#include "AnaglyphCompositeImage.h"

class GreenMagentaCompositeImage : public AnaglyphCompositeImage
{
public:
    void init()
    {
        AnaglyphCompositeImage::LEFT_MASK.red   = 0x00;
        AnaglyphCompositeImage::LEFT_MASK.green = 0xff;
        AnaglyphCompositeImage::LEFT_MASK.blue  = 0x00;

        AnaglyphCompositeImage::RIGHT_MASK.red   = 0xff;
        AnaglyphCompositeImage::RIGHT_MASK.green = 0x00;
        AnaglyphCompositeImage::RIGHT_MASK.blue  = 0xff;
    }

    CompositeImageType getType() { return GREEN_MAGENTA; }
};

#endif //STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGE_H
