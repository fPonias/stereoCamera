//
// Created by remote on 1/6/2021.
//

#ifndef ANDROID_PREPROCESSOR_H
#define ANDROID_PREPROCESSOR_H

#include "Image.h"

enum Side
{
    LEFT,
    RIGHT
};

class PreProcessor
{
    Image left;
    Image right;

public:
    Image* getImage(Side side) {return (side == LEFT) ? &left : &right;}
    void process(bool flip);
};

#endif //ANDROID_PREPROCESSOR_H
