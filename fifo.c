/*
 * fifo.c
 *
 *  Created on: Nov 6, 2013
 *      Author: fclad
 */

#include <stdio.h>
#include <stdlib.h>

void loadfifoPointer(double value, double *fifo, unsigned int lenlista){

	static unsigned int pointer=0;
	//Copy value on top of the fifo
	if(pointer==lenlista) pointer=0;
	*(fifo+pointer)=value;
	pointer++;

}


void loadfifoMooving(double value, double * fifo, unsigned int lenlista){
	unsigned int i;
	for(i=lenlista-1;i>0;i--){
		fifo[i] = fifo[i-1];
	}
	fifo[0] = value;
}

void clearfifo(double *fifo, unsigned int lenlista){
	unsigned int counter = 0;
    while(counter<lenlista) {
        counter++;
        *(fifo++) = 0;
    }
}

void printfifo(double *fifo, unsigned int lenlista){
	unsigned int i;
	for(i=0;i<lenlista;i++){
		printf("[%d]: %g\n",i,fifo[i]);
	}
}

double FIRfilter(double *fifo,
			double *coefficients,
			unsigned int taps){
	double filtered = 0;
	for(unsigned int i=0;i<taps;i++){
		filtered += coefficients[i]*fifo[i];
	}
	return filtered;
}

double sumfifo(double *fifo, unsigned int lenlista){
	unsigned int i,j;
	double result;

	if(lenlista%2!=0){
		printf("Error, the fifo size should be 2^n");
		exit(EXIT_FAILURE);
	}

	double * tempfifo = malloc(sizeof(double)*lenlista);

	//copy fifo into tempfifo
	for(i=0;i<lenlista;i++){
		tempfifo[i] = fifo[i];
	}

	//add elements of tempfifo
	for(j=lenlista/2;j>=1;j=j/2){
		for(i=0;i<j;i++){
			tempfifo[i] = tempfifo[2*i]+tempfifo[2*i+1];
		}
	}
	result = tempfifo[0];
	free(tempfifo);
	return result;
}
