//
// Created by hallmarklabs on 3/27/18.
//

#ifndef STEREOCAMERA_REDCYANCOMPOSITEIMAGESTREAM_H
#define STEREOCAMERA_REDCYANCOMPOSITEIMAGESTREAM_H

#include "AnaglyphCompositeImageStream.h"

class RedCyanCompositeImageStream : public AnaglyphCompositeImageStream
{
protected:

public:
    RedCyanCompositeImageStream();

    CompositeImageType getType() { return RED_CYAN; }
};

#endif //STEREOCAMERA_REDCYANCOMPOSITEIMAGESTREAM_H
