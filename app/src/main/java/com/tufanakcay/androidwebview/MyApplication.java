package com.tufanakcay.androidwebview; 
// Sesuaikan dengan package Anda

import android.app.Application;
import com.google.android.gms.ads.MobileAds;

public class MyApplication extends Application {
    private AppOpenManager appOpenManager;

    @Override
    public void onCreate() {
        super.onCreate();
        
        MobileAds.initialize(this, initializationStatus -> {
            // Inisialisasi AppOpenManager setelah AdMob diinisialisasi
            appOpenManager = new AppOpenManager(this);
        });
    }
}
