##############################################################################

CC=gcc
CFLAGS=-std=c99 -U__STRICT_ANSI__ \
       -W -Wall -pedantic -O3 \
       -D_REENTRANT
LDFLAGS=-lpthread -lm -lgsl -lgslcblas
SOURCES= simple_tcp_server.c fifo.c
EXECUTABLE = simple_tcp_server.bin

all : clean ${EXECUTABLE}

.SUFFIXES:

${EXECUTABLE}: 
	${CC} -o $@ ${SOURCES} ${CFLAGS} ${LDFLAGS}
	@echo

clean :
	rm -f ${EXECUTABLE} *.o a.out core

##############################################################################
