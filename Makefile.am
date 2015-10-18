AUTOMAKE_OPTIONS = foreign

AM_CFLAGS = -W -Wall -O3
AM_LDFLAGS= -lpthread -lgsl -lgslcblas -lm -lbluetooth

bin_PROGRAMS = dataProcessor

dataProcessor_SOURCES = dataProcessor.c fifo.c

#CFLAGS= -U__STRICT_ANSI__ -W -Wall -O3 -D_REENTRANT
