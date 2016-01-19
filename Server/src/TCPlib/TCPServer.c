#include "TCPServer.h"

int createTCPServer(int portNumber, int* tcpSocket){
  int *listenSocket = tcpSocket;

  // Listen socket for the Application
  *listenSocket = socket(PF_INET,SOCK_STREAM,0);
  if(*listenSocket==-1){
    perror("ERROR creating TCP socket - listen");
    fprintf(stderr, "Server with port number %d\n", portNumber);
    return -1;
  }

  // Avoid problems if the program is quickly restarted
  // http://stackoverflow.com/questions/14388706/socket-options-so-reuseaddr-and-so-reuseport-how-do-they-differ-do-they-mean-t
  const int on=1;
  if(setsockopt(*listenSocket,SOL_SOCKET,SO_REUSEADDR,&on,sizeof(int))==-1){
    perror("ERROR creating TCP socket - setsockopt");
    fprintf(stderr, "Server with port number %d\n", portNumber);
    return -1;
  }

  // bound to any local address on the specified port
  struct sockaddr_in myAddrApplication;
  myAddrApplication.sin_family=AF_INET;
  myAddrApplication.sin_port=htons(portNumber);
  myAddrApplication.sin_addr.s_addr=htonl(INADDR_ANY);
  if(bind(*listenSocket,(struct sockaddr *)&myAddrApplication,sizeof(myAddrApplication))==-1){
    perror("ERROR creating TCP socket - bind");
    fprintf(stderr, "Server with port number %d\n", portNumber);
    return -1;
  }

  // Accept connections
  if(listen(*listenSocket,10)==-1){
    perror("ERROR creating TCP socket - listen");
    fprintf(stderr, "Server with port number %d\n", portNumber);
    return -1;
  }
  return 0;
}
