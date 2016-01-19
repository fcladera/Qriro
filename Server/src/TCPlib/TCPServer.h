#ifndef TCP_SERVER_H
#define TCP_SERVER_H

#include <sys/socket.h>
#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include <netinet/in.h>

/* This function creates a TCP socket in tcpSocket, with
 * the given portNumber.
 * Return value: 0 success, -1 failure */

int createTCPServer(int portNumber, int* tcpSocket);

#endif /* TCP_SERVER_H */
