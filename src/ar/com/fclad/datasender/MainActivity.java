package ar.com.fclad.datasender;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
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
	
	public static final String TAG = "MainActivity";
	
	private SensorManager sensorManager;
	private boolean testingSensors;
	private float timestampGyro;
	private static final float NS2S = 1.0f / 1000000000.0f;
	
	private TextView gyroSensorValues;
	
	// Connection modes
	public static final String MODE = "mode";
	public static final int BLUETOOTH_CONNECTION_MODE = 2;
	public static final int TCP_CONNECTION_MODE = 3;
	private int connectionMode;
	
	// TCP server parameters
	private String ServerIP = "192.168.1.122";
	private int port = 7777;
	
	
	
	// Intent codes
	//private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    //private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_MODIFY_TCP_PARAMETERS = 4;
	
	// UI elements
	private ToggleButton testSensors;
	private Button connect_tcp;
	private Button connect_bluetooth;
	private Drawable connectBluetoothBackground, connectTcpBackground;
	private Button disconnect_tcp;
	private Button disconnect_bluetooth;
	private Button startDraw;
	private RadioButton mode_bluetooth,
						mode_tcp;
	
	private BroadcastReceiver receiver;

	private BluetoothAdapter bluetoothAdapter;

	
	public MainActivity() {
		setConnectionMode(BLUETOOTH_CONNECTION_MODE);
	}
	
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
		sensorInfo.append(deviceGyro.getName());	
		
		// UI
		
		connect_tcp = (Button) findViewById(R.id.connect_tcp);
		disconnect_tcp = (Button) findViewById(R.id.disconnect_tcp);
		startDraw = (Button) findViewById(R.id.drawButton);
		testSensors = (ToggleButton) findViewById(R.id.testSensors);
		connect_bluetooth = (Button) findViewById(R.id.connect_Bluetooth);
		disconnect_bluetooth = (Button) findViewById(R.id.diosconnect_Bluetooth);
		mode_tcp = (RadioButton) findViewById(R.id.tcp_mode);
		mode_bluetooth = (RadioButton) findViewById(R.id.bluetooth_mode);
		
		connectBluetoothBackground = connect_bluetooth.getBackground();
		connectTcpBackground = connect_tcp.getBackground();
				
		// BLUETOOTH
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        TextView bluetoothAddress = (TextView) findViewById(R.id.bluetoothAddress);
        bluetoothAddress.setText("Bluetooth Address:\t");
        bluetoothAddress.append(bluetoothAdapter.getAddress());
		
        // Message manage
        
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle bundle = intent.getExtras();
				
				if(bundle!=null){
					String msgTag = bundle.getString("TAG");
					if(msgTag!=null){
						if(msgTag.compareTo(TCPclientService.TAG)==0){
							switch(bundle.getInt(TCPclientService.STATUS)){
							case TCPclientService.STATE_CONNECTED:
								Log.d(TAG, "Tcp service said connected");
								connect_tcp.setBackgroundColor(Color.GREEN);
								startDraw.setEnabled(true);
								return;
							
							case TCPclientService.STATE_CONNECTING:
								Log.d(TAG, "Tcp service said connecting");
								connect_tcp.setEnabled(false);
								connect_tcp.setBackgroundColor(Color.RED);
								disconnect_tcp.setEnabled(true);
								mode_tcp.setEnabled(false);
								mode_bluetooth.setEnabled(false);
								return;
								
							case TCPclientService.STATE_NONE:
								Log.d(TAG, "Tcp service said None");
								disconnect_tcp.setEnabled(false);
								connect_tcp.setBackground(connectTcpBackground);
								connect_tcp.setEnabled(true);
								mode_tcp.setEnabled(true);
								mode_bluetooth.setEnabled(true);
								startDraw.setEnabled(false);
								connect_bluetooth.setEnabled(false);
								return;
							}
						}
						else if(msgTag.compareTo(BluetoothServerService.TAG)==0){
							switch(bundle.getInt(BluetoothServerService.STATUS)){
							case BluetoothServerService.STATE_CONNECTED:
								Log.d(TAG, "Bluetooth service said connected");
								connect_bluetooth.setBackgroundColor(Color.GREEN);
								startDraw.setEnabled(true);
								return;
							case BluetoothServerService.STATE_CONNECTING:
								Log.d(TAG, "Bluetooth service said connecting");
								return;
								
							case BluetoothServerService.STATE_LISTEN:
								Log.d(TAG, "Bluetooth service said listen");
								connect_bluetooth.setEnabled(false);
								connect_bluetooth.setBackgroundColor(Color.RED);
								disconnect_bluetooth.setEnabled(true);
								mode_tcp.setEnabled(false);
								mode_bluetooth.setEnabled(false);
								return;
								
							case BluetoothServerService.STATE_NONE:
								Log.d(TAG, "Bluetooth service said None");
								disconnect_bluetooth.setEnabled(false);
								connect_bluetooth.setBackground(connectBluetoothBackground);
								connect_bluetooth.setEnabled(true);
								mode_tcp.setEnabled(true);
								mode_bluetooth.setEnabled(true);
								startDraw.setEnabled(false);
								connect_tcp.setEnabled(false);
								//enableUIbuttons(false);
								return;
							}
						}
						else{
							Log.w(TAG, "Message from unknow service");
						}
					}	
					else{
						Log.e(TAG,"Message from service does not have TAG extra");
					}
				}
				
			}
		};
		
	}
	
	@Override
    public void onStart() {
        super.onStart();
    }
	
	@Override
	  protected void onResume() {
	    super.onResume();
	    registerReceiver(receiver, new IntentFilter(TCPclientService.NOTIFICATION));
	    registerReceiver(receiver, new IntentFilter(BluetoothServerService.NOTIFICATION));
	    
	    if(connectionMode==TCP_CONNECTION_MODE){
	    	Intent askstatusTCP = new Intent(this,TCPclientService.class);
		    askstatusTCP.putExtra(TCPclientService.COMMAND, TCPclientService.GETSTATUS);
		    startService(askstatusTCP);
	    }
	    if(connectionMode==BLUETOOTH_CONNECTION_MODE){
	    	Intent askstatusBT = new Intent(this,BluetoothServerService.class);
	 	    askstatusBT.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.GETSTATUS);
	 	    startService(askstatusBT);
	    }  
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
				// Then, enable server
				enableBluetoothServer();
			}
			else{
				Log.e(TAG, "Bluetooth enable request failed");
	            Toast.makeText(this, "Error enabling bluetooth", Toast.LENGTH_LONG).show();
	            finish();
			}
			break;
		case REQUEST_MODIFY_TCP_PARAMETERS:
			if(resultCode == RESULT_OK){
				if(data.hasExtra(ConfigureTCP.IP_ADDRESS)){
					ServerIP = data.getExtras().getString(ConfigureTCP.IP_ADDRESS);
					Log.d(TAG,"New IP "+ServerIP);
				}
				if(data.hasExtra(ConfigureTCP.PORT)){
					port = data.getExtras().getInt(ConfigureTCP.PORT);
					Log.d(TAG,"New Port "+port);
				}
			}
		default:
			break;
		}
	}
	
	public void onToggleClicked(View view){	
		switch (view.getId()){
		case R.id.testSensors:
			Log.w(TAG,"Toggled testSensor button");
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
		case R.id.connect_tcp:
			Log.d(TAG, "Connection to remote asked");
			Intent i = new Intent(this, TCPclientService.class);
			i.putExtra(TCPclientService.COMMAND, TCPclientService.CONNECT);
			i.putExtra(TCPclientService.PORT, port);
			i.putExtra(TCPclientService.SERVER, ServerIP);
			startService(i);
			if(testingSensors){
				sensorManager.unregisterListener(this);
				testSensors.setChecked(false);
			}
			return;
			
		case R.id.connect_Bluetooth:
			// If BT is not on, enable it
	        if (!bluetoothAdapter.isEnabled()) {
	            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	        	// the server will be enabled after activating bluetooth
	        }
	        else{
	        	enableBluetoothServer();
	        }
			Log.d(TAG, "Bluetooth connection asked");
			// Bluetooth server will be started onActivityResult()
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
			
		case R.id.drawButton:
			Intent intent = new Intent(MainActivity.this, DrawActivity.class);
			intent.putExtra(MODE, connectionMode);
			startActivity(intent);
			return;
		
		}
	}
	
	public void onRadioSelect(View view){
		switch(view.getId()){
		case R.id.bluetooth_mode:
	    	Intent askstatusBT = new Intent(this,BluetoothServerService.class);
	 	    askstatusBT.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.GETSTATUS);
	 	    startService(askstatusBT);
	 	    setConnectionMode(BLUETOOTH_CONNECTION_MODE);
			return;
		case R.id.tcp_mode:
	    	Intent askstatusTCP = new Intent(this,TCPclientService.class);
		    askstatusTCP.putExtra(TCPclientService.COMMAND, TCPclientService.GETSTATUS);
		    startService(askstatusTCP);
		    setConnectionMode(TCP_CONNECTION_MODE);
			return;
		
		}
		return;
		
	}

	private void enableBluetoothServer(){
		Intent bluetoothServiceIntent = new Intent(this,BluetoothServerService.class);
		bluetoothServiceIntent.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.LISTEN);
		startService(bluetoothServiceIntent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()) {
		case R.id.modify_tcp_parameters:
			Log.d(TAG,"modifying tcp parameters");
			Intent intent = new Intent(this,ConfigureTCP.class);
			startActivityForResult(intent, REQUEST_MODIFY_TCP_PARAMETERS);
			break;

		default:
			break;
		}
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
	
	private void setConnectionMode(int cm){
		Log.d(TAG,"ConnectionMode set to "+cm);
		connectionMode = cm;
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
