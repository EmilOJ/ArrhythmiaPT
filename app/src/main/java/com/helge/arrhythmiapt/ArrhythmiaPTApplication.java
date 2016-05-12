package com.helge.arrhythmiapt;

import com.helge.arrhythmiapt.Models.Arrhythmia;
import com.helge.arrhythmiapt.Models.ECGRecording;
import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Created by emil on 24/04/16.
 */
public class ArrhythmiaPTApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initializes the database and registers Arrhythmia and ECGRecording as ParseObjects
        ParseObject.registerSubclass(Arrhythmia.class);
        ParseObject.registerSubclass(ECGRecording.class);
        Parse.enableLocalDatastore(getApplicationContext());
        Parse.initialize(this, "7wop8ncD1stVBTcbPaCgDa1ZRHeiwkZulGYuV0nE", "RY7qjP9NJDwV81dEh3ZHVrMfy3ZU7qP9fKJ9QZOQ");
    }
}
