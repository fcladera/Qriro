/*
 * Qriro-server.h
 *
 *  Created on: Nov 10, 2013
 *      Author: fclad
 */

#ifndef QRIRO_SERVER_H_
#define QRIRO_SERVER_H_
//=======================================================================
// Includes
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <unistd.h>
#include <errno.h>
#include <inttypes.h>
#include <fcntl.h>
#include <signal.h>
#include <pthread.h>
#include <time.h>
#include <math.h>
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
#include "GnuPlotUtils/GnuPlotUtils.h"


//=======================================================================
// Program parameters

#define SIZE_VALUES 256
#define SIZE_BUFFER 0x1000

#define LOG_TO_FILE 0
#define MEASURE_EXECUTION_TIME 0
#define PLOT_WITH_GNUPLOT 0

//=======================================================================
// Macros
#define RAD_TO_DEG(radians) ((radians)*(180.0/M_PI))

//=======================================================================
// Functions

void printMatrix(gsl_matrix * A);
void howToUse();

void *processingThread(void * arg);
void *applicationThread(void * arg);

//=======================================================================
// Typedefs

typedef enum x{TCP,BLUETOOTH} Mode;

typedef struct configuration{
	volatile Mode mode;	// mode TCP or bluetooth
	volatile bool filterEnabled;	// enable gyro filtering
} Configuration;

typedef struct connection{
	int socket;
  #if LOG_TO_FILE
	FILE * logFile;
  #endif
} Connection;

typedef struct broadcastMessage{
	volatile bool mesageAvailable;
	int message;

} BroadcastMessage;

//=======================================================================
// Messages
// Messages with ID from 1 to 1023 are generated in the phone, sent
// to Qriro-server, and are not broadcasted to the application
#define TOGGLE_FILTER 1
#define RESET_MATRIX 2
// Messages with ID  from 1024 to 2047 are generated in the phone, sent
// to Qriro-server, and are broadcasted to the application
#define DOUBLE_TAP 1024+1
// Messages with ID bigger than 2049 are generated in Qriro-server and
// sent to the application

#endif /* QRIRO_SERVER_H_*/
