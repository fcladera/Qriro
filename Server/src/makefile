##############################################################################

CC=gcc
#CFLAGS=-std=c99 -U__STRICT_ANSI__ \
       -W -Wall -pedantic -O3 \
       -D_REENTRANT

CFLAGS= -U__STRICT_ANSI__ -W -Wall -O3 -D_REENTRANT
LDFLAGS=-lpthread -lm -lgsl -lgslcblas -lbluetooth
SOURCES= Qriro-server.c fifo.c
EXECUTABLE = Qriro-server.bin

all : clean ${EXECUTABLE}

.SUFFIXES:

${EXECUTABLE}:
	${CC} -o $@ ${SOURCES} ${CFLAGS} ${LDFLAGS}
	@echo

clean :
	rm -f ${EXECUTABLE} *.o a.out core

##############################################################################
