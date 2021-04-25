//
//  Use this file to import your target's public headers that you would like to expose to Swift.
//

const void* commNew();
void commStartServer(const void* ptr, unsigned int port);
void commStartClient(const void* ptr, const char* host, unsigned int port);
void commCleanUp(const void* ptr);

int commIsConnected(const void* ptr);
int commRead(const void* ptr, unsigned char* buffer, int buffsz);
int commWrite(const void* ptr, const unsigned char* buffer, int buffsz);

void imageProcessor_initN(const unsigned char* cachePath);
void imageProcessor_setProcessorType(int type);
void imageProcessor_setImageN(int isRight, const unsigned char* jpegpath, int orientation, float zoom);
void imageProcessor_preProcessN(int flip);
void imageProcessor_processN(int growToMaxDim, const unsigned char* outpath);
void imageProcessor_cleanUpN();

enum Side
{
    LEFT,
    RIGHT
};

enum CompositeImageType
{
    SPLIT,
    GREEN_MAGENTA,
    RED_CYAN
};

#import "CorePlot-CocoaTouch.h"
