package com.honeywell.rfidsimpleexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.honeywell.rfidsimpleexample.fragments.settings.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();
    }
}