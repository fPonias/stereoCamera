//
// Created by hallmarklabs on 3/13/18.
//

#ifndef STEREOCAMERA_JPEGCTRL_H
#define STEREOCAMERA_JPEGCTRL_H

#include "jpeglib.h"
#include "util.h"
#include <stdio.h>
#include <setjmp.h>

struct Pixel
{
    JSAMPLE red;
    JSAMPLE green;
    JSAMPLE blue;
};

struct ImageData
{
    Pixel* data;
    JDIMENSION width;
    JDIMENSION height;
};



struct my_error_mgr
{
    struct jpeg_error_mgr pub;
    jmp_buf setjmp_buffer;
};

typedef struct my_error_mgr * my_error_ptr;

METHODDEF(void) my_error_exit (j_common_ptr cinfo)
{
    my_error_ptr myerr = (my_error_ptr) cinfo->err;
    (*cinfo->err->output_message) (cinfo);
    longjmp(myerr->setjmp_buffer, 1);
}

class JpegStream
{
private:
    struct jpeg_compress_struct cinfo;
    struct jpeg_error_mgr jerr;

    FILE * outfile;        /* target file */
    JSAMPROW row_pointer[1];    /* pointer to JSAMPLE row[s] */
public:
    JpegStream(const char* filename, int quality, size_t width, size_t height);
    void writeRow(Pixel* buf);
    void finish();
};

class JpegCtrl
{
public:

    static void write_JPEG_file (const char * filename, int quality, ImageData* data);
    static void read_JPEG_file (const char* filename, ImageData* data);

};

#endif //STEREOCAMERA_JPEGCTRL_H
