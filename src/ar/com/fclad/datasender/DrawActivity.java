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
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class DrawActivity extends Activity implements SensorEventListener {
	
	public static final String TAG = "DrawActivity";
	private SensorManager sensorManager;
	private float timestampGyro;
	private static final float NS2S = 1.0f / 1000000000.0f;
	private int mode;
	
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
	
				String str = dT+":"+event.values[0]+":"+event.values[1]+":"+event.values[2];
				if(mode==MainActivity.BLUETOOTH_CONNECTION_MODE){
					Intent sendmsg = new Intent(this,BluetoothServerService.class);
					sendmsg.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.SENDMSG);
					sendmsg.putExtra(BluetoothServerService.ORIGIN, "G");
					sendmsg.putExtra(BluetoothServerService.MSG, str);
					startService(sendmsg);
				}
				if(mode==MainActivity.TCP_CONNECTION_MODE){
					Intent sendmsg = new Intent(this,TCPclientService.class);
					sendmsg.putExtra(TCPclientService.COMMAND, TCPclientService.SENDMSG);
					sendmsg.putExtra(TCPclientService.ORIGIN, "G");
					sendmsg.putExtra(TCPclientService.MSG, str);
					startService(sendmsg);
				}
				
			}
			
			
		}	
		
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
		    canvas.drawColor(Color.BLACK);
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
					     
					     //Intent msgIntent = new Intent(getContext(),TCPclientService.class);
					     if(mode==MainActivity.BLUETOOTH_CONNECTION_MODE){
					    	 Intent bluetoothIntent = new Intent(getContext(),BluetoothServerService.class);
						     bluetoothIntent.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.SENDMSG);
						     bluetoothIntent.putExtra(BluetoothServerService.ORIGIN, "S");
						     bluetoothIntent.putExtra(BluetoothServerService.MSG,line);
					    	 getContext().startService(bluetoothIntent);
					     }
					     if(mode==MainActivity.TCP_CONNECTION_MODE){
					    	 Intent tcpIntent = new Intent(getContext(),TCPclientService.class);
						     tcpIntent.putExtra(TCPclientService.COMMAND, TCPclientService.SENDMSG);
						     tcpIntent.putExtra(TCPclientService.ORIGIN, "S");
						     tcpIntent.putExtra(TCPclientService.MSG,line);
					    	 getContext().startService(tcpIntent);
					     }
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
		      float deltaScale = (scaledValue-oldScale)*10;
		      if(Math.abs(deltaScale)>sensibilityGesture){
		    	  //Log.w("DrawView",deltaScale>0 ? "ZoomIn" : "ZoomOut");
		    	  //circleRadius += (deltaScale>0 ? 1: -1);
		    	  circleRadius += deltaScale;
		    	  if(circleRadius<1)
		    		  circleRadius = 1;
		    	  //Log.w("DrawView",""+deltaScale);
		    	  
		    	  String msg = "0:0:0:"+deltaScale;
		    	  
		    	  //Intent msgIntent = new Intent(getContext(), TCPclientService.class);
		    	  if(mode==MainActivity.BLUETOOTH_CONNECTION_MODE){
		    		  Intent msgIntent = new Intent(getContext(), BluetoothServerService.class);
			    	  msgIntent.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.SENDMSG);
			    	  msgIntent.putExtra(BluetoothServerService.ORIGIN, "S");
			    	  msgIntent.putExtra(BluetoothServerService.MSG,msg);
			    	  getContext().startService(msgIntent);
		    	  }
		    	  if(mode==MainActivity.TCP_CONNECTION_MODE){
		    		  Intent msgIntent = new Intent(getContext(), TCPclientService.class);
			    	  msgIntent.putExtra(TCPclientService.COMMAND, TCPclientService.SENDMSG);
			    	  msgIntent.putExtra(TCPclientService.ORIGIN, "S");
			    	  msgIntent.putExtra(TCPclientService.MSG,msg);
			    	  getContext().startService(msgIntent);
		    	  }
		    	  
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
