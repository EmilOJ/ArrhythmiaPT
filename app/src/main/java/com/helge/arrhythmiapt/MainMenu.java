package com.helge.arrhythmiapt;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.helge.arrhythmiapt.Notes.NotesListActivity;

public class MainMenu extends AppCompatActivity {
    private static Button notesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        notesButton = (Button) findViewById(R.id.notesButton);

        notesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainMenu.this, NotesListActivity.class);
                startActivity(i);
            }
        });

        SignalProcessing signalProcessing = new SignalProcessing(this);


    }
}
