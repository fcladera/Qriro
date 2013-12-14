package ar.com.fclad.processingEclipse;

import java.util.ArrayList;

import processing.core.*;
import processing.opengl.*;

@SuppressWarnings("serial")
public class MainClass extends PApplet{
	

	public static void main(String args[]) {
		PApplet.main(new String[] { "--present", "MyProcessingSketch" });
	}

	public int NUMSHAPES = 150; // the number of flying pyramids
	public float MAXSPEED = 50; // the maximum speed at which a pyramid may move

	ArrayList <Pyramid> shapes = new ArrayList <Pyramid> (); // arrayList to store all the shapes
	boolean bLights, bWhitebackground, setPerspective; // booleans for toggling lights and background

	public void setup() {
	  size(1280, 720, PGraphicsOpenGL.OPENGL); // use the P3D OpenGL renderer
	  //noStroke(); // turn off stroke (for the rest of the sketch)
	  smooth(6); // set smooth level 6 (default is 2)
	  // create all the shapes with a certain radius and height
	  for (int i=0; i<NUMSHAPES; i++) {
	    float r = random(25, 200);
	    float f = random(2, 5);
	    shapes.add( new Pyramid(this,f*r, r) );
	  }
	}

	public void draw() {
	  background(bWhitebackground?255:0); // draw a white or black background
	  if (bLights) { lights(); } // if true, use lights()
	  if(setPerspective)
		  perspective((float)(PI/3.0), (float) width/height, (float)1, (float)1000000); // perspective to see close shapes
	  // update and display all the shapes
	  /*
	  translate(width/2,height/2);
	  rotateX((float)Math.PI/4);
	  rotateY((float)Math.PI/4);
	  rotateZ((float)Math.PI/4);
	  
	  box(45);
	  */
	  
	  for (Pyramid p : shapes) {
	    p.update();
	    p.display();
	  }
	  
	}

	public void mousePressed() {
	  if (mouseButton==LEFT) { bWhitebackground = !bWhitebackground; } // toggle between black/white background
	  if (mouseButton==RIGHT) { bLights = !bLights; } // toggle the use of lights()
	  if (mouseButton == CENTER){
		  setPerspective = !setPerspective;
		  print(setPerspective+"\n");
	  }
	}
	
	/*
	// EJEMPLO DE CUADROS 2D
	 
	int[] colors = new int[100]; // array to store a selection of random colors
	PGraphics bg; // PGraphics that holds the static background (grid + names)
	float fc1, fc2; // global variables used by all vertices for their dynamic movement

	public void setup() {
	  size(1200, 800, PGraphicsOpenGL.OPENGL); // use the P3D OpenGL renderer
	  createBackground(); // create the background PGraphics once, so it can be used in draw() continuously
	  randomColors(); // generate the first set of random colors
	  smooth(6); // set smooth level 6 (default is 2)
	}

	public void draw() {
	  image(bg, 0, 0); // draw the background PGraphics

	  if (frameCount%240==0) { randomColors(); } // generate random colors every 240th frame

	  translate(width/6, height/4); // uniform translate to center the Shapes in each cell

	  // calculate fc1 and fc2 once per draw(), since they are used for the dynamic movement of each drawn vertex
	  fc1 = (float) (frameCount*0.01);
	  fc2 = (float) (frameCount*0.02);

	  // draw all the Shapes using the custom drawShape() method
	  drawShape(0, 0, LINES, 75, 150, 33);
	  drawShape(width/3, 0, TRIANGLES, 75, 150, 20);
	  drawShape(width/3, 0, POINTS, 75, 150, 20);
	  drawShape(2*width/3, 0, TRIANGLE_FAN, 75, 150, 7);
	  drawShape(0, height/2, QUAD_STRIP, 75, 150, 6);
	  drawShape(width/3, height/2, TRIANGLE_STRIP, 75, 150, 17);
	  drawShape(2*width/3, height/2, QUADS, 75, 150, 16);
	}

	// custom drawShape() method with input parameters for the location, shapeMode, diameters and number of segments
	void drawShape(int x, int y, int mode, float diam_inner, float diam_outer, int numSegments) {
	  pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
	  translate(x, y);
	  if (mode==POINTS || mode==LINES) {
	    // for POINTS and LINES use a white, extra thick stroke
	    strokeWeight(2);
	    stroke(255);
	  } else if (mode==QUAD_STRIP || mode==TRIANGLE_STRIP) {
	    // for QUAD_STRIP and TRIANGLE_STRIP use a white, regular stroke
	    strokeWeight(1);
	    stroke(255);
	  } else {
	    // for all other shapeModes do not use stroke
	    noStroke();
	  }
	  beginShape(mode); // input the shapeMode in the beginShape() call
	  if (mode==TRIANGLE_FAN) { vertex(0, 0); } // for the TRIANGLE_FAN a central point is important
	  float step = TWO_PI/numSegments; // generate the step size based on the number of segments
	  for (int i=0; i<numSegments+1; i++) { // +1 so we connect start and end
	    int im = i==numSegments?0:i; // make sure the end equals the start
	    float theta = step * im; // angle for this segment (both vertices)

	    // calculate x and y based on angle
	    float tx = sin(theta);
	    float ty = cos(theta);

	    // each vertex has a noise-based dynamic movement
	    float dynamicInner = (float) (0.5 + noise(fc1+im));
	    float dynamicOuter = (float) (0.5 + noise(fc2+im));

	    // draw the inner and outer vertices based on the angle, radius and dynamic movement
	    // for the QUADS mode reverse every other segment to form a correct QUAD
	    if (mode==QUADS && i%2==0) {
	      fill(colors[im%colors.length]); // get a color from the palette
	      vertex(tx*diam_outer*dynamicOuter, ty*diam_outer*dynamicOuter);
	      fill(colors[(im+1)%colors.length]); // get a different +1 color from the palette
	      vertex(tx*diam_inner*dynamicInner, ty*diam_inner*dynamicInner);
	    } else {
	      fill(colors[im%colors.length]); // get a color from the palette
	      vertex(tx*diam_inner*dynamicInner, ty*diam_inner*dynamicInner);
	      fill(colors[(im+1)%colors.length]); // get a different +1 color from the palette
	      vertex(tx*diam_outer*dynamicOuter, ty*diam_outer*dynamicOuter);
	    }
	  }
	  endShape(); // finalize the Shape
	  popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
	}

	// generate a random color for each color in the array
	void randomColors() {
	  for (int i=0; i<colors.length; i++) {
	    colors[i] = color(random(255),random(255),random(255));
	  }
	}

	// create the static background (grid + names)
	void createBackground() {
	  bg = createGraphics(width, height, JAVA2D); // create PGraphics equal to sketch dimensions
	  bg.beginDraw(); // always start with beginDraw when using a PGraphics
	  bg.background(0); // background of the PGraphics
	  bg.strokeWeight(2);// set strokeWeight for the grid
	  bg.stroke(255); // stroke color
	  // create the grid, in this case two vertical and one horizontal line
	  bg.line(width/3, 0, width/3, height);
	  bg.line(2*width/3, 0, 2*width/3, height);
	  bg.line(0, height/2, width, height/2);
	  bg.textFont(createFont("Arial", 36)); // create the text font
	  bg.textSize(18); // set textSize
	  bg.textAlign(LEFT, TOP); // align it horizontally to the LEFT and vertically to the TOP
	  // uniform translate to position the names in the topleft of each cell
	  bg.translate(5, 5);
	  // draw the names of the used shapeModes using the same coordinates
	  // as the Shapes themselves (except they have different uniform translates)
	  bg.text("LINES", 0, 0);
	  bg.text("TRIANGLES & POINTS", width/3, 0); // TRIANGLES & POINTS are drawn in the same cell
	  bg.text("TRIANGLE_FAN", 2*width/3, 0);
	  bg.text("QUAD_STRIP", 0, height/2);
	  bg.text("TRIANGLE_STRIP", width/3, height/2);
	  bg.text("QUADS", 2*width/3, height/2);
	  bg.endDraw(); // always end with endDraw when using a PGraphics
	}
	*/

}


