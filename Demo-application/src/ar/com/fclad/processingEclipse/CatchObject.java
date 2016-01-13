package ar.com.fclad.processingEclipse;

import processing.core.PApplet;
import processing.core.PMatrix3D;


// TODO comment
public abstract class CatchObject {
	
	protected CommunicationThread commThread;
	protected PApplet parent;
	private float 	z=0; 
	private PMatrix3D finalMatrix;
	
	public CatchObject(PApplet p, CommunicationThread commThread) {
		this.parent = p;
		this.commThread = commThread;
	}
	
	public float[] getAbsoluteCordinates(){
		float[] resultVector = {-100,-100,100};
		float[] originVector = {0, 0, 0};
		finalMatrix.mult(originVector, resultVector);
		//float[] cordinates = {resultVector.x,resultVector.y,resultVector.z};
		return resultVector;
	}
	
	public void display(){
		parent.pushMatrix();
	    PMatrix3D translationMatrix = new PMatrix3D(1,0,0,0,
	    											0,1,0,0,
	    											0,0,1,z,
	    											0,0,0,1);
	    
	    PMatrix3D rotationMatrix = commThread.getRotationTranslationMatrix();
	    // Eliminate translation
	    rotationMatrix.m03 = 0;
	    rotationMatrix.m13 = 0;
	    rotationMatrix.m23 = 0;
	    
	    rotationMatrix.apply(translationMatrix);
	    
	    finalMatrix = rotationMatrix;
	    
	    parent.setMatrix(finalMatrix); 
	}
	
	public final void afterDisplay(){
		parent.popMatrix();
	}
	
	public void setZ(float z){
		this.z = z;
	}
}
