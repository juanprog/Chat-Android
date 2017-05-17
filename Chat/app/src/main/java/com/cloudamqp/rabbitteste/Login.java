package com.cloudamqp.rabbitteste;

import android.app.Activity;
import android.app.Notification;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The Class Login is an Activity class that shows the login screen to users.
 * The current implementation simply includes the options for Login and button
 * for Register. On login button click, it sends the Login details to Parse
 * server to verify user.
 */
public class Login extends Activity
{
	/** The username edittext. */
	private EditText user;

	private Button btnLogin;

	protected void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		user = (EditText) findViewById(R.id.user);
		btnLogin = (Button) findViewById(R.id.btnLogin);
	}

	public void onClick(View v) {
		if(user.getText().length()==0)
			Toast.makeText(getApplication(), "O campo de usuario e obrigatorio!", Toast.LENGTH_LONG).show();
		else {
			Toast.makeText(getApplication(), "Seja bem vindo " + user.getText().toString() + "!"
					, Toast.LENGTH_LONG).show();
			Intent it = new Intent(Login.this, Destinatario.class);
			it.putExtra("nomeUser", user.getText().toString());
			startActivity(it);
			user.setText("");
			//finish();
		}
	}
}
