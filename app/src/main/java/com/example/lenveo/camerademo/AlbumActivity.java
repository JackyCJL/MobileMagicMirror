package com.example.lenveo.camerademo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;

public class AlbumActivity extends AppCompatActivity {
    ListView listView;
    ImageView progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        listView = (ListView) findViewById(R.id.local_album_list);
        progress = (ImageView) findViewById(R.id.progress_bar);
    }
}
