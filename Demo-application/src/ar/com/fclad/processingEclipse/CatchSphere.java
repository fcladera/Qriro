package ar.com.fclad.processingEclipse;

import java.awt.Color;

import processing.core.PApplet;

public class CatchSphere extends CatchObject{
	private float pRadius;
	private int color;
	
	
	public CatchSphere(PApplet p, CommunicationThread commThread, float pRadius) {
		super(p, commThread);
	    this.pRadius = pRadius; // the radius of the shape
	    this.color = Color.RED.getRGB();
	    //createSphere();
	}
	
	public float getRadius(){
		return pRadius;
	}
	
	
	public void display() {
		super.display();
		parent.fill(color,(float)128);
	    parent.sphere(pRadius);
	    super.afterDisplay();
	       
	  }
}
