package com.helge.arrhythmiapt;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.IOException;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    /*
         Lets the patient login with username and password using the ParseUser object.
         Also includes a button for registering a new user.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText etUsername = (EditText) findViewById(R.id.etUsername);
        final EditText etPassword = (EditText) findViewById(R.id.etPassword);
        final Button bLogin = (Button) findViewById(R.id.bLogin);
        final TextView registerLink = (TextView) findViewById(R.id.tvRegisterHere);

        assert etUsername != null;
        assert etPassword != null;
        assert bLogin != null;
        assert registerLink != null;

        // Start register user activity
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
                LoginActivity.this.startActivity(registerIntent);
            }
        });

        // Get entered username and password and send a login request in the background.
        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();
                bLogin.setEnabled(false);

                // Login in background and start the main menu on success.
                ParseUser.logInInBackground(username, password, new LogInCallback() {
                    public void done(ParseUser user, ParseException e) {
                        if (user != null) {
                            Intent i = new Intent(LoginActivity.this, MainMenu.class);
                            startActivity(i);
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // Fetch notes from Parse server in background and save locally
        fetchNotes();

        SignalProcessing signalProcessing = new SignalProcessing(this);
        try {
            signalProcessing.readECG();
        } catch (IOException e) {
            e.printStackTrace();
        }
        signalProcessing.detect_and_classify();
    }

    // Fetch notes from database
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

    // When fetchNote() is done, a broadcast is sent such that the NotesList will be update
    private void sendDoneBroadcast() {
        Intent intent = new Intent("doneFetchingData");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


}
