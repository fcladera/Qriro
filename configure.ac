AC_INIT([dataProcessor], [0.1], [fcladera@fcladera.com])
AM_INIT_AUTOMAKE
AC_PROG_CC

# Checks for typedefs, structures, and compiler characteristics.
AC_TYPE_UINT32_T
AC_TYPE_UINT8_T

# Checks for header files.
AC_CHECK_HEADERS([arpa/inet.h fcntl.h inttypes.h netdb.h netinet/in.h stdlib.h string.h sys/socket.h sys/time.h unistd.h])

# Checks for libraries.
AC_CHECK_LIB([bluetooth], [sdp_connect])
AC_CHECK_LIB([gsl], [gsl_matrix_set])
AC_CHECK_LIB([gslcblas], [gsl_blas_dgemm])
AC_CHECK_LIB([m], [cos])
AC_CHECK_LIB([pthread], [pthread_create])

# Checks for library functions.
AC_FUNC_MALLOC
AC_CHECK_FUNCS([clock_gettime inet_ntoa select socket])

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
