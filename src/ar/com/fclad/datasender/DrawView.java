package ar.com.fclad.datasender;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

// https://developer.android.com/training/gestures/detector.html

public class DrawView extends View {
    
  private boolean isScaling;
  
  private float x,y;
  private float oldX = 0;
  private float oldY = 0;
  private float oldScale = 0;
  private static final float sensibilityFinger = 5;
  
  private float scaledValue = 1.0f;
  private ScaleGestureDetector scaleGestureDetector;
  private static final float sensibilityGesture = 0.01f;
  
  private Paint paint;
  private float circleRadius = 10f;

  public DrawView(Context context) {
    super(context);
    scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
    setFocusable(true);
    paint = new Paint();
    paint.setStrokeWidth(6);
    paint.setColor(Color.RED);
    paint.setStyle(Paint.Style.FILL_AND_STROKE);
    paint.setAntiAlias(true);
    paint.setStrokeJoin(Paint.Join.BEVEL);
   
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawCircle(x, y, circleRadius, paint);
   
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
	  int pointerIndex = event.getActionIndex();
	  int pointerID = event.getPointerId(pointerIndex);
	
	  scaleGestureDetector.onTouchEvent(event);
	  if(!isScaling){
		  if(pointerID == 0){
			  x = event.getX();
			  y = event.getY();
			  switch (event.getAction()){
			  case MotionEvent.ACTION_DOWN:
			      oldX = x;
			      oldY = y;
			      return true;
			    case MotionEvent.ACTION_MOVE:
			      float deltaX = x-oldX;
			      float deltaY = y-oldY;
			      
			     String line = "0:";
			     if((Math.abs(deltaX)> sensibilityFinger)&&(Math.abs(deltaY) > sensibilityFinger)){
			    	 //Log.w("DrawView",deltaX>0 ? "MoveRight" : "MoveLeft");
			    	 line += deltaX+":"+deltaY;
			     }
			     else if(Math.abs(deltaX)> sensibilityFinger){
			    	 line += deltaX+":"+0;
			     }
			     
			     else if(Math.abs(deltaY) > sensibilityFinger){
			    	// Log.w("DrawView", deltaY> 0 ? "MoveDown" : "MoveUp");
			    	 line += 0+":"+deltaY;
			     }
			     else{
			    	 return false;
			     }
			     
			     line += ":"+0;
			     //Log.w("DrawView",line);
			     
			     Intent msgIntent = new Intent(getContext(),TCPclientService.class);
			     msgIntent.putExtra(TCPclientService.COMMAND, TCPclientService.SENDMSG);
		    	 msgIntent.putExtra(TCPclientService.ORIGIN, "S");
		    	 msgIntent.putExtra(TCPclientService.MSG,line);
		    	 getContext().startService(msgIntent);

			      oldX = x;
			      oldY = y;
			      break;
			    case MotionEvent.ACTION_UP:
			      // nothing to do
			      break;
			    default:
			      return false;
			  }  
		  }
	  }
	 
    invalidate();
    return true;
  }

  private class ScaleListener extends
      ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      scaledValue *= detector.getScaleFactor();
      float deltaScale = scaledValue-oldScale;
      if(Math.abs(deltaScale)>sensibilityGesture){
    	  //Log.w("DrawView",deltaScale>0 ? "ZoomIn" : "ZoomOut");
    	  circleRadius += (deltaScale>0 ? 1: -1);
    	  if(circleRadius<1)
    		  circleRadius = 1;
    	  //Log.w("DrawView",""+deltaScale);
    	  
    	  String msg = "0:0:0:"+deltaScale*10;
    	  
    	  Intent msgIntent = new Intent(getContext(), TCPclientService.class);
    	  msgIntent.putExtra(TCPclientService.COMMAND, TCPclientService.SENDMSG);
    	  msgIntent.putExtra(TCPclientService.ORIGIN, "S");
    	  msgIntent.putExtra(TCPclientService.MSG,msg);
    	  getContext().startService(msgIntent);
      }
      
      oldScale = scaledValue;

      invalidate();
      return true;
    }
    public boolean onScaleBegin(ScaleGestureDetector detector){
    	isScaling = true;
    	return true;
    }
    public void onScaleEnd(ScaleGestureDetector detector){
    	isScaling = false;
    	
    }
  }
}
