package ar.com.fclad.datasender;


import android.app.Activity;

import android.os.Bundle;

public class DrawActivity extends Activity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(new DrawView(this));
	}
}
