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

#include "fifo.h"

#define SIZE_VALUES 100
// http://www.gnuplot.info/files/gpReadMouseTest.c <= C y Gnuplot
// feedgnuplot

// Read
// https://github.com/xioTechnologies/Gait-Tracking-With-x-IMU
// http://www.x-io.co.uk/gait-tracking-with-x-imu/
// http://www.x-io.co.uk/open-source-imu-and-ahrs-algorithms/ <= Source code available


// TODO
/*
 * Improve reception - Using a cable less packets are lost?
 * Improve feedgnuplot plotting (if possible). Multiple plots in one window
 * Show data rate reception for each sensor. Reduce number of samples per second?
 * Convert to physic units!
 * Basic integration. Show drift error
 * Get information from the magnetometer
 * Read imu and ahrs algorithms
 */
int main(int argc, char **argv){


	// Accelerometer vectors (acceleration, velocity, position)
	double x_accel[SIZE_VALUES], y_accel[SIZE_VALUES], z_accel[SIZE_VALUES];
	double x_vel[SIZE_VALUES], y_vel[SIZE_VALUES], z_vel[SIZE_VALUES];
	double x_pos[SIZE_VALUES], y_pos[SIZE_VALUES], z_pos[SIZE_VALUES];

	// Gyro vectors (rotational acceleration, velocity, position)
	double alpha_accel[SIZE_VALUES], beta_accel[SIZE_VALUES], gamma_accel[SIZE_VALUES];
	double alpha_vel[SIZE_VALUES], beta_vel[SIZE_VALUES], gamma_vel[SIZE_VALUES];
	double alpha_pos[SIZE_VALUES], beta_pos[SIZE_VALUES], gamma_pos[SIZE_VALUES];


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


  // Gnuplot pipe
  /* Create a FIFO we later use for communication gnuplot => our program. */
	FILE  *gp_accel_x, *gp_accel_y, *gp_accel_z, *gp_accel_alpha, *gp_accel_beta, *gp_accel_gamma;
	char * command = "feedgnuplot --lines --stream 0.1 --xlen 1000 --ylabel 'Bytes/sec' --xlabel seconds > /dev/null";
	if (NULL == (gp_accel_x = popen(command,"w"))) {
	  perror("gnuplot");
	  pclose(gp_accel_x);
	  return 1;
	}
	if (NULL == (gp_accel_y = popen(command,"w"))) {
	  perror("gnuplot");
	  pclose(gp_accel_y);
	  return 1;
	}
	if (NULL == (gp_accel_z = popen(command,"w"))) {
	  perror("gnuplot");
	  pclose(gp_accel_z);
	  return 1;
	}
	if (NULL == (gp_accel_alpha = popen(command,"w"))) {
		  perror("gnuplot");
		  pclose(gp_accel_alpha);
		  return 1;
	}
	if (NULL == (gp_accel_beta = popen(command,"w"))) {
		  perror("gnuplot");
		  pclose(gp_accel_beta);
		  return 1;
	}
	if (NULL == (gp_accel_gamma = popen(command,"w"))) {
		  perror("gnuplot");
		  pclose(gp_accel_gamma);
		  return 1;
	}
	puts("Connected to gnuplot.\n");

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
		else if(nb==0){
			break;
		}
		buffer[nb]='\0';
		//printf("from %s %d : %d bytes delay %g ns:\n%s\n",
		//	inet_ntoa(fromAddr.sin_addr),ntohs(fromAddr.sin_port),nb,currentTime_us-lastTime_us,buffer);
		lastTime_us = currentTime_us;

		// TODO
		// sscanf only scans one line. It should be analyzed what happens if multiple lines arrive in one packet

		if(buffer[0]=='G'){
			// gyro frame received
			double gyroValues[3];
			unsigned int timeValue;
			sscanf(buffer,"%*c:%u:%lf:%lf:%lf;",&timeValue,gyroValues,gyroValues+1,gyroValues+2);

			loadfifoMooving(gyroValues[0],alpha_accel,SIZE_VALUES);
			loadfifoMooving(gyroValues[1],beta_accel,SIZE_VALUES);
			loadfifoMooving(gyroValues[2],gamma_accel,SIZE_VALUES);
			fprintf(gp_accel_alpha, "\n%lf\n",gyroValues[0]);
			fflush(gp_accel_alpha);
			fprintf(gp_accel_beta, "\n%lf\n",gyroValues[1]);
			fflush(gp_accel_beta);
			fprintf(gp_accel_gamma, "\n%lf\n",gyroValues[2]);
			fflush(gp_accel_gamma);


		}
		else if(buffer[0]=='A'){
			// accelerometer frame received
			double accelValues[3];
			unsigned int timeValue;
			sscanf(buffer,"%*c:%u:%lf:%lf:%lf;",&timeValue,accelValues,accelValues+1,accelValues+2);

			loadfifoMooving(accelValues[0],alpha_accel,SIZE_VALUES);
			loadfifoMooving(accelValues[1],beta_accel,SIZE_VALUES);
			loadfifoMooving(accelValues[2],gamma_accel,SIZE_VALUES);
			fprintf(gp_accel_x, "\n%lf\n",accelValues[0]);
			fflush(gp_accel_x);
			fprintf(gp_accel_y, "\n%lf\n",accelValues[1]);
			fflush(gp_accel_y);
			fprintf(gp_accel_z, "\n%lf\n",accelValues[2]);
			fflush(gp_accel_z);
		}
		else{
			printf("Wrong frame received!!\n");
		}


		//---- send reply to client ----
//		nb=htons(nb);
//		if(sendto(dialogSocket,&nb,sizeof(int),0,(struct sockaddr *)&fromAddr,sizeof(fromAddr))==-1){
//			perror("send");
//			exit(1);
//		}

    }

    //---- close dialog socket ----
    printf("client disconnected\n");
    close(dialogSocket);
  }

  //---- close listen socket ----
  close(listenSocket);
  return 0;

  //----close gnuplot-----
  pclose(gp_accel_x);
  pclose(gp_accel_y);
  pclose(gp_accel_z);
  pclose(gp_accel_alpha);
  pclose(gp_accel_beta);
  pclose(gp_accel_gamma);

}

