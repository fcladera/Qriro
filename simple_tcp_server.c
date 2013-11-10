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

#define SIZE_VALUES 256
#define SIZE_TCP_BUFFER 0x1000

#define LOG_TO_FILE 0
#define MEASURE_EXECUTION_TIME 0
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


#if 0
	// This is an example how to fetch multiple lines from a char[]
	FILE *fd = NULL;
	fd = fopen("example.tst","r");
	if (fd==NULL) perror(__FILE__);
	char linea[MAX_REC_LEN];
	printf("File opened\n");
	/*
	while (!feof(fd)) {
	  double gyroValues[3];
	  double timeValue;
	  int scanresult = fscanf(fd,"%*c:%lf:%lf:%lf:%lf;\n",&timeValue,gyroValues,gyroValues+1,gyroValues+2);
	  printf("%d\n",scanresult);
	  if( scanresult != 4){
		  printf("EOF?\n");
		  break;
	  }

	  //printf("Line %lf:%lf:%lf:%lf\n", timeValue,gyroValues[0],gyroValues[1],gyroValues[2]);
	}
	*/
	fread(linea,1,MAX_REC_LEN,fd);
	fclose(fd);
	//printf("%s",linea);

	char * glissant = linea;
	while (*glissant!='\0') {
		  double gyroValues[3];
		  double timeValue;
		  int scanresult = sscanf(glissant,"%*c:%lf:%lf:%lf:%lf;\n",&timeValue,gyroValues,gyroValues+1,gyroValues+2);
		  printf("%d\n",scanresult);
		  if( scanresult != 4){
			  printf("EOF?\n");
			  break;
		  }

		  printf("Line %lf:%lf:%lf:%lf\n", timeValue,gyroValues[0],gyroValues[1],gyroValues[2]);

		  // jump to next line
		  while(*(++glissant)!='\n');
		  glissant++;

	}
	return 0;
	#endif

	// Accelerometer vectors (acceleration, velocity, position)
	double x_accel[SIZE_VALUES], y_accel[SIZE_VALUES], z_accel[SIZE_VALUES];
	//double x_vel[SIZE_VALUES], y_vel[SIZE_VALUES], z_vel[SIZE_VALUES];
	//double x_pos[SIZE_VALUES], y_pos[SIZE_VALUES], z_pos[SIZE_VALUES];

	// Gyro vectors (rotational velocity, position)
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
	FILE  *gp_accel,  *gp_gyro, *gp_latency;
	char * command = "feedgnuplot --lines --stream 0.1 --xlen 1000 --ylabel 'value' --xlabel sample > /dev/null";
	if (NULL == (gp_accel = popen(command,"w"))) {
	  perror("gnuplot");
	  pclose(gp_accel);
	  return 1;
	}
	if (NULL == (gp_gyro = popen(command,"w"))) {
		  perror("gnuplot");
		  pclose(gp_gyro);
		  return 1;
	}
	if (NULL == (gp_latency = popen(command,"w"))) {
		  perror("gnuplot");
		  pclose(gp_latency);
		  return 1;
	}


	printf("Connected to gnuplot.\n");


	// Log file
	FILE *logfile = NULL;
	if(LOG_TO_FILE){
		printf("WARNING: Log enabled!\nThe values will be stored in points.dat\n");
		logfile = fopen("points.dat","w");
		if (logfile==NULL) perror(__FILE__);
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

		struct timespec spec;
		double startTime, endTime;
		for(;;){
			// Get message from the client

			char buffer[SIZE_TCP_BUFFER];
			int nb=recv(dialogSocket,buffer,SIZE_TCP_BUFFER,0);
			if(nb==-1) {
				perror("recvfrom");
				exit(1);
			}
			else if(nb==0){
				break;
			}

			if(MEASURE_EXECUTION_TIME){
				clock_gettime(CLOCK_REALTIME, &spec);
				startTime = round(spec.tv_nsec / 1.0e3);
			}
			if(LOG_TO_FILE){
				fprintf(logfile,"%s",buffer);
			}
			buffer[nb]='\0';


			//printf("from %s %d : %d bytes:\n%s\n",
			//	inet_ntoa(fromAddr.sin_addr),ntohs(fromAddr.sin_port),nb,buffer);

			char * sliding_pointer = buffer;
			while (*sliding_pointer!='\0') {
				char sensorType;
				double values[3];
				double timeValue;

				if((*sliding_pointer!='G')&&(*sliding_pointer!='A')){
					printf("Wrong frame received!!\n");
					printf("ERRONEOUS FRAME: from %s %d : %d bytes:\n%s\n",
							inet_ntoa(fromAddr.sin_addr),ntohs(fromAddr.sin_port),nb,buffer);
					exit(EXIT_FAILURE);
				}

				if( sscanf(sliding_pointer,"%c:%lf:%lf:%lf:%lf;\n",&sensorType,&timeValue,values,values+1,values+2) != 5){
					printf("Invalid line format?: from %s %d : %d bytes:\n%s\n",
							inet_ntoa(fromAddr.sin_addr),ntohs(fromAddr.sin_port),nb,buffer);
					exit(EXIT_FAILURE);
				}

				if(sensorType=='G'){
					loadfifoMooving(values[0],alpha_vel,SIZE_VALUES);
					loadfifoMooving(values[1],beta_vel,SIZE_VALUES);
					loadfifoMooving(values[2],gamma_vel,SIZE_VALUES);

					double new_alpha_pos = alpha_pos[0]+alpha_vel[0]*timeValue;
					double new_beta_pos = beta_pos[0]+beta_vel[0]*timeValue;
					double new_gamma_pos = gamma_pos[0]+gamma_vel[0]*timeValue;

					loadfifoMooving(new_alpha_pos,alpha_pos,SIZE_VALUES);
					loadfifoMooving(new_beta_pos,beta_pos,SIZE_VALUES);
					loadfifoMooving(new_gamma_pos,gamma_pos,SIZE_VALUES);

					//fprintf(gp_gyro, "%lf\t%lf\t%lf\n",values[0],values[1],values[2]);
					fprintf(gp_gyro, "%lf\t%lf\t%lf\n",new_alpha_pos,new_beta_pos,new_gamma_pos);
					fflush(gp_gyro);
					fprintf(gp_latency,"%lf\n",timeValue);
					fflush(gp_latency);
				}
				else if(sensorType=='A'){
					loadfifoMooving(values[0],x_accel,SIZE_VALUES);
					loadfifoMooving(values[1],y_accel,SIZE_VALUES);
					loadfifoMooving(values[2],z_accel,SIZE_VALUES);
					fprintf(gp_accel, "%lf\t%lf\t%lf\n",values[0],values[1],values[2]);
					fflush(gp_accel);
				}
				else{
					printf("Wrong sensor type: %c\n",sensorType);
				}

				// jump to next line
				while(*(++sliding_pointer)!='\n');
				sliding_pointer++;

			}

			if(MEASURE_EXECUTION_TIME){
				clock_gettime(CLOCK_REALTIME, &spec);
				endTime = round(spec.tv_nsec / 1.0e3);
				printf("Execution time (ns): %g\n",endTime-startTime);
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

  //----close gnuplot-----
  pclose(gp_accel);
  pclose(gp_gyro);
  pclose(gp_latency);
  if(LOG_TO_FILE){
	  fclose(logfile);
  }
  return 0;

}

