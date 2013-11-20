import processing.opengl.*;
import processing.net.*; 

float alpha = 0;
float beta = 0;
float gamma = 0;

float x_pos = 0;
float y_pos = 0;
float z_pos = 0;


final float positionChangeConstant = 10;
final float angleChangeConstant = 0.05;

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
  background(255);
  stroke(0);
  //noFill();
  fill(123);
  translate(width/2+x_pos,height/2+y_pos);
  rotateX(alpha);
  rotateY(beta);
  rotateZ(gamma);
  box(100+z_pos);
}

void keyPressed(){
  if(key== CODED){
    if(keyCode==RIGHT){
      x_pos += positionChangeConstant;
    }
    if(keyCode == LEFT){
      x_pos -= positionChangeConstant;
    }
    if(keyCode == UP){
      y_pos -= positionChangeConstant;
    }
    if(keyCode == DOWN){
      y_pos += positionChangeConstant;
    }
    
  }
  if((key == 'z')||(key=='Z')){
    z_pos+= positionChangeConstant;
  }
  if((key == 'x')||(key=='X')){
    z_pos-= positionChangeConstant;
  }
  
  if((key == 'a')||(key=='A')){
    alpha+= angleChangeConstant;
  }
  if((key == 's')||(key=='S')){
    beta+= angleChangeConstant;
  }
  if((key == 'd')||(key=='D')){
    gamma+= angleChangeConstant;
  }
}
