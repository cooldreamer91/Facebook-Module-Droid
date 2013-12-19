package com.module.facebook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.facebook.SessionState;
import com.module.facebook.FacebookActivity;
import com.module.facebook.R;

public class LoginActivity extends FacebookActivity implements OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_login);

		getHashKey(this);

		Button btnLogin = (Button) findViewById(R.id.main_btn_fb_login);
		btnLogin.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onSessionStateChange(SessionState state, Exception exception) {
		// TODO Auto-generated method stub
		super.onSessionStateChange(state, exception);

		if (state.isOpened()) {
			Log.i("FBModule", "Session is opended.");
			startActivity(new Intent(getBaseContext(), MyInfoActivity.class));
			finish();
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.main_btn_fb_login:
			openSession();

			break;
		default:
			break;
		}
	}
}
