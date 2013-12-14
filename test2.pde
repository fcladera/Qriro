import processing.opengl.*;
import processing.net.*; 

Client myClient;
Cube cube;
String dataIn;

boolean connectionAvailable = true;

void setup() {
  // OPENGL or P3D mode requires the use of beginRaw() and endRaw() instead of beginRecord() and endRecord().
  size(800, 800, OPENGL); 
  smooth();
  cube = new Cube();
  if(connectionAvailable){
     myClient = new Client(this, "127.0.0.1", 7778);
  }
}


void draw() {
  background(255);
  cube.drawCube();
}

void clientEvent(Client myClient){
  dataIn = myClient.readStringUntil('\n'); 
  if(dataIn!=null){
    //println(dataIn);
    String[] list = splitTokens(dataIn,":;");
    //print(char(list[0]));
    if(list[0].equals("MAT")){
       //println("Processing matrix");
      int i=0;
      float[][] rotMatrix = new float[4][4];
      for(String str : list){
        //print(i+" : "+str+"\n");
        if(i!=0){
          int p=i-1;
          rotMatrix[p/4][p%4] = float(str);
          //println(p/4+" "+p%4);
        }
        i++;
        if(i>16) break;
         
      }
      cube.setRotationMatrix(rotMatrix);
    }
  }
  //myClient.write("OK");
}

class Cube{
  
  float[][] rotationMatrix ={{1f,0f,0f,0f},
                             {0f,1f,0f,0f},
                             {0f,0f,1f,-200f},
                             {0f,0f,0f,1f}};
                             
                             
  
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
  
  
  void setRotationMatrix(  float[][] newRotMat){
    rotationMatrix = newRotMat;
                             
  }
                          

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
    //translate(width/2+x_pos,height/2+y_pos);
    //rotateX(alpha);
    //rotateY(beta);
    //rotateZ(gamma);
    PMatrix3D matrix = new PMatrix3D(rotationMatrix[0][0],rotationMatrix[0][1],rotationMatrix[0][2],rotationMatrix[0][3],
                                     rotationMatrix[1][0],rotationMatrix[1][1],rotationMatrix[1][2],rotationMatrix[1][3],
                                     rotationMatrix[2][0],rotationMatrix[2][1],rotationMatrix[2][2],rotationMatrix[2][3]-400,
                                     rotationMatrix[3][0],rotationMatrix[3][1],rotationMatrix[3][2],rotationMatrix[3][3]) ;
    setMatrix(matrix);
    //scale(1,-1);
    // usar scale en vez de el tamaño
    box(z_pos);
    myClient.write("VALS");
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
