//
// Created by remote on 1/6/2021.
//

#include "PreProcessor.h"

void PreProcessor::process(bool flip)
{
    if (flip)
    {
        Image tmp = left;
        left = right;
        right = tmp;

        int lorient = left.getOrientation();
        lorient = (lorient + 2) % 4;
        left.setOrientation(lorient);

        int rorient = right.getOrientation();
        rorient = (rorient + 2) % 4;
        right.setOrientation(rorient);
    }

    left.processJpeg();
    right.processJpeg();
}