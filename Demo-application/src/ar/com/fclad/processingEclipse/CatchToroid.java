package ar.com.fclad.processingEclipse;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

// This class has been extracted and modified from Procesing examples
/*
 *  * Interactive Toroid
 * 	by Ira Greenberg. 
 * 
 * Illustrates the geometric relationship between Toroid, Sphere, and Helix
 * 3D primitives, as well as lathing principal.
 * 
 */

public class CatchToroid extends CatchObject{

	private int pts = 40; 
	private float angle = 0;
	private float externalRadius = 40;

	// lathe segments
	private int segments = 60;
	private float latheAngle = 0;
	private float radius = 100;
	
	//vertices
	PVector vertices[], vertices2[];
	
	public CatchToroid(PApplet p, CommunicationThread commtThread, float radius) {
		super(p, commtThread);
		changeRadius(radius);
	}
	
	public void changeRadius(float radius){
		this.radius = radius;
	}
	
	public float getRadius(){
		return radius-externalRadius/2;
	}

	public void display(){
		super.display();
		parent.fill(150, 195, 125);
		// initialize point arrays
		vertices = new PVector[pts+1];
		vertices2 = new PVector[pts+1];
		 // fill arrays
		for(int i=0; i<=pts; i++){
			vertices[i] = new PVector();
			vertices2[i] = new PVector();
			vertices[i].x = (float) (radius + Math.sin(Math.toRadians(angle))*externalRadius);
			vertices[i].z = (float) (Math.cos(Math.toRadians(angle))*externalRadius);
			angle+=360.0/pts;
		}
		
		// draw toroid
		latheAngle = 0;
		for(int i=0; i<=segments; i++){
			parent.beginShape(PConstants.QUAD_STRIP);
			for(int j=0; j<=pts; j++){
				if (i>0){
				parent.vertex(vertices2[j].x, vertices2[j].y, vertices2[j].z);
				}
				vertices2[j].x = (float) (Math.cos(Math.toRadians(latheAngle))*vertices[j].x);
				vertices2[j].y = (float) (Math.sin(Math.toRadians(latheAngle))*vertices[j].x);
				vertices2[j].z = vertices[j].z;
				parent.vertex(vertices2[j].x, vertices2[j].y, vertices2[j].z);
			}
			latheAngle+=360.0/segments;
			parent.endShape();
		}
		super.afterDisplay();
	}

}
