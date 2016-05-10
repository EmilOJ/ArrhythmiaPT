package com.helge.arrhythmiapt.Notes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import com.google.common.primitives.Doubles;
import com.helge.arrhythmiapt.R;

import java.util.ArrayList;
import java.util.List;

public class SymptomsActivity extends AppCompatActivity {
    private static Button continueButton;
    private static RadioGroup radioGroup;
    List<CheckBox> checkBoxes = new ArrayList<>();


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptoms_main);



        continueButton = (Button) findViewById(R.id.continueButton);
        radioGroup = (RadioGroup) findViewById(R.id.symptomsRadioGroup);


        for (int i = 1; i <= radioGroup.getChildCount(); i++) {
            String idString = "checkBox" + i;
            int id = getResources().getIdentifier(idString, "id", getPackageName());
            checkBoxes.add((CheckBox) findViewById(id));
        }

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Integer> checkedIdList = new ArrayList<Integer>();
                for (int i = 0; i < checkBoxes.size(); i++) {
                    if (checkBoxes.get(i).isChecked()) {
                        checkedIdList.add(i);
                    }
                }
                if (checkedIdList.size() > 0) {
                    Intent i = new Intent(SymptomsActivity.this, SymptomActivity.class);
                    int[] checkedId = new int[checkedIdList.size()];
                    for (int ii = 0; ii < checkedIdList.size(); ii++) {
                        checkedId[ii] =checkedIdList.get(ii);
                    }

                    i.putExtra("checkedIds", checkedId);
                    startActivity(i);
                } else {
                    finish();
                }

            }
        });








    }
}
