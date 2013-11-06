/*
 * fifo.c
 *
 *  Created on: Nov 6, 2013
 *      Author: fclad
 */

#include <stdio.h>

void loadfifoPointer(double value, double *fifo, unsigned int lenlista){

	static unsigned int pointer=0;
	//Copy value on top of the fifo
	if(pointer==lenlista) pointer=0;
	*(fifo+pointer)=value;
	pointer++;

}


void loadfifoMooving(double value, double * fifo, unsigned int lenlista){
	unsigned int i;
	for(i=lenlista;i>0;i--){
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
