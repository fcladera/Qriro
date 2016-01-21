//=======================================================================
// Includes
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
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
#include <arpa/inet.h>

#include <gsl/gsl_matrix.h>
#include <gsl/gsl_cblas.h>
#include <gsl/gsl_blas.h>

#include "FIFOlib/fifo.h"
#include "TCPlib/TCPServer.h"
#include "Bluetoothlib/BluetoothClient.h"
#include "Qriro-server.h"

//=======================================================================
// Global variables shared between threads and the main program
gsl_matrix *rotationAndTranslation = NULL; // Transformation matrix calculated
Configuration *configuration; // Configuration parameters of Qriro-server
BroadcastMessage *broadcastMessage; // Message from server to phone

#if PLOT_WITH_GNUPLOT
FILE  *gp_screen,  *gp_gyro, *gp_latency;
#endif

volatile bool AndroidsocketSet = false;
volatile bool ApplicationSocketSet = false;

// http://www.gnuplot.info/files/gpReadMouseTest.c <= C y Gnuplot
// feedgnuplot

// Read
// https://github.com/xioTechnologies/Gait-Tracking-With-x-IMU
// http://www.x-io.co.uk/gait-tracking-with-x-imu/
// http://www.x-io.co.uk/open-source-imu-and-ahrs-algorithms/ <= Source code available

int main(int argc, char **argv){
	//=======================================================================
	// Allocate program variables
	configuration = malloc(sizeof(configuration));
	broadcastMessage = malloc(sizeof(broadcastMessage));

	// Allocate transformation matrix memory and set to identity
	rotationAndTranslation = gsl_matrix_calloc(4,4);
	gsl_matrix_set_identity(rotationAndTranslation);

	//=======================================================================
	// Ports for TCP connections
	int portApplication;

	// for TCP mode
	int portAndroid;

  //=======================================================================
  // Determine connection mode with the phone (bluetooth, TCP), port
  // (TCP) and address (BT)

	// check arguments
	if(argc!=4){
		howToUse(argv);
	}

	// Get portApplication number
	if(sscanf(argv[3],"%d",&portApplication)!=1){
			fprintf(stderr,"[TCPportApplication] should be a number!, got: %s\n",argv[3]);
			exit(1);
	}

	if(strcmp(argv[1],"TCP")==0){
		// TCP mode
		printf("TCP mode chosen\n");
		configuration->mode = TCP;

		// Get portAndroid number
		if(sscanf(argv[2],"%d",&portAndroid)!=1){
			fprintf(stderr,"[TCPportAndroid] should be a number!, got: %s\n",argv[2]);
			exit(1);
		}
	}
	else if(strcmp(argv[1],"BT")==0){
		// BT mode
		printf("BT mode chosen\n");
		configuration->mode = BLUETOOTH;
	}
	else{
		howToUse(argv);
	}

	//=======================================================================
	// Socket creation and listening

	//---APPLICATION---------------------------------------------------------
  int listenSocketApplication;
  if (createTCPServer(portApplication, &listenSocketApplication) == -1){
    fprintf(stderr, "Error creating TCP server for Application\n");
    exit(EXIT_FAILURE);
  }

	//---TCP MODE------------------------------------------------------------
	int listenSocketAndroid;
	if(configuration->mode==TCP){
    // Create TCP socket to communicate with the Android phone
    if (createTCPServer(portAndroid, &listenSocketAndroid) == -1){
      fprintf(stderr, "Error creating TCP server for Android\n");
      exit(EXIT_FAILURE);
    }
  }

	//---BLUETOOTH MODE------------------------------------------------------
	int socketBluetooth;
  if(configuration->mode==BLUETOOTH){
    // Connect to the Android device, creating socketBluetooth socket
    if (connectToBTDevice(argv[2], &socketBluetooth) == -1){
      fprintf(stderr, "Error connecting to Android device via Bluetooth\n");
      exit(EXIT_FAILURE);
    }
  }

	//=======================================================================
	// Gnuplot (feedplot) pipe
  #if PLOT_WITH_GNUPLOT

	/* Create a FIFO we later use for communication gnuplot => our program. */
	char * command = "feedgnuplot --lines --stream 0.1 --xlen 1000 --ylabel 'value' --xlabel sample > /dev/null";
	if (NULL == (gp_screen = popen(command,"w"))) {
	  perror("gnuplot");
	  pclose(gp_screen);
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
  #endif

	//=======================================================================
	// Log file (useful to store values from sensors in a file to analyze them later)

  #if LOG_TO_FILE
	FILE *logfile = NULL;

	fprintf(stderr,"WARNING: Log enabled!\nThe values will be stored in points.dat\n");
	logfile = fopen("points.dat","w");
	if (logfile==NULL) perror(__FILE__);
  #endif

	//=======================================================================
	// Program loop
  // Launch threads on connection of both an application or an Android
  // phone

	for(;;){
		fd_set rdSet;
		FD_ZERO(&rdSet);
		int maxFd = listenSocketApplication;
		FD_SET(listenSocketApplication,&rdSet);

		if(configuration->mode==TCP){
			FD_SET(listenSocketAndroid,&rdSet);
			maxFd = maxFd>listenSocketAndroid? maxFd: listenSocketAndroid ;
		}
		if(configuration->mode==BLUETOOTH){
			FD_SET(socketBluetooth,&rdSet);
			maxFd = maxFd>socketBluetooth? maxFd : socketBluetooth;
		}

		if(select(maxFd+1,&rdSet,(fd_set *)0,(fd_set *)0,(struct timeval *)0)==-1){
			perror("select");
			exit(1);
		}
		if(FD_ISSET(listenSocketApplication,&rdSet)){
      // accept new connection from application

			struct sockaddr_in fromAddrApplication;
			socklen_t lenApplication = sizeof(fromAddrApplication);
			int dialogSocketApplication=accept(listenSocketApplication,(struct sockaddr *)&fromAddrApplication,&lenApplication);
			if(dialogSocketApplication==-1){
				perror("AcceptApplication");
				exit(1);
			}

      if (ApplicationSocketSet){
        close(dialogSocketApplication);
      }
      else{
        ApplicationSocketSet = true;
        printf("New Application TCP connection %s:%d\n",
            inet_ntoa(fromAddrApplication.sin_addr),ntohs(fromAddrApplication.sin_port));

        // Start new thread to send the data to the application
        pthread_t thread;
        int *arg = malloc(sizeof(int));
        *arg = dialogSocketApplication;
        int createThread = pthread_create(&thread,(pthread_attr_t *) NULL,applicationThread,arg);
        if(createThread!=0){
          fprintf(stderr,"Error on thread creation\n");
          exit(1);

        }
      }

		}
		if((configuration->mode==TCP)&&FD_ISSET(listenSocketAndroid,&rdSet)){
			// accept a new Android connection
			struct sockaddr_in fromAddrAndroid;
			socklen_t len=sizeof(fromAddrAndroid);
			int dialogSocket=accept(listenSocketAndroid,(struct sockaddr *)&fromAddrAndroid,&len);
			if(dialogSocket==-1){
			  perror("accept");
			  exit(1);
			}
      if (AndroidsocketSet){
        close(dialogSocket);
      }
      else{
        AndroidsocketSet = true;
        printf("Android TCP connection %s:%d\n",
            inet_ntoa(fromAddrAndroid.sin_addr),ntohs(fromAddrAndroid.sin_port));

        pthread_t thread;
        Connection * connection = malloc(sizeof(Connection));
        connection->socket=dialogSocket;
        #if LOG_TO_FILE
        connection->logFile = logfile;
        #endif
        int createThread = pthread_create(&thread,(pthread_attr_t *) NULL,processingThread,(void *)connection);
        if(createThread!=0){
          fprintf(stderr,"Error on thread creation\n");
          exit(1);
        }
      }
		}

		if((configuration->mode==BLUETOOTH)&&(FD_ISSET(socketBluetooth,&rdSet))){
      if (AndroidsocketSet){
        continue;
      }
      else{
        AndroidsocketSet = true;
        pthread_t thread;
        Connection * connection = malloc(sizeof(Connection));
        connection->socket=socketBluetooth;
        #if LOG_TO_FILE
        connection->logFile = logfile;
        #endif
        int createThread = pthread_create(&thread,(pthread_attr_t *) NULL,processingThread,(void *)connection);
        if(createThread!=0){
          fprintf(stderr,"Error on thread creation\n");
          exit(1);
        }
			}
		}
	}

	//---- close listen socket ----
	close(listenSocketApplication);
	if(configuration->mode==TCP){
		close(listenSocketAndroid);
	}
	if(configuration->mode==BLUETOOTH){
		close(socketBluetooth);
	}

  #if PLOT_WITH_GNUPLOT
	//----close gnuplot-----
	pclose(gp_screen);
	pclose(gp_gyro);
	pclose(gp_latency);
  #endif

  #if LOG_TO_FILE
	fclose(logfile);
  #endif

	free(configuration);
	free(broadcastMessage);
	return EXIT_SUCCESS;
}

void *processingThread(void * arg){
	//=======================================================================
	// Program Variables
	Connection * connection = (Connection *)arg;
	int dialogSocket = connection->socket;
  #if LOG_TO_FILE
	FILE * logfile = connection->logFile;
  #endif
	free(connection);

	// Screen, integration
	double 	screen_x = 0,
			screen_y = 0,
			screen_z = 0;

	// Gyro vectors (rotational velocity)
	double 	alpha_vel_buffer[SIZE_VALUES],
			beta_vel_buffer[SIZE_VALUES],
			gamma_vel_buffer[SIZE_VALUES];

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

	//Each time the client connects, the buffers are cleaned
	clearfifo(alpha_vel_buffer,SIZE_VALUES);
	clearfifo(beta_vel_buffer,SIZE_VALUES);
	clearfifo(gamma_vel_buffer,SIZE_VALUES);

	gsl_matrix_set_zero(Rx);
	gsl_matrix_set_zero(Ry);
	gsl_matrix_set_zero(Rz);
	gsl_matrix_set_zero(rot_matrix);
	gsl_matrix_set_zero(RxRy);
	gsl_matrix_set_zero(instantaneous_rotation);
	gsl_matrix_set_identity(previous_rotation);
  gsl_matrix_set_identity(rotationAndTranslation);
	int counter_gyro=0;

	for(;;){

		// Get message from the client
		char buffer[SIZE_BUFFER];
		int i;
		for(i=0;i<SIZE_BUFFER;i++)
			buffer[i]=0;
		int nb=recv(dialogSocket,buffer,SIZE_BUFFER,0);
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
    #if LOG_TO_FILE
		fprintf(logfile,"%s",buffer);
    #endif
		buffer[nb]='\0';

		//printf("from %s %d : %d bytes:\n%s\n",
		//	inet_ntoa(fromAddrAndroid.sin_addr),ntohs(fromAddrAndroid.sin_port),nb,buffer);

		char * sliding_pointer = buffer;
		while (*sliding_pointer!='\0') {
			char sensorType;
			double values[3];
			double timeValue;
			long frameID;

			if((*sliding_pointer!='G')&&(*sliding_pointer!='S')&&(*sliding_pointer!='C')){
				fprintf(stderr,"ERRONEOUS FRAME:\n%s\n",buffer);
				exit(EXIT_FAILURE);
			}

			if(sscanf(sliding_pointer,"%c:%ld:%lf:%lf:%lf:%lf;\n",	&sensorType,&frameID,
										&timeValue,values,values+1,values+2)
										!= 6){
				fprintf(stderr,"ERRONEOUS FRAME:\n%s\n",buffer);
				exit(EXIT_FAILURE);
			}

			if((values[0]==NAN)||(values[1]==NAN)||(values[2]==NAN)){
				fprintf(stderr,"ERRONEOUS FRAME:\n%s\n",buffer);
				exit(EXIT_FAILURE);
			}
			if(sensorType=='C'){
				// Commands, sent to this application or to be broadcasted
				// Read Qriro-server.h
				int id = (int)values[0];
				if(id<1024){
					switch (id) {
						case TOGGLE_FILTER:
							configuration->filterEnabled = !configuration->filterEnabled;
							fprintf(stderr,"Filtering: %d\n",(int)configuration->filterEnabled);
							break;
            case RESET_MATRIX:
              clearfifo(alpha_vel_buffer,SIZE_VALUES);
              clearfifo(beta_vel_buffer,SIZE_VALUES);
              clearfifo(gamma_vel_buffer,SIZE_VALUES);
              gsl_matrix_set_zero(Rx);
              gsl_matrix_set_zero(Ry);
              gsl_matrix_set_zero(Rz);
              gsl_matrix_set_zero(rot_matrix);
              gsl_matrix_set_zero(RxRy);
              gsl_matrix_set_zero(instantaneous_rotation);
              gsl_matrix_set_identity(previous_rotation);
              gsl_matrix_set_identity(rotationAndTranslation);
							fprintf(stderr,"Transformation matrix cleared\n");
              break;

						default:
							fprintf(stderr,"Erroneous command ID: %d\n",id);
							break;
					}
				}
				else if(id<2048){
					broadcastMessage->mesageAvailable = true;
					broadcastMessage->message = id;

				}
				else{
					fprintf(stderr,"Received ID: %d>2048 from phone\n",id);
				}
			}

			else if(sensorType=='G'){

        double alpha_vel,
               beta_vel,
               gamma_vel;

        alpha_vel = values[0];
        beta_vel = values[1];
        gamma_vel = values[2];

				double  alpha_pos_delta,
						beta_pos_delta,
						gamma_pos_delta;

				if(configuration->filterEnabled){
					double 	alpha_vel_filtered,
							beta_vel_filtered,
							gamma_vel_filtered;

          // Load new values in buffer
          loadfifoMooving(alpha_vel,alpha_vel_buffer,SIZE_VALUES);
          loadfifoMooving(beta_vel,beta_vel_buffer,SIZE_VALUES);
          loadfifoMooving(gamma_vel, gamma_vel_buffer,SIZE_VALUES);

					alpha_vel_filtered = FIRfilter(alpha_vel_buffer,firCoefs,32);
					beta_vel_filtered = FIRfilter(beta_vel_buffer,firCoefs,32);
					gamma_vel_filtered = FIRfilter(gamma_vel_buffer,firCoefs,32);

					// Integrate to calculate the instantaneous rotation
					alpha_pos_delta = alpha_vel_filtered*timeValue;
					beta_pos_delta = beta_vel_filtered*timeValue;
					gamma_pos_delta = gamma_vel_filtered*timeValue;
          counter_gyro++;
				}
				else {
					// only integrate last velocity value
					alpha_pos_delta = alpha_vel*timeValue;
					beta_pos_delta = beta_vel*timeValue;
					gamma_pos_delta = gamma_vel*timeValue;
          if (counter_gyro > 0){
            counter_gyro = 0;
          }
				}

				//Calculate rotation matrices
				// Be aware that the axis from the telephone and the axis for the application are not
				// the same!
				// x_application => x_telephone
				// y_application => -z_telephone
				// z_application => y_telephone
				gsl_matrix_set(Rx,0,0,1);
				gsl_matrix_set(Rx,1,1,cos(alpha_pos_delta));
				gsl_matrix_set(Rx,1,2,sin(alpha_pos_delta));
				gsl_matrix_set(Rx,2,1,-sin(alpha_pos_delta));
				gsl_matrix_set(Rx,2,2,cos(alpha_pos_delta));

				gsl_matrix_set(Ry,0,0,cos(gamma_pos_delta));
				gsl_matrix_set(Ry,0,2,sin(gamma_pos_delta));
				gsl_matrix_set(Ry,1,1,1);
				gsl_matrix_set(Ry,2,0,-sin(gamma_pos_delta));
				gsl_matrix_set(Ry,2,2,cos(gamma_pos_delta));

				gsl_matrix_set(Rz,0,0,cos(beta_pos_delta));
				gsl_matrix_set(Rz,0,1,-sin(beta_pos_delta));
				gsl_matrix_set(Rz,1,0,sin(beta_pos_delta));
				gsl_matrix_set(Rz,1,1,cos(beta_pos_delta));
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

				double	m00 = gsl_matrix_get(rot_matrix,0,0),
					m01 = gsl_matrix_get(rot_matrix,0,1),
					m02 = gsl_matrix_get(rot_matrix,0,2),
					m10 = gsl_matrix_get(rot_matrix,1,0),
					m11 = gsl_matrix_get(rot_matrix,1,1),
					m12 = gsl_matrix_get(rot_matrix,1,2),
					m20 = gsl_matrix_get(rot_matrix,2,0),
					m21 = gsl_matrix_get(rot_matrix,2,1),
					m22 = gsl_matrix_get(rot_matrix,2,2);

				gsl_matrix_set(rotationAndTranslation,0,0,m00);
				gsl_matrix_set(rotationAndTranslation,0,1,m01);
				gsl_matrix_set(rotationAndTranslation,0,2,m02);
				gsl_matrix_set(rotationAndTranslation,1,0,m10);
				gsl_matrix_set(rotationAndTranslation,1,1,m11);
				gsl_matrix_set(rotationAndTranslation,1,2,m12);
				gsl_matrix_set(rotationAndTranslation,2,0,m20);
				gsl_matrix_set(rotationAndTranslation,2,1,m21);
				gsl_matrix_set(rotationAndTranslation,2,2,m22);

        #if PLOT_WITH_GNUPLOT
				fprintf(gp_gyro, "%lf\t%lf\t%lf\n",RAD_TO_DEG(alpha_pos_delta),RAD_TO_DEG(beta_pos_delta),RAD_TO_DEG(gamma_pos_delta));
				fflush(gp_gyro);
				fprintf(gp_latency,"%lf\n",timeValue);
				fflush(gp_latency);
        #endif

			}
			else if(sensorType=='S'){
				const float xy_scale=0.2;
				const float z_scale=3;

				screen_x = xy_scale*values[0]+screen_x;
				screen_y = xy_scale*values[1]+screen_y;
				screen_z = z_scale*values[2]+screen_z;

				gsl_matrix_set(rotationAndTranslation,0,3,screen_x);
				gsl_matrix_set(rotationAndTranslation,1,3,screen_y);
				gsl_matrix_set(rotationAndTranslation,2,3,screen_z);

        #if PLOT_WITH_GNUPLOT
				fprintf(gp_screen, "%lf\t%lf\t%lf\n",screen_x,screen_y,screen_z);
				fflush(gp_screen);
        #endif

			}
			else{
				fprintf(stderr,"Wrong sensor type: %c\n",sensorType);
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
	}

	//---- close dialog socket ----
	printf("client disconnected\n");
	close(dialogSocket);

	gsl_matrix_free(Rx);
	gsl_matrix_free(Ry);
	gsl_matrix_free(Rz);
	gsl_matrix_free(rot_matrix);
	gsl_matrix_free(RxRy);
	gsl_matrix_free(instantaneous_rotation);
	AndroidsocketSet = false;
	return (void *)0;
}

void * applicationThread(void * arg){
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

		// send broadcast if available
    if (strncmp(buffer, "GETCOM", 6)  == 0){
      if(broadcastMessage->mesageAvailable){
        nb = sprintf(buffer,"COM:%d;\n",broadcastMessage->message);
        broadcastMessage->mesageAvailable=false;
      }
      else{
        nb = sprintf(buffer,"NOCOM\n");
      }
    }

    else if(strncmp(buffer,"GETMAT",6)==0){
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
			nb=sprintf(buffer,"This is not a valid command\n");
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

	ApplicationSocketSet = false;
	return (void *)0;
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

void howToUse(char **argv){
	fprintf(stderr,"Please use:\n\n");
	fprintf(stderr,"%s TCP [TCPportAndroid] [TCPportApplication]\n",argv[0]);
	fprintf(stderr,"for TCP connection with the phone or\n\n");
	fprintf(stderr,"%s BT [BTaddressAndroid] [TCPportApplication]\n",argv[0]);
	fprintf(stderr,"for Bluetooth connection with the phone\n");
	exit(EXIT_FAILURE);
}
