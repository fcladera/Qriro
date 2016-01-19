/*
 * Qriro-server.h
 *
 *  Created on: Nov 10, 2013
 *      Author: fclad
 */

#ifndef QRIRO_SERVER_H_
#define QRIRO_SERVER_H_


//=======================================================================
// Program parameters

#define SIZE_VALUES 256
#define SIZE_BUFFER 0x1000

#define LOG_TO_FILE 0
#define MEASURE_EXECUTION_TIME 0
#define PLOT_WITH_GNUPLOT 0

//=======================================================================
// Functions

double toDegrees(double);
void printMatrix(gsl_matrix * A);
void howToUse();

void *processingThread(void * arg);
void *applicationThread(void * arg);

//=======================================================================
// Typedefs

typedef int bool;
#define true 1
#define false 0

typedef enum x{TCP,BLUETOOTH} Mode;

typedef struct configuration{
	volatile Mode mode;	// mode TCP or bluetooth
	volatile bool filterEnabled;	// enable gyro filtering
} Configuration;

typedef struct connection{
	int socket;
	FILE * logFile;
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
