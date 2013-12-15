package ar.com.fclad.processingEclipse;

import processing.core.*;

public class Diamond {
	PApplet parent;
	private static final int nbPoints = 14;
	private PVector[] v = new PVector[nbPoints]; // the 5 relative coordinates of the pyramid as an object
	private int[] c = new int[nbPoints]; // the colors for each coordinate
	private float pHeight, pRadius; // the height and base radius of the shape
	private float speed, transparency; // the movement speed and the color transparency
	private float 	x=400, 
					y=300, 
					z=0; // the position of the shape as a whole
	
	
	
	public Diamond(PApplet p, float pRadius) {
		parent = p;
		this.pHeight = pRadius; // this is done so the pyramid rotates around the center (see createPyramid() method)
	    this.pRadius = pRadius; // the radius of the shape
	    this.speed = parent.random(10, 20); // set the speed randomly based on the global MAXSPEED variable
	    createPyramid(); // set the relative coordinates of the pyramid as an object
	    //z = parent.random(-5000, 750); // randomly set the z position of the shape as a whole
	    reset(); // randomly set the x and y position of the shape as a whole and the colors of the shape
	}
	
	public float[] getPositionAndRadius(){
		float[] position = new float[4];
		position[0] = x;
		position[1] = y;
		position[2] = z;
		position[3] = pRadius;
		return position;
	}
	
	public void destroyDiamond(){
		reset();
	}
	
	void createPyramid() {
	    v[0] = new PVector(0, -pHeight, 0); // top
	    for(int i=1;i<nbPoints-1;i++){
	    	v[i] = new PVector((float)(pRadius*Math.cos(2*Math.PI/(nbPoints-2)*(i-1))), 0, (float)(pRadius*Math.sin(2*Math.PI/(nbPoints-2)*(i-1)))); // base point 1
	    }
	    v[nbPoints-1] = new PVector(0, pHeight, 0); // bottom 
	  }

	  // controls the z movement of the shape, when the z goes beyond the camera
	  // it's reset to position in the distance and the xy position and colors are randomly set
	  // the transparency is determined by the z position
	  void update() {
	    z += speed; // increase z by speed
	    if (z > 750){ // if beyond the camera, reset() and start again
	    	z = -5000; 
	    	reset(); 
	    } 
	    transparency = z<-2500?PApplet.map(z, -5000, -2500, 0, 255):255; // far away slowly increase the transparency, within range is fully opaque
	  }

	  // displays the shape  
	  void display() {
	    parent.pushMatrix(); // use push/popMatrix so each Shape's translation/rotation does not affect other drawings

	    // move and rotate the shape as a whole    
	    parent.translate(x, y, z); // translate to the position of the shape
	    parent.rotateY((float)(x + parent.frameCount*0.01)); // rotate around the Y axis based on the x position and frameCount
	    parent.rotateX((float)(y + parent.frameCount*0.02)); // rotate around the X axis based on the y position and frameCount
	    
	    // draw the 4 side triangles of the pyramid, each connected to the top of the pyramid
	    parent.beginShape(PConstants.TRIANGLE_FAN); // TRIANGLE_FAN is suited for this, it starts with the center point c[0]
	    for (int i=0; i<nbPoints-1; i++) {
	      parent.fill(c[i], transparency); // use the color, but with the given z-based transparency
	      parent.vertex(v[i].x, v[i].y, v[i].z); // set the vertices based on the object coordinates defined in the createShape() method
	    }
	    parent.fill(c[1], transparency);
	    parent.vertex(v[1].x, v[1].y, v[1].z);
	    
	    parent.endShape();
	    parent.beginShape(PConstants.TRIANGLE_FAN);
	    for (int i=nbPoints-1; i>0; i--) {
		      parent.fill(c[i], transparency); // use the color, but with the given z-based transparency
		      parent.vertex(v[i].x, v[i].y, v[i].z); // set the vertices based on the object coordinates defined in the createShape() method
		}
	    parent.fill(c[nbPoints-2], transparency);
	    parent.vertex(v[nbPoints-2].x, v[nbPoints-2].y, v[nbPoints-2].z);
	    
	    // add the 'first base vertex' to close the shape
	    
	    parent.endShape(); // finalize the Shape
	    //parent.sphere(pRadius*5/6);
	    // draw the base QUAD of the pyramid
	    /*
	    parent.fill(c[1], transparency); // use a single color (optional: for vertex colors you can also put this in the for loop)
	    
	    parent.beginShape(PConstants.QUADS); // it's a QUAD so the QUADS shapeMode is the most suitable
	    for (int i=1; i<nbPoints-1; i++) {
	      parent.vertex(v[i].x, v[i].y, v[i].z); // the 4 base points
	    }
	    parent.endShape(); // finalize the Shape
	    */
	    
	    parent.popMatrix(); // use push/popMatrix so each Shape's translation/rotation does not affect other drawings
	  }

	  // randomly sets the xy position of the shape as a whole and the colors of the shape  
	  void reset() {
	    //x = parent.random(-2*parent.width, 3*parent.width); // set the x position
	    //y = parent.random(-parent.height, 2*parent.height); // set the y position
		  x = parent.width/2+100;
		  y = parent.height/2+100;
		  c[0] = parent.color(parent.random(150, 255), parent.random(150, 255), parent.random(150, 255)); // set the top color (a bit lighter)
		  c[nbPoints-1] = parent.color(parent.random(150, 255), parent.random(150, 255), parent.random(150, 255)); // set the top color (a bit lighter)
	    // randomly set the 4 colors in the base of the shape
	    for (int i=1; i<nbPoints-1; i++) {
	      c[i] = parent.color(parent.random(255), parent.random(255), parent.random(255)); // random RGB color
	    }
	  }
}
