package com.helge.arrhythmiapt.Notes;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.helge.arrhythmiapt.R;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by emil on 24/04/16.
 */
public class NotesAdapter extends ParseQueryAdapter<ParseObject> {

    public NotesAdapter(Context context) {
        super(context, new ParseQueryAdapter.QueryFactory<ParseObject>() {
            public ParseQuery create() {
                ParseQuery query = new ParseQuery("Note");
                return query;
            }
        });
    }

    // Customize the layout by overriding getItemView
    @Override
    public View getItemView(ParseObject object, View v, ViewGroup parent) {
        if (v == null) {
            v = View.inflate(getContext(), R.layout.note_item, null);
        }
        super.getItemView(object, v, parent);

        TextView timestampView = (TextView) v.findViewById(R.id.timestamp);

        Date timestamp = object.getCreatedAt();
        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm");
        timestampView.setText(df.format(timestamp));

        TextView titleTextView = (TextView) v.findViewById(R.id.text1);
        titleTextView.setText(object.getString("userText"));

        return v;
    }
}
