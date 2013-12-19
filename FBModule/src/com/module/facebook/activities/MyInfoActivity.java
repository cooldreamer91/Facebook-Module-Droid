package com.module.facebook.activities;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.facebook.Response;
import com.facebook.model.GraphUser;
import com.module.facebook.FacebookActivity;
import com.module.facebook.R;

public class MyInfoActivity extends FacebookActivity implements OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_myinfo);

		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				requestMe();
			}
		}, 100);
	}

	@Override
	protected void onRequestMeCompleted(GraphUser user, Response response) {
		// TODO Auto-generated method stub
		super.onRequestMeCompleted(user, response);

		if (user != null) {
			Log.i("FBModule", "My First Name = " + user.getFirstName());
			Log.i("FBModule", "My Email = " + user.getProperty("email"));

			String sFullName = user.getFirstName() + "" + user.getLastName();
			String sEmail = (String)user.getProperty("email");
			String sBirthday = user.getBirthday();
			
			((TextView) findViewById(R.id.myinfo_tv_name)).setText(sFullName);
			((TextView) findViewById(R.id.myinfo_tv_email)).setText(sEmail);
			((TextView) findViewById(R.id.myinfo_tv_birthday)).setText(sBirthday);
		}
		Log.i("FBModule", "Response = " + response);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.myinfo_btn_friends_list:
			
			break;

		default:
			break;
		}
	}
}
