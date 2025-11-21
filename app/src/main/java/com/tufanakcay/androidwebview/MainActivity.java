package com.tufanakcay.androidwebview; 

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;

// Imports AdMob
import com.google.android.gms.ads.MobileAds; 
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class MainActivity extends AppCompatActivity {

    private WebView webViewMenu; 
    private WebView webViewDetail;
    private LinearLayout menuLayout;
    private AdView adViewTopBanner;
    
    private InterstitialAd mInterstitialAd; 
    private int backPressCount = 0; 
    private boolean isInDetailView = false; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuLayout = findViewById(R.id.menu_layout);
        webViewMenu = findViewById(R.id.webViewMenu); 
        webViewDetail = findViewById(R.id.webViewDetail);
        adViewTopBanner = findViewById(R.id.ad_view_top_banner);
        
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Log.i("AdMob", "AdMob Initialized. Loading Ads...");
                loadBannerAd();
                loadInterstitialAd(); 
            }
        });

        setupWebViewMenu(webViewMenu, "file:///android_asset/1/index.html"); 
        setupWebViewDetail(webViewDetail); 
    }

    // --- (Fungsi loadBannerAd dan loadInterstitialAd dihilangkan untuk keringkasan, tetapi harus tetap ada) ---
    private void loadBannerAd() { /* ... kode AdMob Banner Anda ... */ }
    private void loadInterstitialAd() { /* ... kode AdMob Interstitial Anda ... */ }
    private void setupWebViewMenu(WebView wv, String url) { /* ... kode setup WebViewMenu Anda ... */ }
    private void setupWebViewDetail(WebView wv) { /* ... kode setup WebViewDetail Anda ... */ }
    
    // --- Logika Detail View ---

    private void loadDetail(String url) {
        webViewDetail.loadUrl(url);
        // 🔥 KRITIS 1: Bersihkan riwayat internal WebView saat masuk Detail
        webViewDetail.clearHistory(); 
        
        menuLayout.setVisibility(View.GONE);
        webViewDetail.setVisibility(View.VISIBLE);
        backPressCount = 0; 
        isInDetailView = true; 
    }
    
    // --- KODE TERAKHIR UNTUK TOMBOL BACK (onBackPressed) ---
    @Override
    public void onBackPressed() {
        
        // Kasus 1: Cek status boolean (Detail View)
        if (isInDetailView) {
            
            // 🔥 Cek dan paksa WebView mundur DAHULU jika ada riwayat
            if (webViewDetail.canGoBack()) {
                Log.d("BackDebug", "WebView history detected, going back.");
                webViewDetail.goBack();
                return; 
            }
            
            // --- Logika Counter Interstitial ---
            backPressCount++;
            Log.d("BackDebug", "Detail View (No history): Counter=" + backPressCount);

            if (backPressCount >= 2) { 
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(this);
                    backPressCount = 0; 
                    
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Setelah Iklan ditutup, kembali ke menu
                            menuLayout.setVisibility(View.VISIBLE);
                            webViewDetail.setVisibility(View.GONE);
                            // 🔥 KRITIS 2: Bersihkan riwayat internal WebView saat kembali
                            webViewDetail.clearHistory(); 
                            loadInterstitialAd(); 
                            isInDetailView = false; 
                        }
                    });
                } else {
                    // Jika iklan TIDAK siap: Langsung kembali ke menu
                    menuLayout.setVisibility(View.VISIBLE);
                    webViewDetail.setVisibility(View.GONE);
                    // 🔥 KRITIS 2: Bersihkan riwayat internal WebView saat kembali
                    webViewDetail.clearHistory(); 
                    loadInterstitialAd(); 
                    backPressCount = 0; 
                    isInDetailView = false;
                }
            } 
            
            // Konsumsi event agar tidak close
            return; 
        }

        // Kasus 2: Menu Utama (isInDetailView == FALSE)
        Log.d("BackDebug", "Menu View: App Closing");
        super.onBackPressed(); 
    }
}
