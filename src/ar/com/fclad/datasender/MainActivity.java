package ar.com.fclad.datasender;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

// Resources used to write this code:
//
// TCP CLIENT
// http://examples.javacodegeeks.com/android/core/socket-core/android-socket-example/
// Creative Commons Attribution-ShareAlike 3.0 Unported License
//
// SENSORS
// https://developer.android.com/guide/topics/sensors/sensors_motion.html


public class MainActivity extends Activity implements SensorEventListener{
	
	private SensorManager sensorManager;
	private boolean testingSensors;
	private float timestampGyro;
	private static final float NS2S = 1.0f / 1000000000.0f;
	private long code;

	
	private TextView gyroSensorValues;
	
	// Server parameters
	private static final String localServer = "10.0.0.1";
	private static final String remoteServer = "192.168.2.122";
	private int port = 7777;
	
	
	// UI elements
	private ToggleButton testSensors;
	private Button connectRemote;
	private Button connectLocal;
	private Button disconnect;
	private Button startDraw;
	
	private BroadcastReceiver receiver;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// SENSORS
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		gyroSensorValues = (TextView) findViewById(R.id.GyroSensorValue);
		
		TextView sensorInfo = (TextView) findViewById(R.id.availableSensors);
		
		testingSensors = ((ToggleButton)findViewById(R.id.testSensors)).isChecked();
		
		sensorInfo.setText("GYRO:\t");
		Sensor deviceGyro= sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		sensorInfo.append(deviceGyro.getName()+"\n");	
		
		// UI
		
		connectLocal = (Button) findViewById(R.id.connect_Local);
		connectRemote = (Button) findViewById(R.id.connect_Remote);
		disconnect = (Button) findViewById(R.id.disconnect);
		startDraw = (Button) findViewById(R.id.drawButton);
		testSensors = (ToggleButton) findViewById(R.id.testSensors);
		
		
		// RECEIVER
		receiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle bundle = intent.getExtras();
				if(bundle!=null){
					switch(bundle.getInt(TCPclientService.STATUS)){
					case TCPclientService.CONNECTED:
						Log.d("MainActivity", "Service said connected");
						connectLocal.setEnabled(false);
						connectRemote.setEnabled(false);
						startDraw.setEnabled(true);
						disconnect.setEnabled(true);
						testSensors.setEnabled(false);
						
						
						return;
					
					case TCPclientService.DISCONNECT:
						Log.d("MainActivity", "Service said disconnected");
						connectLocal.setEnabled(true);
						connectRemote.setEnabled(true);
						startDraw.setEnabled(false);
						disconnect.setEnabled(false);
						testSensors.setEnabled(true);
						return;
					}
				}
				
			}
		};
		
	}
	
	@Override
	  protected void onResume() {
	    super.onResume();
	
	   /*
	   if(testingSensors){
		   sensorManager.registerListener(this,
			        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
			        SensorManager.SENSOR_DELAY_GAME);
	   }
	   */

	    registerReceiver(receiver, new IntentFilter(TCPclientService.NOTIFICATION));
	}

	@Override
	protected void onPause() {
		// unregister listener
		super.onPause();
		
		unregisterReceiver(receiver);
		if(testingSensors)
			sensorManager.unregisterListener(this);
		/*
		if(socket!=null){
			if(isSending){
				destroyWriterStream();
				isSending = false;
				sendData.setChecked(false);
				
			}
			disconnect();
			
		}*/
	    
	}
	
	public void onToggleClicked(View view){	
		switch (view.getId()){
		case R.id.testSensors:
			Log.w("MainActivity","Toggled testSensor button");
			testingSensors = ((ToggleButton) view).isChecked();
			if(testingSensors)
			 sensorManager.registerListener(this,
		        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
		        SensorManager.SENSOR_DELAY_GAME);
			else
				sensorManager.unregisterListener(this);
		return;
		}
	}
	
	public void onClick(View view){
		switch(view.getId()){
		case R.id.connect_Remote:
			Log.d("mainActivity", "Connection to remote asked");
			Intent i = new Intent(this, TCPclientService.class);
			i.putExtra(TCPclientService.COMMAND, TCPclientService.CONNECT);
			i.putExtra(TCPclientService.PORT, port);
			i.putExtra(TCPclientService.SERVER, remoteServer);
			startService(i);
			
			/*if(!isConnected){
				
				new Connect().execute(remoteServer);
			}
			else{
				disconnect();
				if(isSending){
					sendData.setChecked(false);
					isSending=false;
				}
			}*/
			return;
		case R.id.connect_Local:
			Log.d("mainActivity", "Connection to local");
			Intent serviceIntent = new Intent(this, TCPclientService.class);
			serviceIntent.putExtra(TCPclientService.COMMAND, TCPclientService.CONNECT);
			serviceIntent.putExtra(TCPclientService.SERVER, localServer);
			serviceIntent.putExtra(TCPclientService.PORT, port);
			startService(serviceIntent);
			
			/*
			if(!isConnected){
				Log.d("mainActivity", "Connection to local asked");
				new Connect().execute(localServer);
			}
			else{
				disconnect();
				if(isSending){
					sendData.setChecked(false);
					isSending=false;
				}
			}*/
			return;
			
		case R.id.disconnect:
			Intent disconnect = new Intent(this, TCPclientService.class);
			disconnect.putExtra(TCPclientService.COMMAND, TCPclientService.DISCONNECT);
			startService(disconnect);
			return;
		case R.id.drawButton:
			Intent intent = new Intent(MainActivity.this, DrawActivity.class);
			startActivity(intent);
			return;
		
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	
	// TODO: arreglar bug raro con el timestamp y el dt que es siempre nulo! 
	@Override
	public void onSensorChanged(SensorEvent event) {
		
		if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
			float dT = 0;
			if(timestampGyro!=0){
				dT = (event.timestamp-timestampGyro)*NS2S;
			}
			timestampGyro = event.timestamp;

			showGyroValues(event.values,dT);
			
//			if(isSending){
//				String str = "G:"+code+":"+dT+":"+event.values[0]+":"+event.values[1]+":"+event.values[2]+";";
//				code++;
//				writer.println(str);
//				writer.flush();
//			}
			
		}	
	}
	
	private void showGyroValues(float[] values, float dT){
		
		float x = values[0];
		float y = values[1];
		float z = values[2];
		
		//long actualTime = System.currentTimeMillis();
		
		String gyroString = 
							"alpha: \t"+String.format("%.4f", x)+"\t\t"+
							"beta:\t"+String.format("%.4f", y)+"\n"+
							"gamma:\t"+String.format("%.4f", z)+"\t\t"+
							"timeGyro:\t"+String.format("%.4f", dT);
		gyroSensorValues.setText(gyroString);	
	}
}
