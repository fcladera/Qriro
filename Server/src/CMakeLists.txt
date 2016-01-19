cmake_minimum_required (VERSION 2.6)
project (Qriro-server)

set(CMAKE_C_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -Wall -g -O0")

# FIFOlib
include_directories ("${PROJECT_SOURCE_DIR}/FIFOlib")
add_subdirectory (FIFOlib)
add_subdirectory (TCPlib)

add_executable(Qriro-server Qriro-server.c)

# FIFOlib
target_link_libraries(Qriro-server FIFOlib)

# TCPlib
target_link_libraries(Qriro-server TCPlib)

# System libs
target_link_libraries(Qriro-server pthread m gsl gslcblas bluetooth)
