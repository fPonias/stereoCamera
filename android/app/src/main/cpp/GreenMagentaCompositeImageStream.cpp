//
// Created by remote on 1/6/2021.
//

#include "GreenMagentaCompositeImageStream.h"

GreenMagentaCompositeImageStream::GreenMagentaCompositeImageStream()
{
    AnaglyphCompositeImageStream::LEFT_MASK.red   = 0x00;
    AnaglyphCompositeImageStream::LEFT_MASK.green = 0xff;
    AnaglyphCompositeImageStream::LEFT_MASK.blue  = 0x00;

    AnaglyphCompositeImageStream::RIGHT_MASK.red   = 0xff;
    AnaglyphCompositeImageStream::RIGHT_MASK.green = 0x00;
    AnaglyphCompositeImageStream::RIGHT_MASK.blue  = 0xff;
}