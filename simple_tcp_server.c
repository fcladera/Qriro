//=======================================================================
// Includes
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

#include <gsl/gsl_matrix.h>
#include <gsl/gsl_cblas.h>
#include <gsl/gsl_blas.h>

#include "fifo.h"
#include "simple_tcp_server.h"


//=======================================================================
// Program parameters

#define SIZE_VALUES 256
#define SIZE_TCP_BUFFER 0x1000

#define LOG_TO_FILE 0
#define MEASURE_EXECUTION_TIME 0

//=======================================================================
// Global variables
gsl_matrix *rotationAndTranslation = NULL;	// matrix shared by the android server and the application server


// http://www.gnuplot.info/files/gpReadMouseTest.c <= C y Gnuplot
// feedgnuplot

// Read
// https://github.com/xioTechnologies/Gait-Tracking-With-x-IMU
// http://www.x-io.co.uk/gait-tracking-with-x-imu/
// http://www.x-io.co.uk/open-source-imu-and-ahrs-algorithms/ <= Source code available


// TODO
/*
 * Improve reception - Using a cable less packets are lost?
 * Convert to physic units! => Almost done...
 * Basic integration. Show drift error in position
 * Get information from the magnetometer
 * Read imu and ahrs algorithms
 */


//


void * processingThread(void * arg){
	pthread_detach(pthread_self());
	int socket =*(int *) arg;
	free(arg);
	for(;;){
		// Get ask msg from the client
		char buffer[SIZE_VALUES];
		int nb=recv(socket,buffer,SIZE_VALUES,0);
		if(nb<=0){
			break;
		}
		buffer[nb]='\0';
		if(strncmp(buffer,"VALS",4)==0){
			// Send rotationAndTranslation matrix to the client. Only the useful data!
			if(rotationAndTranslation!=NULL){
				//printf("matrix asked");
				nb = sprintf(buffer,"MAT:%g:%g:%g:%g:%g:%g:%g:%g:%g:%g:%g:%g:%g:%g:%g:%g;\n",
							gsl_matrix_get(rotationAndTranslation,0,0),
							gsl_matrix_get(rotationAndTranslation,0,1),
							gsl_matrix_get(rotationAndTranslation,0,2),
							gsl_matrix_get(rotationAndTranslation,0,3),
							gsl_matrix_get(rotationAndTranslation,1,0),
							gsl_matrix_get(rotationAndTranslation,1,1),
							gsl_matrix_get(rotationAndTranslation,1,2),
							gsl_matrix_get(rotationAndTranslation,1,3),
							gsl_matrix_get(rotationAndTranslation,2,0),
							gsl_matrix_get(rotationAndTranslation,2,1),
							gsl_matrix_get(rotationAndTranslation,2,2),
							gsl_matrix_get(rotationAndTranslation,2,3),
							gsl_matrix_get(rotationAndTranslation,3,0),
							gsl_matrix_get(rotationAndTranslation,3,1),
							gsl_matrix_get(rotationAndTranslation,3,2),
							gsl_matrix_get(rotationAndTranslation,3,3));
			}

		}
		else{
			nb=sprintf(buffer,"Erroneous command \n");
		}

		// Send answer to client
		if(send(socket,buffer,nb,0)==-1){
			perror("sendThread");
			exit(1);
		}
	}

	//close dialog socket
	printf("Application disconnected\n");
	close(socket);
	return (void *)0;
}

int main(int argc, char **argv){

	//=======================================================================
	// Socket creation and listening

	// check arguments
	if(argc != 3){
		fprintf(stderr,"Please use: %s portAndroid portApplication\n",argv[0]);
		exit(1);
	}

	// Get port numbers
	int portAndroid, portApplication;
	if(sscanf(argv[1],"%d",&portAndroid)!=1){
		fprintf(stderr,"portAndroid should be a number!: %s\n",argv[1]);
		exit(1);
	}
	if(sscanf(argv[2],"%d",&portApplication)!=1){
			fprintf(stderr,"portApplication should be a number!: %s\n",argv[1]);
			exit(1);
	}

	// Listen socket for Android
	int listenSocketAndroid = socket(PF_INET,SOCK_STREAM,0);
	if(listenSocketAndroid==-1){
		perror("socketAndroid");
		exit(1);
	}

	// Listen socket for the Application
	int listenSocketApplication = socket(PF_INET,SOCK_STREAM,0);
	if(listenSocketApplication==-1){
		perror("socketApplication");
		exit(1);
	}

	// Avoid problems if the program is quickly restarted
	// http://stackoverflow.com/questions/14388706/socket-options-so-reuseaddr-and-so-reuseport-how-do-they-differ-do-they-mean-t
	const int on=1;
	if(setsockopt(listenSocketAndroid,SOL_SOCKET,SO_REUSEADDR,&on,sizeof(int))==-1){
		perror("setsockoptAndroid");
		exit(1);
	}
	if(setsockopt(listenSocketApplication,SOL_SOCKET,SO_REUSEADDR,&on,sizeof(int))==-1){
		perror("setsockoptApplication");
		exit(1);
	}


	// bound to any local address on the specified port
	struct sockaddr_in myAddrAndroid;
	myAddrAndroid.sin_family=AF_INET;
	myAddrAndroid.sin_port=htons(portAndroid);
	myAddrAndroid.sin_addr.s_addr=htonl(INADDR_ANY);
	if(bind(listenSocketAndroid,(struct sockaddr *)&myAddrAndroid,sizeof(myAddrAndroid))==-1){
		perror("bindAndroid");
		exit(1);

	}
	struct sockaddr_in myAddrApplication;
	myAddrApplication.sin_family=AF_INET;
	myAddrApplication.sin_port=htons(portApplication);
	myAddrApplication.sin_addr.s_addr=htonl(INADDR_ANY);
	if(bind(listenSocketApplication,(struct sockaddr *)&myAddrApplication,sizeof(myAddrApplication))==-1){
		perror("bindApplication");
		exit(1);

	}

	// Accepts connections on both sockets
	if(listen(listenSocketAndroid,10)==-1){
		perror("listenAndroid");
		exit(1);
	}

	if(listen(listenSocketApplication,10)==-1){
			perror("listenApplication");
			exit(1);
	}


	//=======================================================================
	// Gnuplot (feedplot) pipe

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

	//=======================================================================
	// Log file (useful to store values from sensors in a file to analyze them later)

	FILE *logfile = NULL;
	if(LOG_TO_FILE){
		fprintf(stderr,"WARNING: Log enabled!\nThe values will be stored in points.dat\n");
		logfile = fopen("points.dat","w");
		if (logfile==NULL) perror(__FILE__);
	}

	//=======================================================================
	// Program Variables

	// Screen vectors (velocity)
	double 	x_vel[SIZE_VALUES],
			y_vel[SIZE_VALUES],
			z_vel[SIZE_VALUES];

	// Screen, integration
	double 	screen_x = 0,
			screen_y = 0,
			screen_z = 0;

	// Gyro vectors (rotational velocity)
	double 	alpha_vel[SIZE_VALUES],
			beta_vel[SIZE_VALUES],
			gamma_vel[SIZE_VALUES];

	// rotational velocity, constant component
	double 	alpha_vel_st=NAN,
			beta_vel_st=NAN,
			gamma_vel_st=NAN;

	// Rotation matrices
	gsl_matrix 	*Rx = gsl_matrix_calloc(3,3),
				*Ry = gsl_matrix_calloc(3,3),
				*Rz = gsl_matrix_calloc(3,3),
				*rot_matrix = gsl_matrix_calloc(3,3),
				*previous_rotation = gsl_matrix_calloc(3,3),
				*RxRy = gsl_matrix_calloc(3,3),
				*instantaneous_rotation = gsl_matrix_calloc(3,3);



	// Time variables, useful to get system time
	struct timespec spec;
	double 	startTime,
			endTime;

	// Clear global matrix qnd set some constant values
	rotationAndTranslation = gsl_matrix_calloc(4,4);
	gsl_matrix_set(rotationAndTranslation,3,3,1.0);


	for(;;){
		fd_set rdSet;
		FD_ZERO(&rdSet);
		FD_SET(listenSocketAndroid,&rdSet);
		FD_SET(listenSocketApplication,&rdSet);
		int maxFd = listenSocketAndroid>listenSocketApplication ? listenSocketAndroid:listenSocketApplication;
		if(select(maxFd+1,&rdSet,(fd_set *)0,(fd_set *)0,(struct timeval *)0)==-1){
			perror("select");
			exit(1);
		}
		if(FD_ISSET(listenSocketApplication,&rdSet)){	// accept new connection from application
			struct sockaddr_in fromAddrApplication;
			socklen_t lenApplication = sizeof(fromAddrApplication);
			int dialogSocketApplication=accept(listenSocketApplication,(struct sockaddr *)&fromAddrApplication,&lenApplication);
			if(dialogSocketApplication==-1){
				perror("AcceptApplication");
				exit(1);
			}
			printf("New Application connection %s:%d\n",
					  inet_ntoa(fromAddrApplication.sin_addr),ntohs(fromAddrApplication.sin_port));

			// Start new thread to send the data to the application
			pthread_t thread;
			int *arg = malloc(sizeof(int));
			*arg = dialogSocketApplication;
			int createThread = pthread_create(&thread,(pthread_attr_t *) NULL,processingThread,arg);
			if(createThread!=0){
				fprintf(stderr,"Error on thread creation\n");
				exit(1);

			}

		}

		if(FD_ISSET(listenSocketAndroid,&rdSet)){
			//Each time the client connects, the buffers are cleaned
			clearfifo(alpha_vel,SIZE_VALUES);
			clearfifo(beta_vel,SIZE_VALUES);
			clearfifo(gamma_vel,SIZE_VALUES);
			alpha_vel_st=NAN,
			beta_vel_st=NAN,
			gamma_vel_st=NAN;

			// accept a new Android connection
			struct sockaddr_in fromAddrAndroid;
			socklen_t len=sizeof(fromAddrAndroid);
			int dialogSocket=accept(listenSocketAndroid,(struct sockaddr *)&fromAddrAndroid,&len);
			if(dialogSocket==-1){
			  perror("accept");
			  exit(1);
			}
			printf("Android connection %s:%d\n",
			  inet_ntoa(fromAddrAndroid.sin_addr),ntohs(fromAddrAndroid.sin_port));

			gsl_matrix_set_identity(previous_rotation);

			int counter_gyro=0;
			for(;;){
				gsl_matrix_set_zero(Rx);
				gsl_matrix_set_zero(Ry);
				gsl_matrix_set_zero(Rz);
				gsl_matrix_set_zero(rot_matrix);
				gsl_matrix_set_zero(RxRy);
				gsl_matrix_set_zero(instantaneous_rotation);

				// Get message from the client
				char buffer[SIZE_TCP_BUFFER];
				for(int i=0;i<SIZE_TCP_BUFFER;i++)
					buffer[i]=0;
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
				//	inet_ntoa(fromAddrAndroid.sin_addr),ntohs(fromAddrAndroid.sin_port),nb,buffer);

				char * sliding_pointer = buffer;
				while (*sliding_pointer!='\0') {
					char sensorType;
					double values[3];
					double timeValue;
					long frameID;

					if((*sliding_pointer!='G')&&(*sliding_pointer!='S')){
						fprintf(stderr,"ERRONEOUS FRAME: from %s %d : %d bytes:\n%s\n",
								inet_ntoa(fromAddrAndroid.sin_addr),ntohs(fromAddrAndroid.sin_port),nb,buffer);
						exit(EXIT_FAILURE);
					}

					if( sscanf(sliding_pointer,"%c:%ld:%lf:%lf:%lf:%lf;\n",&sensorType,&frameID,&timeValue,values,values+1,values+2) != 6){
						fprintf(stderr,"Invalid line format?: from %s %d : %d bytes:\n%s\n",
								inet_ntoa(fromAddrAndroid.sin_addr),ntohs(fromAddrAndroid.sin_port),nb,buffer);
						exit(EXIT_FAILURE);
					}

					if((values[0]==NAN)||(values[1]==NAN)||(values[2]==NAN)){
						fprintf(stderr,"NAN numbers found: from %s %d : %d bytes:\n%s\n",
								inet_ntoa(fromAddrAndroid.sin_addr),ntohs(fromAddrAndroid.sin_port),nb,buffer);
						exit(EXIT_FAILURE);
					}

					if(sensorType=='G'){

						if(counter_gyro<SIZE_VALUES){
							loadfifoMooving(values[0],alpha_vel,SIZE_VALUES);
							loadfifoMooving(values[1],beta_vel,SIZE_VALUES);
							loadfifoMooving(values[2],gamma_vel,SIZE_VALUES);
							counter_gyro++;
						}
						else if(counter_gyro==SIZE_VALUES){
							alpha_vel_st = sumfifo(alpha_vel,SIZE_VALUES)/(double)SIZE_VALUES;
							beta_vel_st = sumfifo(beta_vel,SIZE_VALUES)/(double)SIZE_VALUES;
							gamma_vel_st = sumfifo(gamma_vel,SIZE_VALUES)/(double)SIZE_VALUES;
							counter_gyro++;
							printf("Calibrating Gyro...%lf,%lf,%lf\n",alpha_vel_st,beta_vel_st,gamma_vel_st);
							//printfifo(alpha_vel,SIZE_VALUES);
						}
						else{

							// Store current angular velocity in a fifo
							loadfifoMooving(values[0]-alpha_vel_st,alpha_vel,SIZE_VALUES);
							loadfifoMooving(values[1]-beta_vel_st,beta_vel,SIZE_VALUES);
							loadfifoMooving(values[2]-gamma_vel_st,gamma_vel,SIZE_VALUES);

							// Integrate to calculate the instantaneous rotation
							double alpha_pos_delta = alpha_vel[0]*timeValue;
							double beta_pos_delta = beta_vel[0]*timeValue;
							double gamma_pos_delta = gamma_vel[0]*timeValue;

							//Calculate rotation matrices
							gsl_matrix_set(Rx,0,0,1);
							gsl_matrix_set(Rx,1,1,cos(alpha_pos_delta));
							gsl_matrix_set(Rx,1,2,-sin(alpha_pos_delta));
							gsl_matrix_set(Rx,2,1,sin(alpha_pos_delta));
							gsl_matrix_set(Rx,2,2,cos(alpha_pos_delta));

							gsl_matrix_set(Ry,0,0,cos(beta_pos_delta));
							gsl_matrix_set(Ry,0,2,sin(beta_pos_delta));
							gsl_matrix_set(Ry,1,1,1);
							gsl_matrix_set(Ry,2,0,-sin(beta_pos_delta));
							gsl_matrix_set(Ry,2,2,cos(beta_pos_delta));

							gsl_matrix_set(Rz,0,0,cos(gamma_pos_delta));
							gsl_matrix_set(Rz,0,1,-sin(gamma_pos_delta));
							gsl_matrix_set(Rz,1,0,sin(gamma_pos_delta));
							gsl_matrix_set(Rz,1,1,cos(gamma_pos_delta));
							gsl_matrix_set(Rz,2,2,1);

							gsl_blas_dgemm(CblasNoTrans,CblasNoTrans,
													1.0, Rx,Ry,
													0.0, RxRy);
							gsl_blas_dgemm(CblasNoTrans,CblasNoTrans,
													1.0, RxRy,Rz,
													0.0, instantaneous_rotation);

							// Add the instantaneous rotation to the previous one
							gsl_blas_dgemm(CblasNoTrans,CblasNoTrans,
										1.0, previous_rotation,instantaneous_rotation,
										0.0, rot_matrix);
							gsl_matrix_memcpy(previous_rotation,rot_matrix);
							//printMatrix(rot_matrix);

							gsl_matrix_set(rotationAndTranslation,0,0,gsl_matrix_get(rot_matrix,0,0));
							gsl_matrix_set(rotationAndTranslation,0,1,gsl_matrix_get(rot_matrix,0,1));
							gsl_matrix_set(rotationAndTranslation,0,2,gsl_matrix_get(rot_matrix,0,2));
							gsl_matrix_set(rotationAndTranslation,1,0,gsl_matrix_get(rot_matrix,1,0));
							gsl_matrix_set(rotationAndTranslation,1,1,gsl_matrix_get(rot_matrix,1,1));
							gsl_matrix_set(rotationAndTranslation,1,2,gsl_matrix_get(rot_matrix,1,2));
							gsl_matrix_set(rotationAndTranslation,2,0,gsl_matrix_get(rot_matrix,2,0));
							gsl_matrix_set(rotationAndTranslation,2,1,gsl_matrix_get(rot_matrix,2,1));
							gsl_matrix_set(rotationAndTranslation,2,2,gsl_matrix_get(rot_matrix,2,2));

							//fprintf(gp_gyro, "%lf\t%lf\t%lf\n",toDegrees(new_alpha_pos),toDegrees(new_beta_pos),toDegrees(new_gamma_pos));
							//fflush(gp_gyro);
							//fprintf(gp_latency,"%lf\n",timeValue);
							//fflush(gp_latency);


						}

					}
					else if(sensorType=='S'){

						loadfifoMooving(values[0],x_vel,SIZE_VALUES);
						loadfifoMooving(values[1],y_vel,SIZE_VALUES);
						loadfifoMooving(values[2],z_vel,SIZE_VALUES);

						screen_x = values[0]+screen_x;
						screen_y = values[1]+screen_y;
						screen_z = values[2]+screen_z;


						gsl_matrix_set(rotationAndTranslation,0,3,screen_x);
						gsl_matrix_set(rotationAndTranslation,1,3,screen_y);
						gsl_matrix_set(rotationAndTranslation,2,3,screen_z);

						fprintf(gp_accel, "%lf\t%lf\t%lf\n",screen_x,screen_y,screen_z);
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
	}



  //---- close listen socket ----
  close(listenSocketAndroid);
  close(listenSocketApplication);

  //----close gnuplot-----
  pclose(gp_accel);
  pclose(gp_gyro);
  pclose(gp_latency);


  gsl_matrix_free(Rx);
  gsl_matrix_free(Ry);
  gsl_matrix_free(Rz);
  gsl_matrix_free(rot_matrix);
  gsl_matrix_free(RxRy);
  gsl_matrix_free(instantaneous_rotation);


  if(LOG_TO_FILE){
	  fclose(logfile);
  }
  return 0;
}

double toDegrees(double radians){
	return (radians*180./M_PI);
}

void printMatrix(gsl_matrix *A){

	int row, column;
	int rows = A->size1;
	int columns = A->size2;
	for(row=0;row<rows;row++){
		for(column = 0;column<columns;column++){
			printf("%f\t",gsl_matrix_get(A,row,column));
		}
		printf("\n");
	}
	printf("\n");
}


