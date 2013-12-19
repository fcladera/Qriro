package ar.com.fclad.processingEclipse;

import processing.core.PApplet;
import processing.core.PMatrix3D;

public class Cube {

	float alpha = 0;
	float beta = 0;
	float gamma = 0;
  
	final float minZ = 10;
	final float maxZ = 200;
  
	float x_pos = 0;
	float y_pos = 0;
	float z_pos = 100;
	private final static float positionChangeConstant = 10;
	private final static float angleChangeConstant = (float) (0.02*Math.PI);
	
	PApplet parent;
  
	CommunicationThread commThread;
	  
	  public Cube(PApplet p, CommunicationThread commThread) {
		  this.parent = p;
		  this.commThread = commThread;
		  
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
	  
	  public void display(){
	    parent.stroke(0);
	    //noFill();
	    parent.fill(123);
	    //translate(width/2+x_pos,height/2+y_pos);
	    //rotateX(alpha);
	    //rotateY(beta);
	    //rotateZ(gamma);
	    
	    /*
	   float[] angles = new float[3];
	   
	   angles = commThread.getRotationAngles();
	   parent.translate(parent.width/2, parent.height/2);
	   parent.rotateX(angles[0]);
	   parent.rotateY(angles[1]);
	   parent.rotateZ(angles[2]);
	   */
	   
	   PMatrix3D rotTransMatr = commThread.getRotationTranslationMatrix();
	   parent.setMatrix(rotTransMatr);
	   //scale(1,-1);
	   // usar scale en vez de el tamaño
	   parent.box(100);

	  }
}
