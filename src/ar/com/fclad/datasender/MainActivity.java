package ar.com.fclad.datasender;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
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
	
	private static final String TAG = "MainActivity";
	
	private SensorManager sensorManager;
	private boolean testingSensors;
	private float timestampGyro;
	private static final float NS2S = 1.0f / 1000000000.0f;
	
	private TextView gyroSensorValues;
	
	// Server parameters
	private static final String localServer = "10.0.0.1";
	private static final String remoteServer = "192.168.2.122";
	private int port = 7777;
	
	// Intent codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
	
	// UI elements
	private ToggleButton testSensors;
	private Button connectRemote;
	private Button connectLocal;
	private Button connect_bluetooth;
	private Button disconnect_tcp;
	private Button disconnect_bluetooth;
	private Button startDraw;
	
	private BroadcastReceiver receiver;

	private BluetoothAdapter bluetoothAdapter;
	private BluetoothServerService bluetoothServerService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
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
		disconnect_tcp = (Button) findViewById(R.id.disconnect_tcp);
		startDraw = (Button) findViewById(R.id.drawButton);
		testSensors = (ToggleButton) findViewById(R.id.testSensors);
		connect_bluetooth = (Button) findViewById(R.id.connect_Bluetooth);
		disconnect_bluetooth = (Button) findViewById(R.id.diosconnect_Bluetooth);
		
		// BLUETOOTH
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
		
        // TCP connection
        
		receiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle bundle = intent.getExtras();
				if(bundle!=null){
					switch(bundle.getInt(TCPclientService.STATUS)){
					case TCPclientService.CONNECTED:
						Log.d("MainActivity", "Service said connected");
						enableUIbuttons(true);
						return;
					
					case TCPclientService.DISCONNECT:
						Log.d("MainActivity", "Service said disconnected");
						enableUIbuttons(false);
						return;
					}
				}
				
			}
		};
		
	}
	
	@Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (bluetoothServerService == null) enableBTservice();        
        }
    }
	
	@Override
	  protected void onResume() {
	    super.onResume();
	    registerReceiver(receiver, new IntentFilter(TCPclientService.NOTIFICATION));
	    registerReceiver(receiver, new IntentFilter(BluetoothServerService.NOTIFICATION));
	    
	    Intent askstatus = new Intent(this,TCPclientService.class);
	    askstatus.putExtra(TCPclientService.COMMAND, TCPclientService.GETSTATUS);
	    startService(askstatus);
	}

	@Override
	protected void onPause() {
		// unregister listener
		super.onPause();
		
		unregisterReceiver(receiver);
		if(testingSensors)
			sensorManager.unregisterListener(this);
	    
	}
	
	
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		switch (reqCode) {
		case REQUEST_ENABLE_BT:
			if(resultCode == Activity.RESULT_OK){
				enableBTservice();
			}
			else{
				Log.e(TAG, "Bluetooth enable request failed");
	            Toast.makeText(this, "Error enabling bluetooth", Toast.LENGTH_LONG).show();
	            finish();
			}
			break;

		default:
			break;
		}
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
			if(testingSensors){
				sensorManager.unregisterListener(this);
				testSensors.setChecked(false);
			}
			return;
			
		case R.id.connect_Local:
			Log.d("mainActivity", "Connection to local");
			Intent serviceIntent = new Intent(this, TCPclientService.class);
			serviceIntent.putExtra(TCPclientService.COMMAND, TCPclientService.CONNECT);
			serviceIntent.putExtra(TCPclientService.SERVER, localServer);
			serviceIntent.putExtra(TCPclientService.PORT, port);
			startService(serviceIntent);
			if(testingSensors){
				sensorManager.unregisterListener(this);
				testSensors.setChecked(false);
			}
			return;
			
		case R.id.connect_Bluetooth:
			Log.d(TAG, "Bluetooth connection asked");
			Intent bluetoothServiceIntent = new Intent(this,BluetoothServerService.class);
			bluetoothServiceIntent.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.LISTEN);
			startService(bluetoothServiceIntent);
			enableUIbuttons(true);
			return;
		
		case R.id.diosconnect_Bluetooth:
			Log.d(TAG, "Bluetooth disconnect asked");
			Intent disconnect_bluetooth = new Intent(this,BluetoothServerService.class);
			disconnect_bluetooth.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.STOP_LISTEN);
			startService(disconnect_bluetooth);
			return;
			
		case R.id.disconnect_tcp:
			Intent disconnect = new Intent(this, TCPclientService.class);
			disconnect.putExtra(TCPclientService.COMMAND, TCPclientService.DISCONNECT);
			startService(disconnect);
			return;
		case R.id.sendHello:
			Log.d(TAG, "Sent Hello by BT");
			Intent sendmsg = new Intent(this,BluetoothServerService.class);
			sendmsg.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.SENDMSG);
			sendmsg.putExtra(BluetoothServerService.ORIGIN, "TST");
			sendmsg.putExtra(BluetoothServerService.MSG, "Hello Bluetooth!\n");
			startService(sendmsg);
			return;
		case R.id.drawButton:
			Intent intent = new Intent(MainActivity.this, DrawActivity.class);
			startActivity(intent);
			return;
		
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
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
			timestampGyro = event.timestamp;

			showGyroValues(event.values,dT);
		}	
	}
	
	private void enableUIbuttons(boolean isConnected){
		if(isConnected){
			connectLocal.setEnabled(false);
			connectRemote.setEnabled(false);
			startDraw.setEnabled(true);
			disconnect_tcp.setEnabled(true);
			testSensors.setEnabled(false);
		}
		else{
			connectLocal.setEnabled(true);
			connectRemote.setEnabled(true);
			startDraw.setEnabled(false);
			disconnect_tcp.setEnabled(false);
			testSensors.setEnabled(true);	
		}
	}
	
	private void enableBTservice(){
		
	}
	
	private void showGyroValues(float[] values, float dT){
		
		float x = values[0];
		float y = values[1];
		float z = values[2];
		
		String gyroString = 
							"alpha: \t"+String.format("%.4f", x)+"\t\t"+
							"beta:\t"+String.format("%.4f", y)+"\n"+
							"gamma:\t"+String.format("%.4f", z)+"\t\t"+
							"timeGyro:\t"+String.format("%.4f", dT);
		gyroSensorValues.setText(gyroString);	
	}


}
