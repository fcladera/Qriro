package ar.com.fclad.processingEclipse;

import java.awt.Color;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PMatrix3D;

//This class has been extracted and modified from Procesing examples
/*
 * Texture Cube
 * by Dave Bollinger.
 * 
 * Drag mouse to rotate cube. Demonstrates use of u/v coords in 
 * parent.vertex() and effect on texture(). The textures get distorted using
 * the P3D renderer as you can see, but they look great using OPENGL.
*/
public class Cube {

	private PApplet parent;
	private CommunicationThread commThread;
	
	private ArrayList<PImage> textures;
	
	private float size;
	
	public Cube(PApplet p, CommunicationThread commThread, float size) {
		this.parent = p;
		this.commThread = commThread;
		textures = new ArrayList<>();
		System.out.println("Working Directory = " +
	              System.getProperty("user.dir"));
		for(int i=0;i<6;i++){
			textures.add(parent.loadImage("textures/"+i+".jpg"));
		}
		if(size>10){
			this.size = size;
		}
		else{
			size=100;
		}
	 }
	 
	public void display(){
		parent.pushMatrix();
		parent.stroke(0);
		parent.background(Color.WHITE.getRGB());
		parent.fill(123);
		PMatrix3D translationMatrix = new PMatrix3D(1,0,0,0,
				0,1,0,0,
				0,0,1,-400,
				0,0,0,1);
		PMatrix3D rotTransMatr = commThread.getRotationTranslationMatrix();
		rotTransMatr.preApply(translationMatrix);
		parent.setMatrix(rotTransMatr);
		  
		// +Z "front" face
		parent.beginShape(PConstants.QUADS);
		parent.texture(textures.get(0));
		parent.vertex(-size, -size,  size, 0, 0);
		parent.vertex( size, -size,  size, size, 0);
		parent.vertex( size,  size,  size, size, size);
		parent.vertex(-size,  size,  size, 0, size);
		parent.endShape();
		
		// -Z "back" face
		parent.beginShape(PConstants.QUADS);
		parent.texture(textures.get(1));
		parent.vertex( size, -size, -size, 0, 0);
		parent.vertex(-size, -size, -size, size, 0);
		parent.vertex(-size,  size, -size, size, size);
		parent.vertex( size,  size, -size, 0, size);
		parent.endShape();
		
		// +Y "bottom" face
		parent.beginShape(PConstants.QUADS);
		parent.texture(textures.get(2));
		parent.vertex(-size,  size,  size, 0, 0);
		parent.vertex( size,  size,  size, size, 0);
		parent.vertex( size,  size, -size, size, size);
		parent.vertex(-size,  size, -size, 0, size);
		parent.endShape();
		
		// -Y "top" face
		parent.beginShape(PConstants.QUADS);
		parent.texture(textures.get(3));
		parent.vertex(-size, -size, -size, 0, 0);
		parent.vertex( size, -size, -size, size, 0);
		parent.vertex( size, -size,  size, size, size);
		parent.vertex(-size, -size,  size, 0, size);
		parent.endShape();
		
		// +X "right" face
		parent.beginShape(PConstants.QUADS);
		parent.texture(textures.get(4));
		parent.vertex( size, -size,  size, 0, 0);
		parent.vertex( size, -size, -size, size, 0);
		parent.vertex( size,  size, -size, size, size);
		parent.vertex( size,  size,  size, 0, size);
		parent.endShape();
		
		// -X "left" face
		parent.beginShape(PConstants.QUADS);
		parent.texture(textures.get(5));
		parent.vertex(-size, -size, -size, 0, 0);
		parent.vertex(-size, -size,  size, size, 0);
		parent.vertex(-size,  size,  size, size, size);
		parent.vertex(-size,  size, -size, 0, size);
		parent.endShape();
		
		parent.line(-2*size, 0, 0, 2*size, 0, 0);
		parent.line(0, -2*size, 0, 0, 2*size, 0);
		parent.line(0, 0, -2*size, 0, 0, 2*size);
		//parent.box(100);
		parent.popMatrix();

	  }
}
