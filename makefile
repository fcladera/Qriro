##############################################################################

ALL=$(shell for i in *.c ; do basename $$i .c ; done)

all : ${ALL}

CC=gcc
CFLAGS=-std=c99 -U__STRICT_ANSI__ \
       -W -Wall -pedantic -O3 \
       -D_REENTRANT
LDFLAGS=-lpthread -lm

.SUFFIXES:

% : %.c
	${CC} -o $@.bin $< ${CFLAGS} ${LDFLAGS}
	@echo

clean :
	rm -f ${ALL}.bin *.o a.out core

##############################################################################
