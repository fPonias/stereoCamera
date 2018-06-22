#include <stdio.h>
#include <setjmp.h>
#include <memory.h>
#include "jpegCtrl.hpp"

void JpegCtrl::write_JPEG_file (const char * filename, int quality, ImageData* data)
{
    struct jpeg_compress_struct cinfo;
    struct jpeg_error_mgr jerr;
    
    FILE * outfile;        /* target file */
    
    /* Step 1: allocate and initialize JPEG compression object */
    cinfo.err = jpeg_std_error(&jerr);
    jpeg_create_compress(&cinfo);
    
    /* Step 2: specify data destination (eg, a file) */
    if ((outfile = fopen(filename, "wb")) == NULL)
    {
        fprintf(stderr, "can't open %s\n", filename);
        return;
    }
    jpeg_stdio_dest(&cinfo, outfile);
    
    /* Step 3: set parameters for compression */
    cinfo.image_width = data->width;     /* image width and height, in pixels */
    cinfo.image_height = data->height;
    cinfo.input_components = sizeof(Pixel) / sizeof(JSAMPLE);        /* # of color components per pixel */
    cinfo.in_color_space = JCS_RGB;     /* colorspace of input image */
    
    jpeg_set_defaults(&cinfo);
    jpeg_set_quality(&cinfo, quality, TRUE /* limit to baseline-JPEG values */);
    
    /* Step 4: Start compressor */
    jpeg_start_compress(&cinfo, TRUE);
    
    JSAMPROW row_pointer[1];    /* pointer to JSAMPLE row[s] */
    int row = 0;
    /* Step 5: while (scan lines remain to be written) */
    while (cinfo.next_scanline < cinfo.image_height)
    {
        row = cinfo.next_scanline;
        row_pointer[0] =  (JSAMPLE*) (data->data + (row * data->width));
        (void) jpeg_write_scanlines(&cinfo, &(row_pointer[0]), 1);
    }
    
    /* Step 6: Finish compression */
    jpeg_finish_compress(&cinfo);
    fclose(outfile);
    
    /* Step 7: release JPEG compression object */
    jpeg_destroy_compress(&cinfo);
}

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

void JpegCtrl::read_JPEG_file(const char *path, ImageData* retPtr)
{
    struct jpeg_decompress_struct cinfo;
    
    struct my_error_mgr jerr;
    
    FILE * infile;        /* source file */
    
    if ((infile = fopen(path, "rb")) == NULL)
    {
        fprintf(stderr, "can't open %s\n", path);
        return;
    }
    
    /* Step 1: allocate and initialize JPEG decompression object */
    cinfo.err = jpeg_std_error(&jerr.pub);
    jerr.pub.error_exit = my_error_exit;
    
    if (setjmp(jerr.setjmp_buffer))
    {
        jpeg_destroy_decompress(&cinfo);
        fclose(infile);
        return;
    }
    
    jpeg_create_decompress(&cinfo);
    
    /* Step 2: specify data source (eg, a file) */
    jpeg_stdio_src(&cinfo, infile);
    
    /* Step 3: read file parameters with jpeg_read_header() */
    (void) jpeg_read_header(&cinfo, TRUE);
    
    /* Step 4: set parameters for decompression */
    
    /* Step 5: Start decompressor */
    (void) jpeg_start_decompress(&cinfo);
    
    /* JSAMPLEs per row in output buffer */
    retPtr->width = cinfo.output_width;
    retPtr->height = cinfo.output_height;
    
    size_t sz = cinfo.output_width * cinfo.output_height;
    retPtr->data = new Pixel[sz];
    JSAMPLE* data_ptr;
    JSAMPROW arr[1];
    
    /* Step 6: while (scan lines remain to be read) */
    while (cinfo.output_scanline < cinfo.output_height)
    {
        int scanLine = cinfo.output_scanline;
        data_ptr = (JSAMPLE*) (retPtr->data + (cinfo.output_width * scanLine));
        arr[0] = data_ptr;
        (void) jpeg_read_scanlines(&cinfo, &(arr[0]), 1);
    }
    
    /* Step 7: Finish decompression */
    (void) jpeg_finish_decompress(&cinfo);
    
    /* Step 8: Release JPEG decompression object */
    jpeg_destroy_decompress(&cinfo);
    fclose(infile);
}
