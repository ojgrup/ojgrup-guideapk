package com.tufanakcay.androidwebview; 
// PASTIKAN PACKAGE INI SESUAI DENGAN YANG ANDA GUNAKAN

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
import com.google.android.gms.ads.AdListener;
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
    private int backPressCount = 0; // Penghitung Interstitial (2 kali back)
    
    // 🔥 VARIABEL STATUS BOOLEAN (Pengganti cek visibility yang tidak stabil)
    private boolean isInDetailView = false; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuLayout = findViewById(R.id.menu_layout);
        webViewMenu = findViewById(R.id.webViewMenu); 
        webViewDetail = findViewById(R.id.webViewDetail);
        adViewTopBanner = findViewById(R.id.ad_view_top_banner);
        
        // 1. Inisialisasi MobileAds
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

    private void loadBannerAd() {
        adViewTopBanner.loadAd(new AdRequest.Builder().build());
        adViewTopBanner.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e("AdMob", "Banner GAGAL dimuat. Error: " + loadAdError.getMessage());
                adViewTopBanner.setVisibility(View.GONE); 
            }
            @Override
            public void onAdLoaded() {
                Log.i("AdMob", "Banner BERHASIL dimuat.");
                adViewTopBanner.setVisibility(View.VISIBLE); 
            }
        });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", 
            adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    Log.i("AdMob", "Interstitial Ad loaded.");
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                    Log.e("AdMob", "Interstitial Ad failed to load: " + loadAdError.getMessage());
                }
            });
    }

    // --- Logika WebView ---

    private void setupWebViewMenu(WebView wv, String url) {
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("myapp://")) {
                    String detailUrl = url.replace("myapp://", "file:///android_asset/2/");
                    loadDetail(detailUrl + ".html");
                    return true;
                }
                return false;
            }
        });
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // Pastikan AdPlacer.java ada di package yang sama
        wv.addJavascriptInterface(new AdPlacer(this, wv), "AndroidAds"); 
        wv.loadUrl(url);
    }
    
    private void setupWebViewDetail(WebView wv) {
        wv.setWebViewClient(new WebViewClient());
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        wv.setVisibility(View.GONE);
    }

    private void loadDetail(String url) {
        webViewDetail.loadUrl(url);
        menuLayout.setVisibility(View.GONE);
        webViewDetail.setVisibility(View.VISIBLE);
        backPressCount = 0; 
        // 🔥 Set status Detail View = TRUE saat masuk
        isInDetailView = true; 
    }
    
    // --- KODE TERAKHIR UNTUK TOMBOL BACK (onBackPressed) ---
    @Override
    public void onBackPressed() {
        
        // Kasus 1: Cek status boolean (Detail View)
        if (isInDetailView) {
            
            // 🔥 RIWAYAT WEBVIEW: Paksa mundur jika ada riwayat internal WebView (Mencegah close paksa)
            if (webViewDetail.canGoBack()) {
                Log.d("BackDebug", "WebView history detected, going back.");
                webViewDetail.goBack();
                return; // KONSUMSI: Biarkan WebView mundur
            }
            
            // --- Logika Counter Interstitial (Hanya berjalan jika WebView tidak bisa mundur) ---
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
                            loadInterstitialAd(); 
                            // 🔥 Set status Detail View = FALSE setelah kembali ke menu
                            isInDetailView = false; 
                        }
                    });
                } else {
                    // Jika iklan TIDAK siap: Langsung kembali ke menu
                    menuLayout.setVisibility(View.VISIBLE);
                    webViewDetail.setVisibility(View.GONE);
                    loadInterstitialAd(); 
                    backPressCount = 0; 
                    // 🔥 Set status Detail View = FALSE setelah kembali ke menu
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
