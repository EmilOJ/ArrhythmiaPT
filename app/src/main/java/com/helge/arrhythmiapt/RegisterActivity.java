package com.helge.arrhythmiapt;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class RegisterActivity extends AppCompatActivity {
    static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        final EditText etName = (EditText) findViewById(R.id.etName);
        final EditText etUsername = (EditText) findViewById(R.id.etUsername);
        final EditText etPassword = (EditText) findViewById(R.id.etPassword);
        final EditText etCPR = (EditText) findViewById(R.id.etCPR);
        final Button bRegister = (Button) findViewById(R.id.bRegister);
        final ProgressBar spinner = (ProgressBar) findViewById(R.id.progressBar);

        assert etName != null;
        assert etUsername!= null;
        assert etPassword != null;
        assert etCPR != null;
        assert bRegister != null;
        assert spinner != null;



        bRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser user = new ParseUser();
                final String name = etName.getText().toString();
                final String username = etUsername.getText().toString();
                final String password = etPassword.getText().toString();
                final int CPR = Integer.parseInt(etCPR.getText().toString());

                user.setUsername(username);
                user.setPassword(password);
                user.put("name", name);
                user.put("CPR", CPR);

                spinner.setVisibility(View.VISIBLE);
                bRegister.setEnabled(false);

                user.signUpInBackground(new SignUpCallback() {
                    public void done(ParseException e) {
                        if (e == null) {
                            Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
                            loginIntent.putExtra("status", "User Created");
                            RegisterActivity.this.startActivity(loginIntent);
                        } else {
                            Log.d(TAG, e.toString());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    spinner.setVisibility(View.GONE);
                                    bRegister.setEnabled(true);
                                }
                            });
                            // Sign up didn't succeed.
                            Toast.makeText(RegisterActivity.this, "Signup failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


            }
        });
    }
}
