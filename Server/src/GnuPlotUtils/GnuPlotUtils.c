#include "GnuPlotUtils.h"

int createGnuPlotPipe(GnuPlotPipe* gnuPlotPipe){
  *gnuPlotPipe = NULL;

  char *command = "feedgnuplot --lines --stream 0.1 --xlen 1000 --ylabel 'value' --xlabel sample > /dev/null";

  *gnuPlotPipe = popen(command,"w");

  if ((*gnuPlotPipe == NULL)){
    perror("Error creating GnuPlot Pipe");
    pclose(*gnuPlotPipe);
    return -1;
  }

  return 0;
}

int writeToGnuPlotPipe(GnuPlotPipe gnuPlotPipe, double x, double y, double z){
  fprintf(gnuPlotPipe, "%lf\t%lf\t%lf\n", x, y, z);
  fflush(gnuPlotPipe);
  return 0;
}

int closeGnuPlotPipe(GnuPlotPipe gnuPlotPipe){
  if (pclose(gnuPlotPipe) < 0){
    perror("Error closing GnuPlot pipe");
    return -1;
  }
  return 0;
}
