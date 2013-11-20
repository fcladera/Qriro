import processing.opengl.*;
import processing.net.*; 


Client myClient;
int dataIn;

boolean connectionAvailable = false;

void setup() {
  // OPENGL or P3D mode requires the use of beginRaw() and endRaw() instead of beginRecord() and endRecord().
  size(400, 400, OPENGL); 
  smooth();
  if(connectionAvailable){
     myClient = new Client(this, "127.0.0.1", 5204);
  }
}

int i = 0;
void draw() {
  if(connectionAvailable){
    if (myClient.available() > 0) { 
      dataIn = myClient.read(); 
      background(dataIn); 
      //myClient.write("OK");
    }
  }
}

