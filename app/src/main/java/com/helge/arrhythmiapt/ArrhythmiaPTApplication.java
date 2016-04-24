package com.helge.arrhythmiapt;

import com.parse.Parse;

/**
 * Created by emil on 24/04/16.
 */
public class ArrhythmiaPTApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Parse.enableLocalDatastore(getApplicationContext());
        Parse.initialize(this, "7wop8ncD1stVBTcbPaCgDa1ZRHeiwkZulGYuV0nE", "RY7qjP9NJDwV81dEh3ZHVrMfy3ZU7qP9fKJ9QZOQ");
    }
}
