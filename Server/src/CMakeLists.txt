cmake_minimum_required (VERSION 2.6)
project (Qriro-server)

# FIFOlib
include_directories ("${PROJECT_SOURCE_DIR}/FIFOlib")
add_subdirectory (FIFOlib)

add_executable(Qriro-server Qriro-server.c)

# FIFOlib
target_link_libraries(Qriro-server FIFOlib)

# System libs
target_link_libraries(Qriro-server pthread m gsl gslcblas bluetooth)
