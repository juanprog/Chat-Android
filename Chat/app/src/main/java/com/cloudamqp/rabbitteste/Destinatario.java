package com.cloudamqp.rabbitteste;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by juan on 29/04/17.
 */

public class Destinatario extends Activity
{
    /** The username edittext. */
    private EditText dest;

    private Button btnOk;

    private String userName;

    protected void onCreate(Bundle savedInstanceState)	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.destinatario);

        Intent it = getIntent();
        userName = it.getStringExtra("nomeUser");

        dest = (EditText) findViewById(R.id.dest);
        btnOk = (Button) findViewById(R.id.btnOk);

        // Get a support ActionBar corresponding to this toolbar
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void onClick(View v) {
        if(dest.getText().length()==0)
            Toast.makeText(getApplication(), "O campo de usuario eh obrigatorio!", Toast.LENGTH_LONG).show();
        else {
            Intent it = new Intent(Destinatario.this, Chat.class);
            it.putExtra("nomeDest", dest.getText().toString());
            it.putExtra("nomeUser", userName);
            startActivity(it);
            dest.setText("");
            //finish();
        }
    }
}
