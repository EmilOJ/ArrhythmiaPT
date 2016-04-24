package com.helge.arrhythmiapt.Notes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.helge.arrhythmiapt.R;
import com.parse.ParseObject;
import com.parse.ParseQueryAdapter;

public class NotesListActivity extends AppCompatActivity {

    private ParseQueryAdapter<ParseObject> mainAdapter;
    private ListView notesListView;
    private Button newNoteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);



        mainAdapter = new ParseQueryAdapter<ParseObject>(this, "Note");
        mainAdapter.setTextKey("userText");

        notesListView = (ListView) findViewById(R.id.notesListView);
        notesListView.setAdapter(mainAdapter);
        mainAdapter.loadObjects();


        newNoteButton = (Button) findViewById(R.id.addButton);
        newNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(NotesListActivity.this, NoteEditActivity.class);
                i.putExtra("new", true);
                startActivity(i);
            }
        });

        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i                = new Intent(NotesListActivity.this, NoteEditActivity.class);
                ParseObject noteObject  = mainAdapter.getItem(position);
                noteObject.pinInBackground();

                String noteID           = noteObject.getObjectId();
                i.putExtra("noteID", noteID);

                startActivity(i);
            }
        });



    }
}
