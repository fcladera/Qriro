package ar.com.fclad.processingEclipse;

import java.awt.Color;

import processing.core.PApplet;
import processing.core.PFont;

@SuppressWarnings("serial")
public class ScoreCounter extends PApplet {
	PApplet parent;
	private int score;
	private PFont f;
	
	public ScoreCounter(PApplet p) {
		this.parent = p;
		score = 100;
		f = createFont("Georgia", 30);
		parent.textFont(f);
	}
	
	public synchronized void decreaseScore(int decreaseValue){
		score -= decreaseValue;
	}
	
	public synchronized void increaseScore(int increaseValue){
		score += increaseValue;
	}
	
	public int getScore(){
		return score;
	}
	
	public void display(){
		parent.pushMatrix();
		parent.fill(Color.WHITE.getRGB());
		parent.text("SCORE: "+score, parent.width-200, parent.height-50);
		parent.popMatrix();	
	}
}
