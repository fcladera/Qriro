#ifndef BLUETOOTH_CLIENT_H
#define BLUETOOTH_CLIENT_H

#include <stdlib.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>
#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>

/* This function connects to a bluetooth server with the given
 * deviceAddress, creating a socket in bluetoothSocket.
 * Return value: 0 success, -1 failure */

int connectToBTDevice(char* deviceAddress, int* bluetoothSocket);

#endif /* BLUETOOTH_CLIENT_H */
