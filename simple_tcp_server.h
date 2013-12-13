/*
 * simple_tcp_server.h
 *
 *  Created on: Nov 10, 2013
 *      Author: fclad
 */

#ifndef SIMPLE_TCP_SERVER_H_
#define SIMPLE_TCP_SERVER_H_

//#include <gsl/gsl_matrix.h>

double toDegrees(double);
void printMatrix(gsl_matrix * A);
void howToUse();

void processingThread(int dialogSocket, FILE *logfile);
void *applicationThread(void * arg);


typedef enum x{TCP,BLUETOOTH} mode;

#endif /* SIMPLE_TCP_SERVER_H_ */
