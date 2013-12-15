package ar.com.fclad.processingEclipse;

import java.awt.Color;

import processing.core.PApplet;
import processing.core.PMatrix3D;

public class CatchSphere {
	PApplet parent;
	private float pRadius;
	private int color;
	private float 	x=400, 
					y=300, 
					z=0; // the position of the shape as a whole
	CommunicationThread commThread;
	
	
	
	public CatchSphere(PApplet p, CommunicationThread commThread, float pRadius) {
		parent = p;
		this.commThread = commThread;
	    this.pRadius = pRadius; // the radius of the shape
	    this.color = Color.RED.getRGB();
	    //createSphere();
	}
	
	public float[] getPositionAndRadius(){
		float[] position = new float[4];
		position[0] = x;
		position[1] = y;
		position[2] = z;
		position[3] = pRadius;
		return position;
	}
	
	public void setPosition(float x, float y, float z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/*
	void createSphere() {
		sphere(pRadius);
	  }
	  */
 
	  void display() {
	    parent.pushMatrix();
	    parent.translate(x, y, 0);
	    PMatrix3D originalMatrix=null;
	    //parent.ro
	    //parent.getMatrix(originalMatrix);
	    //parent.setMatrix(commThread.getRotationWithoutTranslationMatrix(originalMatrix));
	    //parent.rotateX((float)(Math.atan2(parent.mouseY-parent.height/2,parent.height/2)));
	    //parent.rotateY((float)(-Math.atan2(parent.mouseX-parent.width/2,parent.width/2)));
	    parent.translate(0, 0,z);
	    //parent.rotateY((float)(x + parent.frameCount*0.01)); 
	    //parent.rotateX((float)(y + parent.frameCount*0.02));
	    parent.fill(color,(float)128);
	    parent.sphere(pRadius);
	    parent.popMatrix(); // use push/popMatrix so each Shape's translation/rotation does not affect other drawings
	    
	    
		 /*parent.pushMatrix();
	    //parent.translate(parent.width/2,parent.height/2,750);
	    //PApplet.println((parent.height/2.0)/Math.tan(Math.PI*30.0 / 180.0)+"\n");
	    
	    //float boxSize = 750+z-pRadius;
	    //float zCam = 750;
	    //parent.translate(0, 0, 750/2);
	    //parent.rotateY((float)Math.atan2(x-parent.width/2, parent.width/2));
	    //parent.rotateX((float)-Math.atan2(y-parent.height/2, parent.height/2));
	    //parent.box(10, 10, boxSize);
	    
	    parent.line(parent.width/2, parent.height/2, zCam, x,y,z-pRadius);
	    parent.popMatrix();
	    */
	    
	  }

	  // randomly sets the xy position of the shape as a whole and the colors of the shape  

}
