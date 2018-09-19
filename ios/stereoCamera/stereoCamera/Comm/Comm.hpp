//
//  Comm.hpp
//  stereoCamera
//
//  Created by hallmarklabs on 6/14/18.
//  Copyright © 2018 cody. All rights reserved.
//

#ifndef Comm_hpp
#define Comm_hpp

#include <stdio.h>

class CommCpp
{
private:
    int serverSocket;
    int clientSocket;
    unsigned int port;
    int connectAttempt;
    
    struct in_addr stringToAddr(const char* address);
public:
    void startServer(unsigned int port);
    void startClient(const char* host, unsigned int port);
    void cleanUp();
    bool isConnected();
    
    int read(unsigned char* buffer, int buffsz);
    int write(const unsigned char* buffer, int buffsz);
};

extern "C"
{
    const void* commNew() { return new CommCpp(); }
    void commStartServer(const void* ptr, unsigned int port) { ((CommCpp*) ptr)->startServer(port); }
    void commStartClient(const void* ptr, const char* host, unsigned int port) { ((CommCpp*) ptr)->startClient(host, port); }
    void commCleanUp(const void* ptr)
    {
        ((CommCpp*) ptr)->cleanUp();
        //delete (CommCpp*) ptr;
    }
    
    int commIsConnected(const void* ptr) { return ((CommCpp*) ptr)->isConnected(); }
    int commRead(const void* ptr, unsigned char* buffer, int buffsz) { return ((CommCpp*) ptr)->read(buffer, buffsz); }
    int commWrite(const void* ptr, const unsigned char* buffer, int buffsz) { return ((CommCpp*) ptr)->write(buffer, buffsz); }
}

#endif /* Comm_hpp */
