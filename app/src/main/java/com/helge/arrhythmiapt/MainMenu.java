package com.helge.arrhythmiapt;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.helge.arrhythmiapt.Notes.NotesListActivity;
import com.helge.arrhythmiapt.Notes.SymptomsActivity;

import java.io.IOException;

public class MainMenu extends AppCompatActivity {
    /*
        Main menu which shows a record button and a notes button.
        It also shows patient information.
     */

    private static Button notesButton;
    private static Button recordButton;

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


        // Proxy button for displaying questions. This should in reality be implemented
        // as an AlarmManager so the questions will be displayed every 8 hours.
        recordButton = (Button) findViewById(R.id.questionButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainMenu.this)
                        .setIcon(android.R.drawable.ic_menu_add)
                        .setTitle("Time for questions")
                        .setMessage("Have you felt any symptoms since last time?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(MainMenu.this, SymptomsActivity.class);
                                startActivity(i);
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });


        // Hardcoded patient information. This should in reality be implemented as a query
        // to the database.

        String patientName = "Lone Hansen";
        String patientCPR = "111100-1111";
        // Name of patient
        TextView textView6 = (TextView) findViewById(R.id.name);
        textView6.setText(patientName);

        // cpr-number
        TextView textView7 = (TextView) findViewById(R.id.cpr);
        textView7.setText(patientCPR);

        // Medicin that patient takes
        TextView textView8 = (TextView) findViewById(R.id.med1);
        textView8.setText("ChlorTrimeton");
        //textView8.setText(patientMedicin);
        TextView textView9 = (TextView) findViewById(R.id.med2);
        textView9.setText("(allergy and hay fever)");
        TextView textView10 = (TextView) findViewById(R.id.med3);
        textView10.setText("Ventolin");
        //textView8.setText(patientMedicin);
        TextView textView11 = (TextView) findViewById(R.id.med4);
        textView11.setText("(asthma and obstructive pulmonary disease)");


        // Simulation of recording the signal. This creates an instance of the signal
        // processing class which reads a signal from a local csv file and run the
        // detection algorithm.

    }
}
