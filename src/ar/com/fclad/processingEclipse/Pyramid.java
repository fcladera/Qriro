package ar.com.fclad.processingEclipse;

import processing.core.*;

public class Pyramid {
	PApplet parent;
	
	PVector[] v = new PVector[5]; // the 5 relative coordinates of the pyramid as an object
	int[] c = new int[5]; // the colors for each coordinate
	float pHeight, pRadius; // the height and base radius of the shape
	float speed, transparency; // the movement speed and the color transparency
	float x, y, z; // the position of the shape as a whole
	
	public Pyramid(PApplet p,float pHeight, float pRadius) {
		parent = p;
		this.pHeight = pHeight/2; // this is done so the pyramid rotates around the center (see createPyramid() method)
	    this.pRadius = pRadius; // the radius of the shape
	    this.speed = parent.random(150/8, 50); // set the speed randomly based on the global MAXSPEED variable
	    createPyramid(); // set the relative coordinates of the pyramid as an object
	    z = parent.random(-5000, 750); // randomly set the z position of the shape as a whole
	    reset(); // randomly set the x and y position of the shape as a whole and the colors of the shape
	}
	
	void createPyramid() {
	    v[0] = new PVector(0, -pHeight, 0);                                      // top of the pyramid
	    v[1] = new PVector((float)(pRadius*Math.cos(Math.PI/2)), pHeight, (float)(pRadius*Math.sin(Math.PI/2))); // base point 1
	    v[2] = new PVector((float)(pRadius*Math.cos(Math.PI)), pHeight, (float)(pRadius*Math.sin(Math.PI)));           // base point 2
	    v[3] = new PVector((float)(pRadius*Math.cos(1.5*Math.PI)), pHeight, (float)(pRadius*Math.sin(1.5*Math.PI)));   // base point 3
	    v[4] = new PVector((float)(pRadius*Math.cos(2*Math.PI)), pHeight, (float)(pRadius*Math.sin(2*Math.PI)));   // base point 4
	  }

	  // controls the z movement of the shape, when the z goes beyond the camera
	  // it's reset to position in the distance and the xy position and colors are randomly set
	  // the transparency is determined by the z position
	  void update() {
	    z += speed; // increase z by speed
	    if (z > 750) { z = -5000; reset(); } // if beyond the camera, reset() and start again
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
	    for (int i=0; i<5; i++) {
	      parent.fill(c[i], transparency); // use the color, but with the given z-based transparency
	      parent.vertex(v[i].x, v[i].y, v[i].z); // set the vertices based on the object coordinates defined in the createShape() method
	    }
	    // add the 'first base vertex' to close the shape
	    parent.fill(c[1], transparency);
	    parent.vertex(v[1].x, v[1].y, v[1].z);
	    parent.endShape(); // finalize the Shape
	    
	    // draw the base QUAD of the pyramid
	    parent.fill(c[1], transparency); // use a single color (optional: for vertex colors you can also put this in the for loop)
	    parent.beginShape(PConstants.QUADS); // it's a QUAD so the QUADS shapeMode is the most suitable
	    for (int i=1; i<5; i++) {
	      parent.vertex(v[i].x, v[i].y, v[i].z); // the 4 base points
	    }
	    parent.endShape(); // finalize the Shape
	    
	    parent.popMatrix(); // use push/popMatrix so each Shape's translation/rotation does not affect other drawings
	  }

	  // randomly sets the xy position of the shape as a whole and the colors of the shape  
	  void reset() {
	    x = parent.random(-2*parent.width, 3*parent.width); // set the x position
	    y = parent.random(-parent.height, 2*parent.height); // set the y position
	    c[0] = parent.color(parent.random(150, 255), parent.random(150, 255), parent.random(150, 255)); // set the top color (a bit lighter)
	    // randomly set the 4 colors in the base of the shape
	    for (int i=1; i<5; i++) {
	      c[i] = parent.color(parent.random(255), parent.random(255), parent.random(255)); // random RGB color
	    }
	  }
}
