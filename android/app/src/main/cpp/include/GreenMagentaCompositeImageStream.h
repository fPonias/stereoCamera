//
// Created by hallmarklabs on 3/23/18.
//

#ifndef STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGESTREAM_H
#define STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGESTREAM_H

#include "CompositeImage.h"
#include "jpegCtrl.hpp"
#include "AnaglyphCompositeImageStream.h"

class GreenMagentaCompositeImageStream : public AnaglyphCompositeImageStream
{
public:
    GreenMagentaCompositeImageStream();
    CompositeImageType getType() { return GREEN_MAGENTA; }
};

#endif //STEREOCAMERA_GREEDMAGENTACOMPOSITEIMAGESTREAM_H
