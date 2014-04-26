Qriro-server
============
NOTE: Currently, only the server is available. I will upload the other applications soon! 

Qriro is an application that can be used to translate the motion sensor and screen lectures from an android device into a rotation/translation matrix (transformation matrix).
This transformation matrix can be obtained using a TCP socket in other applications (such as virtual reality applications).

## How does Qriro work?
Qriro is composed of tree applications:
- The Android application: it only sends data from the giroscope, the screen and commands to the server application, via Bluetooth or TCP (on WiFi links).
- The Server (dataProcessor.bin): It receives bulk data from an android device  and performs the necessary calculations to obtain the rotation/translation matrix. It can broadcast commands to the upper level application.
- The virtual reality application: It uses the transformation matrix in order to represent objects in a computer simulated environment


## How does the Server work?
The following frames are received from the android device:

Origin : ID : dT : val(0) : val(1) : val(2);

 - **Origin:**
 is the sensor which sends the message. Currently, tree origins are supported: Gyro, Screen and Command.
 Gyro and screen are used to calculate the transformation matrix.
 Commands may be broadcast to the virtual reality application or used to notify the server.
 
 - **ID**:
 A simple tag for the current message.
 
 - **dT**:
 The time passed from the last measurement of the sensor. 
 
 - **val(1-3):
 Values from the sensors. E.g. rotations for the gyro.
 Commands only use val(1), where ComID is stored (command identifier).
 
After receiving a frame, the Server processes the transformation matrix. 
The matrix is send to the virtual reality application using a TCP socket, after the GETMAT request.
The following frame is sent:

MAT:m00:m01:m02:m03:m10:m11:m12:m13:m20:m21:m22:m23:
m30:m31:m32:m33:m40:m41:m42:m43;

Where mij are the elements of the transformation matrix.
If there is a command, available, the following data is sent:

COM:ComID;
MAT:m00:m01:m02:m03:m10:m11:m12:m13:m20:m21:m22:m23:
m30:m31:m32:m33:m40:m41:m42:m43;

## How to use Qriro?

You need to:
- Compile this application (make)
- Install the android application in the device.

### Using Bluetooth (recommended)
1. Start the android application, and select "Connect Bluetooth". The button will turn red. 
2. Exec the dataProcessor.bin application, with the following parameters

./dataProcessor.bin BT bluetoothAddress portApplication

BluetoothAddress should have the following pattern (AB:CD:EF:01:23:45)
port indicates the TCP port where the virtual reality application will ask for data.
3. Select "Start Drawing" to send data to the Server.

### Using TCP (on WiFi)
1. Start dataProcessor.bin with the following parameters
./dataProcessor.bin TCP portDevice portApplication
2. Start the android application. Modify TCP parameters to suit your connection configuration (Server IP and portDevice)
3. Select "Start Drawing" to send data to the Server.

## TODO (in the whole project)
- The angular position is obtained integrating the Gyro values. This method suffers from drifting over time. Better solutions can also use the accelerometer.
- Some nice videos are needed as POC.
- Applications (Android and Server) have to be tested.

If you need help or want to contribute, don't hesitate to notify 




