//
// Created by hallmarklabs on 3/27/18.
//

#ifndef STEREOCAMERA_REDCYANCOMPOSITEIMAGE_H
#define STEREOCAMERA_REDCYANCOMPOSITEIMAGE_H

#include "AnaglyphCompositeImage.h"

class RedCyanCompositeImage : public AnaglyphCompositeImage
{
protected:

public:
    RedCyanCompositeImage()
    {
        AnaglyphCompositeImage::LEFT_MASK.red   = 0xff;
        AnaglyphCompositeImage::LEFT_MASK.green = 0x00;
        AnaglyphCompositeImage::LEFT_MASK.blue  = 0x00;

        AnaglyphCompositeImage::RIGHT_MASK.red   = 0x00;
        AnaglyphCompositeImage::RIGHT_MASK.green = 0xff;
        AnaglyphCompositeImage::RIGHT_MASK.blue  = 0xff;
    }

    CompositeImageType getType() { return RED_CYAN; }
};

#endif //STEREOCAMERA_REDCYANCOMPOSITEIMAGE_H
