#include <stdio.h>

typedef FILE* GnuPlotPipe;

/* Create a GnuPlotPipe and connect to GnuPlot
 * return value: 0: success, -1: failure
 */
int createGnuPlotPipe(GnuPlotPipe* gnuPlotPipe);

/* Send measurements x, y and z to a given gnuPlotPipe*/
int writeToGnuPlotPipe(GnuPlotPipe gnuPlotPipe, double x, double y, double z);

/* Close a gnuPlotPîpe
 * return value: 0: success, -1: failure
 */
int closeGnuPlotPipe(GnuPlotPipe gnuPlotPipe);
