package com.helge.arrhythmiapt;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText etUsername = (EditText) findViewById(R.id.etUsername);
        final EditText etPassword = (EditText) findViewById(R.id.etPassword);
        final Button bLogin = (Button) findViewById(R.id.bLogin);
        final TextView registerLink = (TextView) findViewById(R.id.tvRegisterHere);
        final Button goButton = (Button) findViewById(R.id.goButton);

        assert etUsername != null;
        assert etPassword != null;
        assert bLogin != null;
        assert registerLink != null;
        assert goButton != null;

        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
                LoginActivity.this.startActivity(registerIntent);
            }
        });

        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();
                bLogin.setEnabled(false);


                ParseUser.logInInBackground(username, password, new LogInCallback() {
                    public void done(ParseUser user, ParseException e) {
                        if (user != null) {
                            // Hooray! The user is logged in.
                            Intent i = new Intent(LoginActivity.this, MainMenu.class);
                            startActivity(i);
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, MainMenu.class);
                startActivity(i);
            }
        });

//      Get intent extra and display status
        Bundle extras = getIntent().getExtras();
        String status;

        if (extras != null) {
            status = extras.getString("status");
            Toast.makeText(LoginActivity.this, status, Toast.LENGTH_SHORT).show();
        }

        // Fetch notes from Parse server in background and save locally
        fetchNotes();

    }

    private void fetchNotes() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Note");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                ParseObject.pinAllInBackground(objects, new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        sendDoneBroadcast();
                    }
                });

            }
        });
    }

    private void sendDoneBroadcast() {
        Intent intent = new Intent("doneFetchingData");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
