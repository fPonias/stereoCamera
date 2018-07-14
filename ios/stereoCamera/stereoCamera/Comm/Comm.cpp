//
//  Comm.cpp
//  stereoCamera
//
//  Created by hallmarklabs on 6/14/18.
//  Copyright © 2018 cody. All rights reserved.
//

#include "Comm.hpp"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <netinet/ip.h>
#include <unistd.h>

void CommCpp::startServer(unsigned int port)
{
    if (isConnected())
        return;
    
    this->port = port;
    serverSocket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    
    struct sockaddr_in sin, cli;
    memset(&sin, 0, sizeof(sin));
    sin.sin_len = sizeof(sin);
    sin.sin_family = AF_INET;
    sin.sin_port = port;
    sin.sin_addr.s_addr = INADDR_ANY;
    
    if (bind(serverSocket, (struct sockaddr*)&sin, sizeof(sin)) < 0)
    {
        cleanUp();
        return;
    }
    
    if (listen(serverSocket, 1) == -1)
    {
        cleanUp();
        return;
    }
    
    size_t clisz = sizeof(cli);
    clientSocket = accept(serverSocket, (struct sockaddr*) &cli, (socklen_t*)&clisz);
    
    if (clientSocket == -1)
    {
        cleanUp();
        return;
    }
}

void CommCpp::startClient(const char *host, unsigned int port)
{
    struct sockaddr_in sin;
    bzero((char*) &sin, sizeof(sin));
    sin.sin_family = AF_INET;
    sin.sin_len = sizeof(sin);
    sin.sin_port = port;
    sin.sin_addr = stringToAddr(host);
    
    serverSocket = 0;
    clientSocket = socket(AF_INET, SOCK_STREAM, 0);
    int sz = connect(clientSocket, (const sockaddr*) &sin, sizeof(sin));
    
    if (sz < 0)
    {
        cleanUp();
        return;
    }
}

struct in_addr CommCpp::stringToAddr(const char* address)
{
    char part[4];
    size_t sz = strlen(address);
    int idx = 0;
    int count = 0;
    in_addr_t ret = 0;
    
    for (int i = 0; i < sz; i++)
    {
        char ch = address[i];
        
        if (idx < 4 && ch != '.')
        {
            part[idx] = ch;
            idx++;
        }
        
        if (count < 4 && (ch == '.' || i == sz - 1))
        {
            part[idx] = 0;
            uint32_t num = atoi(part);
            num <<= 8 * (count);
            ret += num;
            
            idx = 0;
            count++;
        }
    }
            
    struct in_addr retStr;
    retStr.s_addr = ret;
    return retStr;
}

void CommCpp::cleanUp()
{    
    if (serverSocket > 0)
    {
        shutdown(serverSocket, SHUT_RDWR);
        close(serverSocket);
    }
    
    if (clientSocket > 0)
    {
        close(clientSocket);
    }
    
    clientSocket = 0;
    serverSocket = 0;
}

bool CommCpp::isConnected()
{
    if (clientSocket > 0)
        return true;
    else
        return false;
}

int CommCpp::read(unsigned char* buffer, int buffsz)
{
    ssize_t current = 0;
    ssize_t sz = 1;
    ssize_t diff, recSz;

    while (sz > 0 && current < buffsz)
    {
        diff = buffsz - current;
        recSz = (diff > 16384) ? 16384 : diff;
    
        sz = recv(clientSocket, buffer + current, recSz, 0);
        current += sz;
        
        printf("comm read %ld bytes\n", sz);
    }
    
    if (sz <= 0)
    {
        cleanUp();
    }
    
    return (int) sz;
}

int CommCpp::write(const unsigned char* buffer, int buffsz)
{
    ssize_t current = 0;
    ssize_t sz = 1;
    ssize_t diff, sendSz;
    
    while (sz > 0 && current < buffsz)
    {
        diff = buffsz - current;
        sendSz = (diff > 16384) ? 16384 : diff;
        
        sz = send(clientSocket, buffer + current, sendSz, 0);
        current += sz;
        
        printf("comm wrote %ld bytes\n", sz);
    }
    
    if (sz <= 0)
    {
        cleanUp();
    }
    
    return (int) sz;
}
