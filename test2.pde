import processing.opengl.*;
import processing.net.*; 


Client myClient;
int dataIn;

void setup() {
  // OPENGL or P3D mode requires the use of beginRaw() and endRaw() instead of beginRecord() and endRecord().
  size(400, 400, OPENGL); 
  smooth();
   myClient = new Client(this, "127.0.0.1", 5204); 
}

void draw() {
  if (myClient.available() > 0) { 
    dataIn = myClient.read(); 
  }
  //myClient.write("OK");
  background(dataIn); 
}

