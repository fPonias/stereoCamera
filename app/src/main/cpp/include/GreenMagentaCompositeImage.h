//
// Created by hallmarklabs on 3/23/18.
//

#ifndef STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGE_H
#define STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGE_H

#include "CompositeImage.h"
#include "jpegCtrl.h"
#include "AnaglyphCompositeImage.h"

class GreenMagentaCompositeImage : public AnaglyphCompositeImage
{
public:
    GreenMagentaCompositeImage();
    CompositeImageType getType() { return GREEN_MAGENTA; }
};

#endif //STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGE_H
