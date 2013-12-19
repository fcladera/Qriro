package ar.com.fclad.processingEclipse;

import java.awt.Color;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import processing.core.*;
import processing.opengl.*;

@SuppressWarnings("serial")
public class MainClass extends PApplet{
	

	public static void main(String args[]) {
		PApplet.main(new String[] { "--present", "MyProcessingSketch" });
	}

	public int NUMSHAPES = 5; // the number of flying pyramids
	public float MAXSPEED = 5; // the maximum speed at which a pyramid may move

	ArrayList <Diamond> shapes = new ArrayList <Diamond> (); // arrayList to store all the shapes
	boolean bLights, 	
			bWhitebackground=true, 
			setPerspective; // booleans for toggling lights and background
	
	Diamond testDiamond;
	CatchSphere catchSphere = null;
	
	//Client myClient;
	private Cube cube = null;
	String dataIn = null;
	
	private CatchToroid toroid;
	
	boolean connectionAvailable = true;
	
	CommunicationThread commThread;
	
	public MainClass() {
		commThread = new CommunicationThread(this);
		commThread.start();
		
	}

	public void setup() {
		size(800, 800, PGraphicsOpenGL.OPENGL); // use the P3D OpenGL renderer
		//hint(DISABLE_DEPTH_TEST);
		//noStroke(); // turn off stroke (for the rest of the sketch)
		smooth(6); // set smooth level 6 (default is 2)
		// create all the shapes with a certain radius and height
		
		for (int i=0; i<NUMSHAPES; i++) {
		  float r = random(25, 50);
		  shapes.add( new Diamond(this, r) );
		}
		
		
		/*
		float fov = (float) (PI/6.0);
		float cameraZ = (float) ((height/2.0) / tan((float) (fov/2.0)));
		perspective((float)fov, (float)width/(float)height, 
		            (float)(cameraZ/10.0), (float)(cameraZ*10.0));
		*/
		
		//testDiamond = new Diamond(this, 150);
		
		catchSphere = new CatchSphere(this
		  ,commThread,100);
		catchSphere.setZ((float) (-max(width,height)*1.5));
		
		cube = new Cube(this,commThread);
		
		toroid = new CatchToroid(this, commThread, 50, 120);
		toroid.setZ((float) (-max(width,height)*1.5));
		// Ask first rotation matrix
		commThread.askNewRotationMatrix();
//commThread.askNewRotationAngles();
		  
	}

	public void draw() {
		// Any active command?
		int command = commThread.getCommand();
		if(command!=0){
			switch (command) {
				
			case CommunicationThread.COMMAND_DOUBLE_TAP:
				System.out.println("Double tap");				
				break;
			case CommunicationThread.COMMAND_CALIBRATION_FINISHED:
				System.out.println("Calibration finished");
			case CommunicationThread.COMMAND_THREAD_START:
				System.out.println("Begin data rcv");
			default:
				break;
			}
			commThread.setCommand(0); // ACK command
		}
		
		
		background(Color.BLACK.getRGB()); 
		lights();
		camera((float)(0.0), (float)(0.0), (float)(220.0), // eyeX, eyeY, eyeZ
				(float)mouseX, (float)mouseY, (float)(0.0), // centerX, centerY, centerZ
				(float)(0.0), (float)(1.0), (float)(0.0)); // upX, upY, upZ
		
		
		//catchSphere.display();
		stroke(0);
		toroid.display();
		noStroke();
		
		/*
		pushMatrix();
		fill(Color.RED.getRGB());
		translate(mouseX, mouseY, -1600);
		circle.drawPoints();;
		popMatrix();
		*/
		//float[] cordinates = catchSphere.getAbsoluteCordinates();
		//pushMatrix();
		//translate(cordinates[0], cordinates[1], cordinates[2]);
		//setMatrix(new PMatrix3D(1,0,0,cordinates[0],0,1,0,cordinates[1],0,0,1,cordinates[2],0,0,0,1));
		//sphere(100);
		//System.out.print(cordinates[0]+" "+cordinates[1]+" "+cordinates[2]+"\t");
		//System.out.println(Math.sqrt(Math.pow(cordinates[0], 2)+Math.pow(cordinates[1], 2)+Math.pow(cordinates[2], 2))+"");
		//popMatrix();
	
		//testDiamond.display();
		
		
		for (Diamond d : shapes) {
			d.display();
			if(detectCapture(	//catch.getAbsoluteCordinates(),
								//catchSphere.getRadius(),
								toroid.getAbsoluteCordinates(),
								toroid.getRadius(),								
								d.getAbsoluteCordinates(),
								d.getRadius())){
				background(128);
			}
			d.update();
		}
		
		//cube.display();

		commThread.askNewRotationMatrix();

	}

	public void mousePressed() {
	  if (mouseButton==LEFT) { bWhitebackground = !bWhitebackground; } // toggle between black/white background
	  	if (mouseButton==RIGHT){ 
	  		bLights = !bLights; 
	  		//commThread.write("Hello dude!");
	  	} // toggle the use of lights()
	  if (mouseButton == CENTER){
		  setPerspective = !setPerspective;
		  print(setPerspective+"\n");
	  }
	}
	
	private boolean detectCapture(float[] ballPos, float ballRad, float[] diamondPos, float diamondRad){
		boolean isCaptured = true;
		for(int i=0;i<3;i++){
			if(abs(ballPos[i]-diamondPos[i])>(ballRad-diamondRad)){
				isCaptured = false;
				break;
			}
		}
		return isCaptured;
	}
	
	/*
	public void clientEvent(Client myClient){
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
		}*/
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


