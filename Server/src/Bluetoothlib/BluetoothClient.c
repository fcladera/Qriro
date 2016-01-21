#include "BluetoothClient.h"

int connectToBTDevice(char* deviceAddress, int* bluetoothSocket){
  // Get port number of the application with sdp
  uint8_t svc_uuid_int[] = {0x2f, 0xa7, 0xbe, 0xb1,
    0x6a, 0xcf,
    0x47, 0x03,
    0x8b, 0x7e,
    0xdc, 0xbc, 0x14, 0xf0, 0x7b, 0x89};
  int status;
  bdaddr_t target;
  uuid_t svc_uuid;
  sdp_list_t *response_list, *search_list, *attrid_list;
  sdp_session_t *session = 0;
  uint32_t range = 0x0000ffff;
  uint8_t port = 0;
  str2ba(deviceAddress, &target);

  // connect to the SDP server running on the remote machine (Android
  // device)
  session = sdp_connect( BDADDR_ANY, &target, 0 );

  sdp_uuid128_create( &svc_uuid, &svc_uuid_int );
  search_list = sdp_list_append( 0, &svc_uuid );
  attrid_list = sdp_list_append( 0, &range );

  // get a list of service records that have the UUID svc_uuid_int
  response_list = NULL;
  status = sdp_service_search_attr_req( session, search_list,
      SDP_ATTR_REQ_RANGE, attrid_list, &response_list);

  if(status == 0) {
    sdp_list_t *proto_list = NULL;
    sdp_list_t *r = response_list;

    // go through each of the service records
    for (; r; r = r->next ) {
      sdp_record_t *rec = (sdp_record_t*) r->data;

      // get a list of the protocol sequences
      if(sdp_get_access_protos(rec, &proto_list) == 0) {

        // get the RFCOMM port number
        port = sdp_get_proto_port(proto_list, RFCOMM_UUID);
        sdp_list_free(proto_list, 0);
      }
      sdp_record_free(rec);
    }
  }
  sdp_list_free(response_list, 0);
  sdp_list_free(search_list, 0);
  sdp_list_free(attrid_list, 0);
  sdp_close(session);

  if(port != 0){
    printf("Service running on RFCOMM port %d\n", port);
  }
  else{
    fprintf(stderr,"ERROR: RFCOMM application not found!\n");
    return -1;
  }

  // Create bluetooth socket
  struct sockaddr_rc addr = {0};

  // allocate a socket
  *bluetoothSocket = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
  if(*bluetoothSocket == -1){
    perror("ERROR connecting to Bluetooth device - socket");
    return -1;
  }

  // set the connection parameters
  addr.rc_family = AF_BLUETOOTH;
  addr.rc_channel = port;
  str2ba(deviceAddress, &addr.rc_bdaddr);

  // connect to server
  if(connect(*bluetoothSocket, (struct sockaddr *)&addr, sizeof(addr)) == -1){
    perror("ERROR connecting to Bluetooth device - connect");
    return -1;
  }

  return 0;
}
