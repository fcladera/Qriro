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
// Messages with ID from 1 to 1024 are sent from the phone to Qriro-server, and are not broadcasted to the application
#define TOGGLE_FILTER 1
// Messages with ID  from 1025 to 2048 are sent from the phone to the application and are broadcasted to the application
#define DOUBLE_TAP 1024+1
// Messages with ID bigger than 2049 are sent from Qriro-server to the application
#define START_THREAD 2048+1
#define CALIBRATION_END 2048+2

#endif /* QRIRO_SERVER_H_*/
