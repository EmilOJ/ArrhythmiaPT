package com.helge.arrhythmiapt.Notes;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.helge.arrhythmiapt.MainMenu;
import com.helge.arrhythmiapt.R;

import java.util.Calendar;

public class SymptomActivity extends AppCompatActivity {
    /*
        This activity is displayed for when symptoms are selected in SymptomsActivity. It includes
        a time selector and a text field. If more symptoms are selected in SymptomsActivity,
        the fields are reset when pressing continue until all symptoms have received input.
        In this version the entered information is not stored anywhere.
     */

    private static TextView symptomTextView;
    private static EditText timeEditText;
    private static EditText detailsEditText;
    private static Button continueButton;
    private static int mCur_id = 0;
    private static int[] mSymptomsId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom);

        symptomTextView = (TextView) findViewById(R.id.symptomText);
        timeEditText = (EditText) findViewById(R.id.timeEditText);
        continueButton = (Button) findViewById(R.id.symptomContinueButton);
        detailsEditText = (EditText) findViewById(R.id.editText2);

        // Receives the list of selected symptoms from SymptomsActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSymptomsId = extras.getIntArray("checkedIds");
            mCur_id = 0;
            nextSypmtom();
        }

        // Shows time selector popup
        timeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(SymptomActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        timeEditText.setText( selectedHour + ":" + selectedMinute);
                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextSypmtom();
            }
        });

    }

    private void nextSypmtom() {
        /*
            Clears both input fields and shows the next symptom in the headline.
         */
        if (mCur_id == mSymptomsId.length) {
            Intent i = new Intent(this, MainMenu.class);
            startActivity(i);
            Toast.makeText(SymptomActivity.this, "Thank you for your response", Toast.LENGTH_SHORT).show();
        } else {
            int prevSymptomId = mSymptomsId[mCur_id];
            int stringId = this.getResources().getIdentifier("symptoms" + (prevSymptomId + 1), "string", getPackageName());
            String symptomText = getResources().getString(stringId);
            symptomTextView.setText(symptomText);

            timeEditText.setText("");
            detailsEditText.setText("");

            mCur_id++;
        }

    }
}
