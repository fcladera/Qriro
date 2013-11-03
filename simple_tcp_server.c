#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <inttypes.h>
#include <math.h>
#include <fcntl.h>
#include <signal.h>
#include <pthread.h>
#include <time.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/mman.h>
#include <netdb.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#if 0
#include <stdio.h>

FILE *pipe = popen("gnuplot -persist","w");
fprintf(pipe, "set data style lines\n");
fprintf(pipe, "plot 'yourfile.dat' using 1:2\n");
close(pipe);
#endif
// http://www.gnuplot.info/files/gpReadMouseTest.c <= C y Gnuplot

int main(int argc, char **argv){
  //---- check command line arguments ----
  if(argc!=2){ 
    fprintf(stderr,"usage: %s port\n",argv[0]); 
    exit(1); 
  }

  //---- extract local port number ----
  int portNumber;
  if(sscanf(argv[1],"%d",&portNumber)!=1){ 
    fprintf(stderr,"invalid port %s\n",argv[1]); 
    exit(1); 
  }

  //---- create listen socket ----
  int listenSocket=socket(PF_INET,SOCK_STREAM,0);
  if(listenSocket==-1){ 
    perror("socket"); 
    exit(1); 
  }
  // timewait problems
  int on=1;
  if(setsockopt(listenSocket,SOL_SOCKET,SO_REUSEADDR,&on,sizeof(int))==-1){ 
    perror("setsockopt"); 
    exit(1); 
  }
  // bound to any local address on the specified port
  struct sockaddr_in myAddr;
  myAddr.sin_family=AF_INET;
  myAddr.sin_port=htons(portNumber);
  myAddr.sin_addr.s_addr=htonl(INADDR_ANY);
  if(bind(listenSocket,(struct sockaddr *)&myAddr,sizeof(myAddr))==-1){ 
    perror("bind"); 
    exit(1); 
    
  }
  // listening connections
  if(listen(listenSocket,10)==-1){ 
    perror("listen"); 
    exit(1);  
  }

  for(;;){
    //---- accept new connection ----
    struct sockaddr_in fromAddr;
    socklen_t len=sizeof(fromAddr);
    int dialogSocket=accept(listenSocket,(struct sockaddr *)&fromAddr,&len);
    if(dialogSocket==-1){ 
      perror("accept"); 
      exit(1); 
    }
    printf("new connection from %s:%d\n",
	  inet_ntoa(fromAddr.sin_addr),ntohs(fromAddr.sin_port));

    for(;;){
    	//---- receive and display message from client ----
		struct timespec spec;
		clock_gettime(CLOCK_REALTIME, &spec);
		static double lastTime_us = 0;
		volatile double currentTime_us = round(spec.tv_nsec / 1.0e3);

		char buffer[0x100];
		int nb=recv(dialogSocket,buffer,0x100,0);
		if(nb==-1) {
			perror("recvfrom");
			exit(1);
		}
		else if(nb==0)
			break;
		buffer[nb]='\0';
		printf("from %s %d : %d bytes delay %g ns:\n%s\n",
			inet_ntoa(fromAddr.sin_addr),ntohs(fromAddr.sin_port),nb,currentTime_us-lastTime_us,buffer);
		lastTime_us = currentTime_us;

		//---- send reply to client ----
		nb=htons(nb);
		if(sendto(dialogSocket,&nb,sizeof(int),0,(struct sockaddr *)&fromAddr,sizeof(fromAddr))==-1){
			perror("send");
			exit(1);
		}
    }

    //---- close dialog socket ----
    printf("client disconnected\n");
    close(dialogSocket);
  }

  //---- close listen socket ----
  close(listenSocket);
  return 0;
}

