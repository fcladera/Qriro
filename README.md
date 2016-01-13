Qriro
============

Qriro is an application that can be used to translate the motion sensor information from an android device into a rotation/translation matrix (transformation matrix).
This transformation matrix can be used in other applications, such as virtual reality and games.

## How does Qriro work?
Qriro is composed of two applications:
- The mobile application: as its name suggests, it runs on an Android device. It sends bulk data from the giroscope, the screen and commands to the server, via Bluetooth or TCP (on WiFi).
- The Server: it runs on a (Linux) computer. It receives bulk data from a mobile device and performs the necessary calculations to obtain the transformation matrix. It can also broadcast commands to the upper level application.

In addition, a virtual reality demo is provided. This application uses the transformation matrix in order to represent objects in a computer simulated environment

## How does the Android application work?
First, a connection needs to be established with the server.
- If the connection is established using TCP, dataProcessor.bin acts as a server and the Android application acts as a client (see TCPclientService.java).
- If the connection is established using Bluetooth, dataProcessor.bin acts as a client and the Android application acts as a server (see BluetoothServerService.java).

Once the connection with the server is established, an Android service is active. Messages can be broadcasted to the server using this service. The application switches to DrawActivity activity, where a message is sent each time a sensor changes or the screen is touched.

Actually, only the Gyro and the screen can send information to the server. Commands can also be sent.

## How does the Server work?
The following frames are received from the android device:

Origin : ID : dT : val(0) : val(1) : val(2);

 - **Origin:**
 is the sensor which sends the message. Currently, three origins are supported: Gyro, Screen and Command.
 Gyro and screen are used to calculate the transformation matrix.
 Commands may be broadcast to the virtual reality application or used to notify the server.

 - **ID**:
 A simple tag for the current message.

 - **dT**:
 The elapsed time from the last measurement of the sensor.

 - **val(1-3)**:
 Values of the sensors. E.g. angular velocity in three axis for the gyro.
 Commands only use val(1), where ComID is stored (command identifier).

After receiving a frame, the Server processes the transformation matrix.
The result is sent to the virtual reality application using a TCP socket, after the GETMAT request.

MAT:m00:m01:m02:m03:m10:m11:m12:m13:m20:m21:m22:m23:
m30:m31:m32:m33:m40:m41:m42:m43;

Where mij are the elements of the transformation matrix.
If there is a command, available, the following data is sent:

COM:ComID;

MAT:m00:m01:m02:m03:m10:m11:m12:m13:m20:m21:m22:m23:
m30:m31:m32:m33:m40:m41:m42:m43;

## How does the demo work?
In the demo application, the transformation matrix is used in two virtual reality examples.

First, a socket is established between the server and this application (see CommunicationThread.java). Each time the screen is drawn, a new translation matrix is requested to the server using `commThread.askNewRotationMatrix()`. This matrix can be used with `commThread.getRotationTranslationMatrix()`.

As specified, two examples are given:
- A Cube. whose position is modified using the transformation matrix (see Cube.java)
- A Game, where diamonds have to be caught using a ring controlled by the phone. This game is inspired in the [processing tutorial](https://github.com/AmnonOwed/P5_CanTut_GeometryTexturesShaders/tree/master/Custom3DGeometry) by [Amnon Owed](https://github.com/AmnonOwed)

## Requirements

### Server
 - A working C compiler
 - bluez-libs
 - GNU Scientific Library (GSL)

### Android application
 - An android phone with gyroscope and Android > 4.0

### For the demo application
- Processing (tested with 2.2.1)
- JDK (tested with Openjdk 7)

## Building

### Server
Go to the Server folder and call make
```
cd Server
make
```
### Android
TODO

### Demo application
TODO

## How to use Qriro?

You need to:
- Compile the server
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
1. Connect to the server using TCP
2. Send the TCP request `GETMAT`

## TODO
- The angular position is obtained integrating Gyro values. This method suffers from drifting over time. Better solutions can use the accelerometer too.
- Organize the code.
- Some nice videos are needed as POC.
- Applications (Android and Server) have to be tested.

If you need help or want to contribute, don't hesitate fill a bug report.
You can also write to fclad at mecatronicauncu.org
