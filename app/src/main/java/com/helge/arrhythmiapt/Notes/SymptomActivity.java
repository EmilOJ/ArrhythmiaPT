package com.helge.arrhythmiapt.Notes;

import android.app.TimePickerDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.helge.arrhythmiapt.R;

import java.util.Calendar;

public class SymptomActivity extends AppCompatActivity {

    private static TextView symptomTextView;
    private static EditText timeEditText;
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

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSymptomsId = extras.getIntArray("checkedIds");
            mCur_id = 0;
            nextSypmtom();
        }

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
        int prevSymptomId = mSymptomsId[mCur_id];
        int stringId = this.getResources().getIdentifier("symptoms" + (prevSymptomId + 1), "string", getPackageName());
        String symptomText = getResources().getString(stringId);
        symptomTextView.setText(symptomText);

        mCur_id++;
    }
}
