package ar.com.fclad.datasender;

import java.util.regex.Pattern;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ConfigureTCPActivity extends Activity {
	
	public static final String TAG = "ConfigureTCP";
	
	
	// http://stackoverflow.com/questions/3698034/validating-ip-in-android/11545229#11545229
	// Pattern to verify correct IPv4 address
	private static final Pattern PARTIAl_IP_ADDRESS =
	          Pattern.compile("^((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])\\.){0,3}"+
	                           "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])){0,1}$"); 
	
	// Messages sent by this activity
	public static final String PORT = "port";
	public static final String IP_ADDRESS = "ip_address";
	
	// REFACTOR ALL!
	private String ipAddressString = null;
	private int portInt = 7777;
	
	// UI references.
	private EditText port;
	private EditText ipAddress;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_configure_tcp);

		// UI
		port = (EditText) findViewById(R.id.tcp_port);
		ipAddress = (EditText) findViewById(R.id.ip_address);
	}
	
	public void onClickListener(View view){
		switch (view.getId()) {
		case R.id.ok_button:
			// Verify correct IPv4 number in ipAddress
			Editable ipEditable = ipAddress.getEditableText();
			if(PARTIAl_IP_ADDRESS.matcher(ipEditable).matches()){
				ipAddressString = ipEditable.toString();
			}
			else{
				Toast.makeText(getApplicationContext(), "Error on IP address", Toast.LENGTH_SHORT).show();
				return;
			
			}
			// Verify correct port number (1024<)
			Editable portEditable = port.getEditableText();
			int value;
			try {
				value = Integer.parseInt(portEditable.toString());
			} catch (NumberFormatException e) {
				value = 7777; 
			}
			if(value>1024){
				portInt = value;
			}
			else{
				Toast.makeText(getApplicationContext(), "Error on port", Toast.LENGTH_SHORT).show();
				return;
			}
			Intent intent = new Intent();
			intent.putExtra(IP_ADDRESS, ipAddressString);
			intent.putExtra(PORT, portInt);
			setResult(RESULT_OK,intent);
			finish();
			return;
		case R.id.cancel_button:
			setResult(RESULT_CANCELED);
			finish();
			break;
		default:
			break;
		}
	}
}
