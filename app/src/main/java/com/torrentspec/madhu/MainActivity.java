package com.torrentspec.madhu;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This line tells Android to draw the XML file we just created!
        setContentView(R.layout.activity_main);
    }
}
