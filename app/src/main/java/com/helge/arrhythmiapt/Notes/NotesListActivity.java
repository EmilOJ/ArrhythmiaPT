package com.helge.arrhythmiapt.Notes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.helge.arrhythmiapt.R;
import com.parse.ParseObject;
import com.parse.ParseQueryAdapter;

public class NotesListActivity extends AppCompatActivity {
    /*
        ListView displaying all Notes in the database for the current user. Uses the NotesAdapter
        to load and display the data. Also starts the NoteEditActivity when tapping either of
        the notes on the list.
        Includes a broadcastreceiver for updating the list when notes are synced from database.
     */

    private ParseQueryAdapter<ParseObject> notesAdapter;
    private ListView notesListView;
    private Button newNoteButton;
    private BroadcastReceiver mFetchtDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            notesAdapter.loadObjects();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);


        notesAdapter = new NotesAdapter(this);

        notesListView = (ListView) findViewById(R.id.notesListView);
        notesListView.setAdapter(notesAdapter);
        notesAdapter.loadObjects();

        //Set the ListAdapter which retrives and processes the data to be displayed
        newNoteButton = (Button) findViewById(R.id.addButton);
        newNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(NotesListActivity.this, NoteEditActivity.class);
                i.putExtra("new", true); // add "new" as extra which is recognized in editActivity
                startActivity(i);
            }
        });


        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(NotesListActivity.this, NoteEditActivity.class);
                ParseObject noteObject = notesAdapter.getItem(position);
                noteObject.pinInBackground();

                String noteID = noteObject.getObjectId();
                i.putExtra("noteID", noteID);

                startActivity(i);
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(mFetchtDataReceiver, new IntentFilter("doneFetchingData"));


    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mFetchtDataReceiver);
        super.onDestroy();
    }
}
