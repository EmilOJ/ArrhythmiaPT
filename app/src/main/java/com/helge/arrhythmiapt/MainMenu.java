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
    private static Button notesButton;
    private static Button qButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        String patientName = "Lone Hansen";
        String patientCPR = "";
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


        notesButton = (Button) findViewById(R.id.notesButton);

        notesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainMenu.this, NotesListActivity.class);
                startActivity(i);
            }
        });

        qButton = (Button) findViewById(R.id.questionButton);
        qButton.setOnClickListener(new View.OnClickListener() {
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



//        SignalProcessing signalProcessing = new SignalProcessing(this);
//        try {
//            signalProcessing.readECG();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        signalProcessing.detect_and_classify();
    }
}
