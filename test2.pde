import processing.opengl.*;
import processing.net.*; 

Client myClient;
Cube cube;
int dataIn;

boolean connectionAvailable = false;

void setup() {
  // OPENGL or P3D mode requires the use of beginRaw() and endRaw() instead of beginRecord() and endRecord().
  size(400, 400, OPENGL); 
  smooth();
  cube = new Cube();
  if(connectionAvailable){
     myClient = new Client(this, "127.0.0.1", 5204);
  }
}


void draw() {
  if(connectionAvailable){
    if (myClient.available() > 0) { 
      dataIn = myClient.read(); 
      background(dataIn); 
      //myClient.write("OK");
    }
  }
  background(255);
  cube.drawCube();
}

class Cube{
  float alpha = 0;
  float beta = 0;
  float gamma = 0;
  
  final float minZ = 10;
  final float maxZ = 200;
  
  float x_pos = 0;
  float y_pos = 0;
  float z_pos = 100;
  final float positionChangeConstant = 10;
  final float angleChangeConstant = 0.05;
  

  
  void increaseX(){
    x_pos += positionChangeConstant;
    //if(x_pos < 0) x_pos = 0;
  }
  void increaseY(){
    y_pos += positionChangeConstant;
    //if(y_pos < 0) y_pos = 0;
  }
  void increaseZ(){
    z_pos += positionChangeConstant;
    if(z_pos > maxZ) z_pos = maxZ;
  }
  void decreaseX(){
    x_pos -= positionChangeConstant;
  }
  void decreaseY(){
    y_pos -= positionChangeConstant;
  }
  void decreaseZ(){
    z_pos -= positionChangeConstant;
    if(z_pos < minZ) z_pos = minZ;
  }
  
  void increaseAlpha(){
    alpha+= angleChangeConstant;
  }
   void increaseBeta(){
    beta+= angleChangeConstant;
  }
   void increaseGamma(){
    gamma+= angleChangeConstant;
  }
  
  void drawCube(){
    stroke(0);
    //noFill();
    fill(123);
    translate(width/2+x_pos,height/2+y_pos);
    rotateX(alpha);
    rotateY(beta);
    rotateZ(gamma);
    box(z_pos);
  }
}

void keyPressed(){
  if(key== CODED){
    if(keyCode==RIGHT){
      cube.increaseX();
    }
    if(keyCode == LEFT){
       cube.decreaseX();
    }
    if(keyCode == UP){
     cube.increaseY();
    }
    if(keyCode == DOWN){
      cube.decreaseY();
    }
    
  }
  if((key == 'z')||(key=='Z')){
    cube.increaseZ();
  }
  if((key == 'x')||(key=='X')){
    cube.decreaseZ();
  }
  
  if((key == 'a')||(key=='A')){
    cube.increaseAlpha();
  }
  if((key == 's')||(key=='S')){
    cube.increaseBeta();
  }
  if((key == 'd')||(key=='D')){
    cube.increaseGamma();
  }
}
