package ar.com.fclad.processingEclipse;

import java.awt.Color;
import processing.core.*;

public class Diamond {
	PApplet parent;

	//=======================================================================
	// Diamond types
	private static final int DIAMOND_BAD = 0;
	private static final int DIAMOND_GOOD = 1;
	private static final int DIAMOND_VERY_GOOD = 2;
	private int diamondType;
	
	//=======================================================================
	// Geometric properties
	private static final int nbPoints = 14;
	private PVector[] v = new PVector[nbPoints];
	private int[] color = new int[nbPoints]; // the colors each coordinate
	private float pHeight, pRadius; // height and radius of the shape
	private float speed, transparency; // fly speed and the color transparency
	private float 	x=100, 
					y=100, 
					z=-5000; // initial diamond position
	
	private ScoreCounter scoreCounter; // to add and remove points
	private boolean isDiamondCaught;
	private int captureFrame;
	

	public Diamond(PApplet p, ScoreCounter scoreCounter, float pRadius) {
		this.parent = p;
		this.scoreCounter = scoreCounter;
		this.pHeight = pRadius; // the diamond fits in a sphere
	    this.pRadius = pRadius; // the radius of the shape
	    this.speed = parent.random(10, 20); // random speed
	    createDyamond(); // create cordinates of the diamond
	    reset(); // set random x and y position and random diamond type
	}
	
	public float getRadius(){
		return pRadius;
	}
	
	public int getDiamondType(){
		return diamondType;
	}
	
	public void diamondCaught(){
		// The score depends on the diamond type
		if(!isDiamondCaught){
			switch (diamondType) {
			case DIAMOND_BAD:
				scoreCounter.decreaseScore(5);
				break;
			case DIAMOND_GOOD:
				scoreCounter.increaseScore(5);
				break;
			case DIAMOND_VERY_GOOD:
				scoreCounter.increaseScore(25);
				break;
			default:
				break;
			}
			isDiamondCaught = true;
			captureFrame = parent.frameCount;
		}
	}
	
	public void diamondEscaped(){
		scoreCounter.decreaseScore(10);
		reset();
	}
	
	void createDyamond() {
	    v[0] = new PVector(0, -pHeight, 0); // top
	    for(int i=1;i<nbPoints-1;i++){
	    	v[i] = new PVector((float)(pRadius*Math.cos(2*Math.PI/(nbPoints-2)*(i-1))), 0, (float)(pRadius*Math.sin(2*Math.PI/(nbPoints-2)*(i-1))));
	    }
	    v[nbPoints-1] = new PVector(0, pHeight, 0); // bottom 
	  }
	
	  void update() {
	    z += speed; 
	    if (z > 200){ // if beyond the camera, reset() and start again 
	    	diamondEscaped();
	    } 
	    transparency = z<-2500?PApplet.map(z, -5000, -2500, 0, 255):255; // the transparency is determined by the z position
	  }


	  void display() {
		  // Disolve caught diamonds
		  if(isDiamondCaught){
			int frameCount = parent.frameCount-captureFrame;
			if(frameCount>=31){
				reset();
			}
			else{
				transparency = 255-8*frameCount;
			}
			
		}
	    parent.pushMatrix(); 
	    PMatrix3D positionMatrix = new PMatrix3D(1,0,0,x,0,1,0,y,0,0,1,z,0,0,0,1);
	    positionMatrix.rotateY((float)(x + parent.frameCount*0.01)); // Random rotation of the diamond
	    positionMatrix.rotateX((float)(y + parent.frameCount*0.02)); 
	    parent.setMatrix(positionMatrix);
	    
	    // draw one side of the diamond
	    parent.beginShape(PConstants.TRIANGLE_FAN);
	    for (int i=0; i<nbPoints-1; i++) {
	      parent.fill(color[i], transparency);
	      parent.vertex(v[i].x, v[i].y, v[i].z);
	    }
	    parent.fill(color[1], transparency);
	    parent.vertex(v[1].x, v[1].y, v[1].z);
	    parent.endShape();
	    
	    // draw the other side of the diamond
	    parent.beginShape(PConstants.TRIANGLE_FAN);
	    for (int i=nbPoints-1; i>0; i--) {
		      parent.fill(color[i], transparency); 
		      parent.vertex(v[i].x, v[i].y, v[i].z);
		}
	    parent.fill(color[nbPoints-2], transparency);
	    parent.vertex(v[nbPoints-2].x, v[nbPoints-2].y, v[nbPoints-2].z);
	    parent.endShape(); 
	    parent.popMatrix(); 
	  }

	  
	  void reset() {
		  // Set z far far away
		  z = -5000;
		  // Set random x-y position
		  x = parent.random((float)(-parent.width/2*.7), (float) (parent.width/2*.7)); 
		  y = parent.random((float)(-parent.height/2*.7), (float)(parent.height/2*.7));
		  
		  // Set diamond type and color
		  int randomValue = (int) parent.random(0,10);
		  
		  switch (randomValue) {
			case 1:
			case 0:
				// Diamond is DIAMOND_BAD which are ugly red
				diamondType = DIAMOND_BAD;
				for(int i=0;i<nbPoints;i++){
					color[i] = Color.RED.getRGB();
				}
				break;
			case 2:
				// Diamond is DIAMOND_VERY_GOOD with random colors!
				diamondType = DIAMOND_VERY_GOOD;
			    for (int i=0; i<nbPoints; i++) {
			      color[i] = parent.color(parent.random(255), parent.random(255), parent.random(255)); // random RGB color
			    }
				break;
			default:
				// Diamond is DIAMOND_GOOD which are green
				diamondType = DIAMOND_GOOD;
				for(int i=0;i<nbPoints;i++){
					color[i] = Color.GREEN.getRGB();
				}
				break;
		  }	
		  isDiamondCaught = false;	// give freedom to diamond
		 
	  }

	public float[] getAbsoluteCordinates() {
		float[] cordinates = {x,y,z};
		return cordinates;
	}
}
