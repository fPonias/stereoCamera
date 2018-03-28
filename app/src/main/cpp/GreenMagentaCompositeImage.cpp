#include "GreenMagentaCompositeImage.h"

GreenMagentaCompositeImage::GreenMagentaCompositeImage()
{
    AnaglyphCompositeImage::LEFT_MASK.red   = 0x00;
    AnaglyphCompositeImage::LEFT_MASK.green = 0xff;
    AnaglyphCompositeImage::LEFT_MASK.blue  = 0x00;

    AnaglyphCompositeImage::RIGHT_MASK.red   = 0xff;
    AnaglyphCompositeImage::RIGHT_MASK.green = 0x00;
    AnaglyphCompositeImage::RIGHT_MASK.blue  = 0xff;
}