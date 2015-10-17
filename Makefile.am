AUTOMAKE_OPTIONS = foreign

AM_CFLAGS = -W -Wall -pedantic -O3 \
						-U__STRICT_ANSI_
AM_LDFLAGS=-lpthread -lm -lgsl -lgslcblas -lbluetooth

bin_PROGRAMS = dataProcessor

dataProcessor_SOURCES = dataProcessor.c fifo.c

#CFLAGS= -U__STRICT_ANSI__ -W -Wall -O3 -D_REENTRANT
