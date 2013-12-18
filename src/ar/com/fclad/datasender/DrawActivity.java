package ar.com.fclad.datasender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class DrawActivity extends Activity implements SensorEventListener{
	
	public static final String TAG = "DrawActivity";
	private SensorManager sensorManager;
	private float timestampGyro;
	private static final float NS2S = 1.0f / 1000000000.0f;
	private int mode;
	
	GestureDetector gestureDetector;
	
	// Command codes sent to dataProcessor
	private static final int COMMAND_TOGGLE_FILTER = 0x10;
	private static final int COMMAND_DOUBLE_TAP = 0x11;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(new DrawView(this));
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Bundle bundle = getIntent().getExtras();
		mode = bundle.getInt(MainActivity.MODE);
		// Log.w(TAG,mode+"");
	}
	
	@Override
	protected void onPause() {
		sensorManager.unregisterListener(this);
		super.onPause();
	}

	@Override
	protected void onResume() {
		sensorManager.registerListener(this,
		        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
		        SensorManager.SENSOR_DELAY_GAME);
		super.onResume();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {		
	}
	
	private void sendMessage(String origin, String message){
		if(mode==MainActivity.BLUETOOTH_CONNECTION_MODE){
			Intent sendmsg = new Intent(this,BluetoothServerService.class);
			sendmsg.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.SENDMSG);
			sendmsg.putExtra(BluetoothServerService.ORIGIN, origin);
			sendmsg.putExtra(BluetoothServerService.MSG, message);
			startService(sendmsg);
		}
		if(mode==MainActivity.TCP_CONNECTION_MODE){
			Intent sendmsg = new Intent(this,TCPclientService.class);
			sendmsg.putExtra(TCPclientService.COMMAND, TCPclientService.SENDMSG);
			sendmsg.putExtra(TCPclientService.ORIGIN, origin);
			sendmsg.putExtra(TCPclientService.MSG, message);
			startService(sendmsg);
		}
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
			float dT = 0;
			if(timestampGyro!=0){
				dT = (event.timestamp-timestampGyro)*NS2S;
			}
			else{
				timestampGyro = event.timestamp;
				return;
			}
			if(dT!=0){	// Fixes bug in android, timestamp constant
			
				timestampGyro = event.timestamp;
	
				String message = dT+":"+event.values[0]+":"+event.values[1]+":"+event.values[2];
				sendMessage("G", message);
				
			}
		}	
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.draw, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()) {
		case R.id.toggle_filtering:
			String msg = "0:"+COMMAND_TOGGLE_FILTER+":0:0";
			if(mode==MainActivity.BLUETOOTH_CONNECTION_MODE){
	    		  Intent msgIntent = new Intent(this, BluetoothServerService.class);
		    	  msgIntent.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.SENDMSG);
		    	  msgIntent.putExtra(BluetoothServerService.ORIGIN, "C");
		    	  msgIntent.putExtra(BluetoothServerService.MSG,msg);
		    	  startService(msgIntent);
	    	  }
	    	  if(mode==MainActivity.TCP_CONNECTION_MODE){
	    		  Intent msgIntent = new Intent(this, TCPclientService.class);
		    	  msgIntent.putExtra(TCPclientService.COMMAND, TCPclientService.SENDMSG);
		    	  msgIntent.putExtra(TCPclientService.ORIGIN, "C");
		    	  msgIntent.putExtra(TCPclientService.MSG,msg);
		    	  startService(msgIntent);
	    	  }
			break;

		default:
			break;
		}
		return true;
	}
	
	class DrawView extends View {
	    
		  private boolean isScaling;
		  
		  private float x = 300,y = 300;
		  private float oldX = 0;
		  private float oldY = 0;
		  private float oldScale = 0;
		  private static final float sensibilityFinger = 5;
		  
		  private float scaledValue = 1.0f;
		  private ScaleGestureDetector scaleGestureDetector;
		  private GestureDetector gestureDetector;
		  private static final float sensibilityGesture = 0.01f;
		  
		  private Paint paint;
		  private float circleRadius = 10f;

		  public DrawView(Context context) {
		    super(context);
		    scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		    gestureDetector = new GestureDetector(context, new GestureListener());
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
		    canvas.drawColor(Color.BLACK);
		    canvas.drawCircle(x, y, circleRadius, paint);
		   
		  }

		  @Override
		  public boolean onTouchEvent(MotionEvent event) {
			  int pointerIndex = event.getActionIndex();
			  int pointerID = event.getPointerId(pointerIndex);
			
			  scaleGestureDetector.onTouchEvent(event);
			  gestureDetector.onTouchEvent(event);
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
						      
						     String message = "0:";
						     if((Math.abs(deltaX)> sensibilityFinger)&&(Math.abs(deltaY) > sensibilityFinger)){
						    	 //Log.w("DrawView",deltaX>0 ? "MoveRight" : "MoveLeft");
						    	 message += deltaX+":"+deltaY;
						     }
						     else if(Math.abs(deltaX)> sensibilityFinger){
						    	 message += deltaX+":"+0;
						     }
						     
						     else if(Math.abs(deltaY) > sensibilityFinger){
						    	// Log.w("DrawView", deltaY> 0 ? "MoveDown" : "MoveUp");
						    	 message += 0+":"+deltaY;
						     }
						     else{
						    	 return false;
						     }
						     
						     message += ":"+0;
						     //Log.w("DrawView",line);
						     
						     sendMessage("S", message);
						    
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
		  
		  private class GestureListener extends 
		  		GestureDetector.SimpleOnGestureListener{

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				Log.d(TAG,"DoubleTap");
				String message = "0:"+COMMAND_DOUBLE_TAP+":0:0";
				sendMessage("C", message);
				return super.onDoubleTap(e);
			}
			  
			  
		  }

		  private class ScaleListener extends
		      ScaleGestureDetector.SimpleOnScaleGestureListener {
		    @Override
		    public boolean onScale(ScaleGestureDetector detector) {
		      scaledValue *= detector.getScaleFactor();
		      float deltaScale = (scaledValue-oldScale)*10;
		      if(Math.abs(deltaScale)>sensibilityGesture){
		    	  //Log.w("DrawView",deltaScale>0 ? "ZoomIn" : "ZoomOut");
		    	  //circleRadius += (deltaScale>0 ? 1: -1);
		    	  circleRadius += deltaScale;
		    	  if(circleRadius<1)
		    		  circleRadius = 1;
		    	  //Log.w("DrawView",""+deltaScale);
		    	  
		    	  String message = "0:0:0:"+deltaScale;
		    	  sendMessage("S", message);
		    	
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
}
