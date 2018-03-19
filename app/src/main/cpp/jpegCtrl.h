//
// Created by hallmarklabs on 3/13/18.
//

#ifndef STEREOCAMERA_JPEGCTRL_H
#define STEREOCAMERA_JPEGCTRL_H

#include "jpeg-9c/jpeglib.h"
#include "util.h"

struct Pixel
{
    JSAMPLE red;
    JSAMPLE green;
    JSAMPLE blue;
};

class JpegCtrl
{
public:
    struct ImageData
    {
        Pixel* data;
        JDIMENSION width;
        JDIMENSION height;
    };

    static void write_JPEG_file (const char * filename, int quality, ImageData* data);
    static void read_JPEG_file (const char* filename, ImageData* data);

};

#endif //STEREOCAMERA_JPEGCTRL_H
