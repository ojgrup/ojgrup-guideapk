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
    private int backPressCount = 0; 
    private boolean isInDetailView = false; // State Management

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuLayout = findViewById(R.id.menu_layout);
        webViewMenu = findViewById(R.id.webViewMenu); 
        webViewDetail = findViewById(R.id.webViewDetail);
        adViewTopBanner = findViewById(R.id.ad_view_top_banner);
        
        // Pastikan menuLayout terlihat saat awal
        menuLayout.setVisibility(View.VISIBLE); 

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
        
        // Memuat ulang Menu saat onCreate (Fix Layar Kosong)
        webViewMenu.loadUrl("file:///android_asset/1/index.html"); 
    }

    private void loadBannerAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        adViewTopBanner.loadAd(adRequest);
        adViewTopBanner.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                adViewTopBanner.setVisibility(View.GONE); 
            }
            @Override
            public void onAdLoaded() {
                adViewTopBanner.setVisibility(View.VISIBLE); 
            }
        });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", 
            adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) { mInterstitialAd = interstitialAd; }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) { mInterstitialAd = null; }
            });
    }

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
        // wv.addJavascriptInterface(new AdPlacer(this, wv), "AndroidAds"); 
    }
    
    private void setupWebViewDetail(WebView wv) {
        wv.setWebViewClient(new WebViewClient());
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        wv.setVisibility(View.GONE);
    }

    private void loadDetail(String url) {
        webViewDetail.loadUrl(url);
        webViewDetail.clearHistory(); 
        
        menuLayout.setVisibility(View.GONE);
        webViewDetail.setVisibility(View.VISIBLE);
        backPressCount = 0; 
        isInDetailView = true; 
    }
    
    // --- KODE TERAKHIR UNTUK TOMBOL BACK (onBackPressed) ---
    @Override
    public void onBackPressed() {
        
        // Kasus 1: Detail View
        if (isInDetailView) {
            
            // Cek Riwayat WebView internal (sangat penting untuk mencegah close)
            if (webViewDetail.canGoBack()) {
                webViewDetail.goBack();
                return; 
            }
            
            // --- Logika Counter Interstitial (Hanya berjalan jika WebView tidak bisa mundur) ---
            backPressCount++;
            Log.d("BackDebug", "Detail View (No history): Counter=" + backPressCount);

            if (backPressCount >= 2) { 
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(this);
                    backPressCount = 0; 
                    
                    // 🔥 KRITIS: Semua logika kembali ke menu harus di dalam callback Iklan
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // 1. Ganti View
                            menuLayout.setVisibility(View.VISIBLE);
                            webViewDetail.setVisibility(View.GONE);
                            // 2. Bersihkan/Muat Ulang
                            webViewDetail.clearHistory(); 
                            webViewMenu.loadUrl("file:///android_asset/1/index.html"); 
                            // 3. Set State dan Muat Ulang Iklan
                            loadInterstitialAd(); 
                            isInDetailView = false; 
                        }
                    });
                } else {
                    // Jika iklan TIDAK siap: Langsung kembali ke menu (Jalur B)
                    menuLayout.setVisibility(View.VISIBLE);
                    webViewDetail.setVisibility(View.GONE);
                    webViewDetail.clearHistory(); 
                    loadInterstitialAd(); 
                    backPressCount = 0; 
                    isInDetailView = false;
                    
                    // Muat ulang Menu Utama
                    webViewMenu.loadUrl("file:///android_asset/1/index.html"); 
                }
            } 
            
            // Konsumsi event agar tidak close (menunggu klik kedua)
            return; 
        }

        // Kasus 2: Menu Utama (Keluar)
        super.onBackPressed(); 
    }
}
