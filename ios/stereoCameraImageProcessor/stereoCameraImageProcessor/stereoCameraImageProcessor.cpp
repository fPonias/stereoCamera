//
//  stereoCameraImageProcessor.cpp
//  stereoCameraImageProcessor
//
//  Created by hallmarklabs on 6/19/18.
//  Copyright © 2018 cody. All rights reserved.
//

#include <iostream>
#include "stereoCameraImageProcessor.hpp"
#include "stereoCameraImageProcessorPriv.hpp"

void stereoCameraImageProcessor::HelloWorld(const char * s)
{
    stereoCameraImageProcessorPriv *theObj = new stereoCameraImageProcessorPriv;
    theObj->HelloWorldPriv(s);
    delete theObj;
};

void stereoCameraImageProcessorPriv::HelloWorldPriv(const char * s) 
{
    std::cout << s << std::endl;
};

