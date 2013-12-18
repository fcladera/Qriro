/*
 * simple_tcp_server.h
 *
 *  Created on: Nov 10, 2013
 *      Author: fclad
 */

#ifndef SIMPLE_TCP_SERVER_H_
#define SIMPLE_TCP_SERVER_H_


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

void processingThread(int dialogSocket, FILE *logfile);
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

typedef struct broadcastMessage{
	volatile bool mesageAvailable;
	int message;

} BroadcastMessage;

#endif /* SIMPLE_TCP_SERVER_H_ */
