/*
 * fifo.h
 *
 *  Created on: Nov 6, 2013
 *      Author: fclad
 */

#ifndef FIFO_H_
#define FIFO_H_


void loadfifoPointer(double, double *, unsigned int);

void loadfifoMooving(double, double *, unsigned int);

void clearfifo(double *, unsigned int);

void printfifo(double *, unsigned int );

double sumfifo(double *, unsigned int );

double FIRfilter(	double *,
					double *,
					unsigned int);

double firCoefs[32] = {
		-0.00129092,
		-0.00163098,
		-0.00218104,
		-0.00276853,
		-0.00299268,
		-0.00225752,
		0.000133458,
		0.00483449,
		0.0122938,
		0.0226042,
		0.0354045,
		0.0498593,
		0.0647295,
		0.0785294,
		0.0897435,
		0.0970648,
		0.0996094,
		0.0970648,
		0.0897435,
		0.0785294,
		0.0647295,
		0.0498593,
		0.0354045,
		0.0226042,
		0.0122938,
		0.00483449,
		0.000133458,
		-0.00225752,
		-0.00299268,
		-0.00276853,
		-0.00218104,
		-0.00163098
};

#endif /* FIFO_H_ */
