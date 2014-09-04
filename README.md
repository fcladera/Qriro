Qriro-android
============

Qriro is an application that can be used to translate the motion sensor information from an android device into a rotation/translation matrix (transformation matrix).
This transformation matrix can be used in other applications, such as virtual reality and games.

## How does Qriro work?
Qriro is composed of tree applications:
- [The Android application](https://github.com/fcladera/Qriro-android): it sends bulk data from the giroscope, the screen and commands to the server, via Bluetooth or TCP (on WiFi).
- [The Server](https://github.com/fcladera/Qriro-android) (dataProcessor.bin): It receives bulk data from an android device and performs the necessary calculations to obtain the transformation matrix. It can also broadcast commands to the upper level application.
- [The virtual reality application](https://github.com/fcladera/Qriro-RV-sample): It uses the transformation matrix in order to represent objects in a computer simulated environment


## How does the Android application work?
First, a connection needs to be established with the C server. 
- If the connection is established using TCP, dataProcessor.bin acts as a server and the Android application acts as a client (see TCPclientService.java).
- If the connection is established using Bluetooth, dataProcessor.bin acts as a client and the Android application acts as a server (see BluetoothServerService.java).

Once the connection with the C server is established, an Android service is active. Messages can be broadcasted to the C server using this service. The application switches to DrawActivity activity, where a message is sent each time a sensor changes or the screen is touched.

Actually, only the Gyro and the screen can send information to the C server. Commands can also be sent.


## How to use Qriro?

You need to:
- Compile this application (make)
- Install the android application in the device.

### Using Bluetooth (recommended)
1. Start the android application, and select "Connect Bluetooth". The button will turn red. 
2. Exec the dataProcessor.bin application, with the following parameters
`./dataProcessor.bin BT bluetoothAddress portApplication`
BluetoothAddress should have the following pattern (AB:CD:EF:01:23:45)
port indicates the TCP port where the virtual reality application will ask for data.
3. Select "Start Drawing" to send data to the Server.

### Using TCP (on WiFi)
1. Start dataProcessor.bin with the following parameters
`./dataProcessor.bin TCP portDevice portApplication`
2. Start the android application. Modify TCP parameters to suit your connection configuration (Server IP and portDevice)
3. Select "Start Drawing" to send data to the Server.

### Get rotation matrix
1. Connect to the C server using TCP
2. Send the TCP request `GETMAT`

## TODO
- The angular position is obtained integrating Gyro values. This method suffers from drifting over time. Better solutions can use the accelerometer too.
- Organize the code.
- Some nice videos are needed as POC.
- Applications (Android and Server) have to be tested.

If you need help or want to contribute, don't hesitate fill a bug report. 
You can also write to fclad at mecatronicauncu.org
