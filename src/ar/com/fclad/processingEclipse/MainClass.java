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

	//=======================================================================
	// Shared variables
	
	private CommunicationThread commThread; 
	private boolean calibrated=true;
	PFont f;
	
	//=======================================================================
	// Application running
	private static final int APPLICATION_DIAMONDS = 0;
	private static final int APPLICATION_CUBE = 1;
	private static final int APPLICATION_LAND = 2;
	private int currentApplication = APPLICATION_CUBE;
	
	//=======================================================================
	// Diamonds game program variables
	
	private int NUMSHAPES = 5; // number of flying diamonds
	private static final int MAX_RADIUS_DIAMONDS = 50;
	private static final int MIN_RADIUS_DIAMONDS = 25;
	private static final int RADIUS_CATCH_OBJECT = 150;
	
	private boolean SphereOverThoroid = false;

	ArrayList <Diamond> shapes = new ArrayList <Diamond> (); // arrayList to store all the diamonds
	
	// These figures can catch diamonds in the game
	CatchSphere catchSphere = null;
	private CatchToroid toroid;
	
	// Text to display score
	ScoreCounter scoreCounter;
	
	//=======================================================================
	// Cube program variables
	
	private Cube cube = null;
	

	//=======================================================================
	// Land program variables
	
	
	
	
	
	public MainClass() {
		commThread = new CommunicationThread(this);
		commThread.start();
		
	}

	public void setup() {
		size(800, 800, PGraphicsOpenGL.OPENGL); // use OpenGL renderer
		//hint(DISABLE_DEPTH_TEST);
		smooth(6); 
		f = createFont("Georgia", 30);
		textFont(f);
		
		//=======================================================================
		// Diamonds game program
		
		// init scoreCounter
		scoreCounter = new ScoreCounter(this);
		
		// init diamonds shapes
		for (int i=0; i<NUMSHAPES; i++) {
		  float r = random(MIN_RADIUS_DIAMONDS, MAX_RADIUS_DIAMONDS);
		  shapes.add( new Diamond(this, scoreCounter, r) );
		}
		
		catchSphere = new CatchSphere(this,commThread,RADIUS_CATCH_OBJECT);
		catchSphere.setZ((float) (-max(width,height)*1.5));
		toroid = new CatchToroid(this, commThread, RADIUS_CATCH_OBJECT);
		toroid.setZ((float) (-max(width,height)*1.5));
		
		//=======================================================================
		// Cube program
		
		cube = new Cube(this,commThread,60);
		
		//=======================================================================
		// Land program
		
		//=======================================================================
		// Ask first rotation matrix
		commThread.askNewRotationMatrix();
		  
	}

	public void draw() {
		// Any active command?
		int command = commThread.getCommand();
		if(command!=0){
			switch (command) {
				
			case CommunicationThread.COMMAND_DOUBLE_TAP:
				System.out.println("Double tap");
				changeApplication();
				break;
			case CommunicationThread.COMMAND_CALIBRATION_FINISHED:
				System.out.println("Calibration finished");
				calibrated = true;
			case CommunicationThread.COMMAND_THREAD_START:
				System.out.println("Begin data rcv");
			default:
				break;
			}
			commThread.setCommand(0); // ACK command
		}
		
		if(!calibrated){
			background(Color.BLACK.getRed());
			fill(Color.GREEN.getRGB());
			text("Waiting for calibration...",width/2-175,height/2);
		}
		else{
			// System calibrated! go on
			// Select application to draw
			
			switch (currentApplication) {
			case APPLICATION_DIAMONDS:
				//=======================================================================
				// Diamonds game program
				stroke(0);
				
				if(!SphereOverThoroid){
					background(Color.BLACK.getRGB()); 
					toroid.display();
					for (Diamond d : shapes) {
						d.display();
						if(detectCapture(	toroid.getAbsoluteCordinates(),
											toroid.getRadius(),								
											d.getAbsoluteCordinates(),
											d.getRadius())){
							d.diamondCaught();
						}
						d.update();
					}
					
				}
				else{
					background(Color.white.getRGB());
					catchSphere.display();
					for (Diamond d : shapes) {
						d.display();
						if(detectCapture(	catchSphere.getAbsoluteCordinates(),
											catchSphere.getRadius(),		
											d.getAbsoluteCordinates(),
											d.getRadius())){
							d.diamondCaught();
						}
						d.update();
					}
					catchSphere.display();
				}
				
				scoreCounter.display();
				noStroke();
				break;
			case APPLICATION_CUBE:
				cube.display();
			default:
				break;
			}
			
		}
		
		
		

		//=======================================================================
		// Ask new rotation matrix
		commThread.askNewRotationMatrix();

	}

	public void mousePressed() {
	  if (mouseButton==LEFT) {
		  SphereOverThoroid = !SphereOverThoroid;
		  if(SphereOverThoroid)
			  hint(DISABLE_DEPTH_TEST);
		  else
			  hint(ENABLE_DEPTH_TEST);
	  } 
	  	if (mouseButton==RIGHT){ 
	  	} 
	}
	
	private void changeApplication(){
		currentApplication++;
		if(currentApplication==APPLICATION_LAND+1){
			currentApplication = APPLICATION_DIAMONDS;
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

}


