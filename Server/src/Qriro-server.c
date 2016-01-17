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
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>
#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>

#include <gsl/gsl_matrix.h>
#include <gsl/gsl_cblas.h>
#include <gsl/gsl_blas.h>

#include "FIFOlib/fifo.h"
#include "Qriro-server.h"

//=======================================================================
// Global variables
gsl_matrix *rotationAndTranslation = NULL; // Transformation matrix calculated
Configuration *configuration; // Configuration parameters of Qriro-server
BroadcastMessage *broadcastMessage; // Message from server to phone

volatile bool socketSet = false;
// http://www.gnuplot.info/files/gpReadMouseTest.c <= C y Gnuplot
// feedgnuplot

// Read
// https://github.com/xioTechnologies/Gait-Tracking-With-x-IMU
// http://www.x-io.co.uk/gait-tracking-with-x-imu/
// http://www.x-io.co.uk/open-source-imu-and-ahrs-algorithms/ <= Source code available

int main(int argc, char **argv){
	//=======================================================================
	// Program variables allocation
	configuration = malloc(sizeof(configuration));
	broadcastMessage = malloc(sizeof(broadcastMessage));

	//=======================================================================
	// Connection variables
	int portApplication;

	// for TCP mode
	int portAndroid;

	//=======================================================================
	// Determine connection mode with the phone (bluetooth, TCP), port (TCP) and address (BT)

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

	// Listen socket for the Application
	int listenSocketApplication = socket(PF_INET,SOCK_STREAM,0);
	if(listenSocketApplication==-1){
		perror("socketApplication");
		exit(1);
	}

	// Avoid problems if the program is quickly restarted
	// http://stackoverflow.com/questions/14388706/socket-options-so-reuseaddr-and-so-reuseport-how-do-they-differ-do-they-mean-t
	const int on=1;
	if(setsockopt(listenSocketApplication,SOL_SOCKET,SO_REUSEADDR,&on,sizeof(int))==-1){
		perror("setsockoptApplication");
		exit(1);
	}

	// bound to any local address on the specified port
	struct sockaddr_in myAddrApplication;
		myAddrApplication.sin_family=AF_INET;
		myAddrApplication.sin_port=htons(portApplication);
		myAddrApplication.sin_addr.s_addr=htonl(INADDR_ANY);
		if(bind(listenSocketApplication,(struct sockaddr *)&myAddrApplication,sizeof(myAddrApplication))==-1){
			perror("bindApplication");
			exit(1);

	}

	// Accept connections
	if(listen(listenSocketApplication,10)==-1){
			perror("listenApplication");
			exit(1);
	}

	//---TCP MODE------------------------------------------------------------
	int listenSocketAndroid=-1;
	if(configuration->mode==TCP){	// Create TCP socket for the program
		// Listen socket for Android
		listenSocketAndroid = socket(PF_INET,SOCK_STREAM,0);
		if(listenSocketAndroid==-1){
			perror("socketAndroid");
			exit(1);
		}

		// Avoid problems if the program is quickly restarted
		const int on=1;
		if(setsockopt(listenSocketAndroid,SOL_SOCKET,SO_REUSEADDR,&on,sizeof(int))==-1){
			perror("setsockoptAndroid");
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

		// Accepts connections
		if(listen(listenSocketAndroid,10)==-1){
			perror("listenAndroid");
			exit(1);
		}
	}

	//---BLUETOOTH MODE------------------------------------------------------
	int socketBluetooth=-1;
	if(configuration->mode==BLUETOOTH){
		// Get port number of the application with sdp
		uint8_t svc_uuid_int[] = {	0x2f, 0xa7, 0xbe, 0xb1,
									0x6a, 0xcf,
									0x47, 0x03,
									0x8b, 0x7e,
									0xdc, 0xbc, 0x14, 0xf0, 0x7b, 0x89 };
		int status;
		bdaddr_t target;
		uuid_t svc_uuid;
		sdp_list_t *response_list, *search_list, *attrid_list;
		sdp_session_t *session = 0;
		uint32_t range = 0x0000ffff;
		uint8_t port = 0;
		str2ba( argv[2], &target );
		// connect to the SDP server running on the remote machine
		session = sdp_connect( BDADDR_ANY, &target, 0 );

		sdp_uuid128_create( &svc_uuid, &svc_uuid_int );
		search_list = sdp_list_append( 0, &svc_uuid );
		attrid_list = sdp_list_append( 0, &range );

		// get a list of service records that have the UUID svc_uuid_int
		response_list = NULL;
		status = sdp_service_search_attr_req( session, search_list,
				SDP_ATTR_REQ_RANGE, attrid_list, &response_list);

		if( status == 0 ) {
			sdp_list_t *proto_list = NULL;
			sdp_list_t *r = response_list;

			// go through each of the service records
			for (; r; r = r->next ) {
				sdp_record_t *rec = (sdp_record_t*) r->data;

				// get a list of the protocol sequences
				if( sdp_get_access_protos( rec, &proto_list ) == 0 ) {

					// get the RFCOMM port number
					port = sdp_get_proto_port( proto_list, RFCOMM_UUID );
					sdp_list_free( proto_list, 0 );
				}
				sdp_record_free( rec );
			}
		}
		sdp_list_free( response_list, 0 );
		sdp_list_free( search_list, 0 );
		sdp_list_free( attrid_list, 0 );
		sdp_close( session );

		if( port != 0 ) {
			printf("Service running on RFCOMM port %d\n", port);
		}
		else{
			fprintf(stderr,"ERROR: RFCOMM application not found!\n");
			exit(EXIT_FAILURE);
		}

		// Create bluetooth socket
		struct sockaddr_rc addr = {0};

		// allocate a socket
		socketBluetooth = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
		if(socketBluetooth==-1){
			perror("socketBluetooth");
			exit(EXIT_FAILURE);
		}


		// set the connection parameters
		addr.rc_family = AF_BLUETOOTH;
		addr.rc_channel = port;
		str2ba( argv[2], &addr.rc_bdaddr );

		// connect to server
		if(connect(socketBluetooth, (struct sockaddr *)&addr, sizeof(addr)) == -1){
			perror("Connection to BT phone");
			exit(EXIT_FAILURE);
		}
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

	// Clear global matrix qnd set some constant values
	rotationAndTranslation = gsl_matrix_calloc(4,4);
	gsl_matrix_set_identity(rotationAndTranslation);

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
		if(FD_ISSET(listenSocketApplication,&rdSet)){	// accept new connection from application
			struct sockaddr_in fromAddrApplication;
			socklen_t lenApplication = sizeof(fromAddrApplication);
			int dialogSocketApplication=accept(listenSocketApplication,(struct sockaddr *)&fromAddrApplication,&lenApplication);
			if(dialogSocketApplication==-1){
				perror("AcceptApplication");
				exit(1);
			}
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
		if((configuration->mode==TCP)&&FD_ISSET(listenSocketAndroid,&rdSet)&&(socketSet==false)){
			// accept a new Android connection
			struct sockaddr_in fromAddrAndroid;
			socklen_t len=sizeof(fromAddrAndroid);
			int dialogSocket=accept(listenSocketAndroid,(struct sockaddr *)&fromAddrAndroid,&len);
			if(dialogSocket==-1){
			  perror("accept");
			  exit(1);
			}
			printf("Android TCP connection %s:%d\n",
			  inet_ntoa(fromAddrAndroid.sin_addr),ntohs(fromAddrAndroid.sin_port));

			pthread_t thread;
			Connection * connection = malloc(sizeof(Connection));
			connection->socket=dialogSocket;
			connection->logFile = logfile;
			socketSet = true;
			int createThread = pthread_create(&thread,(pthread_attr_t *) NULL,processingThread,(void *)connection);
			if(createThread!=0){
				fprintf(stderr,"Error on thread creation\n");
				exit(1);
			}
		}

		if((configuration->mode==BLUETOOTH)&&(FD_ISSET(socketBluetooth,&rdSet))&&(socketSet==false)){
			pthread_t thread;
			Connection * connection = malloc(sizeof(Connection));
			connection->socket=socketBluetooth;
			connection->logFile = logfile;
			socketSet = true;
			int createThread = pthread_create(&thread,(pthread_attr_t *) NULL,processingThread,(void *)connection);
			if(createThread!=0){
				fprintf(stderr,"Error on thread creation\n");
				exit(1);

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

	//----close gnuplot-----
	pclose(gp_accel);
	pclose(gp_gyro);
	pclose(gp_latency);

	if(LOG_TO_FILE){
	  fclose(logfile);
	}
	free(configuration);
	free(broadcastMessage);
	return EXIT_SUCCESS;
}

void *processingThread(void * arg){
	//=======================================================================
	// Program Variables
	Connection * connection = (Connection *)arg;
	int dialogSocket = connection->socket;
	FILE * logfile = connection->logFile;
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
				}
				else {
					// only integrate last velocity value
					alpha_pos_delta = alpha_vel*timeValue;
					beta_pos_delta = beta_vel*timeValue;
					gamma_pos_delta = gamma_vel*timeValue;
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

				//fprintf(gp_gyro, "%lf\t%lf\t%lf\n",toDegrees(new_alpha_pos),toDegrees(new_beta_pos),toDegrees(new_gamma_pos));
				//fflush(gp_gyro);
				//fprintf(gp_latency,"%lf\n",timeValue);
				//fflush(gp_latency);

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

				//fprintf(gp_accel, "%lf\t%lf\t%lf\n",screen_x,screen_y,screen_z);
				//fflush(gp_accel);

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

	gsl_matrix_free(Rx);
	gsl_matrix_free(Ry);
	gsl_matrix_free(Rz);
	gsl_matrix_free(rot_matrix);
	gsl_matrix_free(RxRy);
	gsl_matrix_free(instantaneous_rotation);
	socketSet = false;
	return (void *)0;
}

void * applicationThread(void * arg){
	pthread_detach(pthread_self());
	int socket =*(int *) arg;
	free(arg);
	for(;;){
		// Get ask msg from the client
		char buffer[SIZE_VALUES];
		char broadcastBuffer[SIZE_VALUES];
		int nb=recv(socket,buffer,SIZE_VALUES,0);
		if(nb<=0){
			break;
		}
		buffer[nb]='\0';

		// Before, check and broadcast if available
		if(broadcastMessage->mesageAvailable){
			nb = sprintf(broadcastBuffer,"COM:%d;\n",broadcastMessage->message);
			if(send(socket,broadcastBuffer,nb,0)==-1){
							perror("sendThreadBroadcast");
							exit(1);
				}
			broadcastMessage->mesageAvailable=false;
		}


		// Then answer query
		//printf("%s\n",buffer);
		if(strncmp(buffer,"GETMAT",6)==0){
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
			nb=sprintf(buffer,"ERR\n");
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

void howToUse(char **argv){
	fprintf(stderr,"Please use:\n\n");
	fprintf(stderr,"%s TCP [TCPportAndroid] [TCPportApplication]\n",argv[0]);
	fprintf(stderr,"for TCP connection with the phone or\n\n");
	fprintf(stderr,"%s BT [BTaddressAndroid] [TCPportApplication]\n",argv[0]);
	fprintf(stderr,"for Bluetooth connection with the phone\n");
	exit(EXIT_FAILURE);
}
