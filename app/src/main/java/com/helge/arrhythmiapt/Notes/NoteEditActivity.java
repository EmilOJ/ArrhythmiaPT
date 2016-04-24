package com.helge.arrhythmiapt.Notes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.helge.arrhythmiapt.R;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class NoteEditActivity extends AppCompatActivity {
    private static EditText bodyEditText;
    private static Button confirmButton;
    private ParseObject noteObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        bodyEditText = (EditText) findViewById(R.id.bodyEditText);
        confirmButton = (Button) findViewById(R.id.confirmButton);

        Bundle extras = getIntent().getExtras();
        boolean newNote = false;
        String noteID = "";
        if (extras != null) {
            newNote = extras.getBoolean("new");
            noteID = extras.getString("noteID");
        }


        if (newNote) {
            noteObject = new ParseObject("Note");
        } else {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Note");
            query.fromLocalDatastore();
            query.getInBackground(noteID, new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject object, ParseException e) {
                    if (e == null) {
                        noteObject = object;
                        bodyEditText.setText(noteObject.getString("userText"));
                    } else {
                        Toast.makeText(NoteEditActivity.this, "Could not load note", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }




        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String editedText = bodyEditText.getText().toString();
                if (editedText != noteObject.getString("userText")) {
                    noteObject.put("userText", editedText);
                    noteObject.saveEventually();
                }
                Intent i = new Intent(NoteEditActivity.this, NotesListActivity.class);
                startActivity(i);
            }
        });






    }
}
