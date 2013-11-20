import processing.opengl.*;
import processing.net.*; 

Client myClient;
Cube cube;
String dataIn;

boolean connectionAvailable = true;

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
  background(255);
  cube.drawCube();
}

void clientEvent(Client myClient){
  dataIn = myClient.readStringUntil('\n'); 
  if(dataIn!=null){
    println(dataIn);
    String[] list = splitTokens(dataIn,":;");
    //print(char(list[0]));
    if(list[0].equals("S")){
       println("Processing position");
      int i=0;
      for(String str : list){
        print(str+"\n");
        switch(i){
          case 2:
           cube.setX(float(str));
           break;
          case 3:
            cube.setY(float(str));
            break;
          case 4:
            cube.setZ(float(str));
        }
        i++; 
      }       
    }
    if(list[0].equals("G")){
      println("Processing angle");
      int i=0;
      for(String str : list){
        print(str+"\n");
        switch(i){
          case 2:
           cube.setAlpha(float(str));
           break;
          case 3:
            cube.setBeta(float(str));
            break;
          case 4:
            cube.setGamma(float(str));
        }
        i++; 
      }       
    }
  }
  //myClient.write("OK");
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
  private final static float positionChangeConstant = 10;
  private final static float angleChangeConstant = 0.02*PI;
  

  void setX(float value){
   x_pos =  value;
  }
  
  void setY(float value){
   y_pos =  value;
  }
  
  void setZ(float value){
   z_pos = value; 
   if(z_pos > maxZ) z_pos = maxZ;
   else if(z_pos < minZ) z_pos = minZ;
  }
  
  void setAlpha(float value){
    alpha = value;
  }
  
  void setBeta(float value){
    beta = value;
  }
  
  void setGamma(float value){
   gamma = value; 
  }
  
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
    // usar scale en vez de el tamaño
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
