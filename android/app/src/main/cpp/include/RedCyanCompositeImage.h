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
    RedCyanCompositeImage();
    CompositeImageType getType() { return RED_CYAN; }
};

#endif //STEREOCAMERA_REDCYANCOMPOSITEIMAGE_H
