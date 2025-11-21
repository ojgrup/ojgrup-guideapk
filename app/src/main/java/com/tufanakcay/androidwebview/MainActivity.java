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
    private boolean isInDetailView = false; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuLayout = findViewById(R.id.menu_layout);
        webViewMenu = findViewById(R.id.webViewMenu); 
        webViewDetail = findViewById(R.id.webViewDetail);
        adViewTopBanner = findViewById(R.id.ad_view_top_banner);
        
        menuLayout.setVisibility(View.VISIBLE); 

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                loadBannerAd();
                loadInterstitialAd(); 
            }
        });

        // Setup dan Loading Menu hanya terjadi SATU KALI di onCreate
        setupWebViewMenu(webViewMenu, "file:///android_asset/1/index.html"); 
        setupWebViewDetail(webViewDetail); 
        webViewMenu.loadUrl("file:///android_asset/1/index.html"); 
    }

    // --- (Fungsi AdMob dan Setup lainnya dihilangkan untuk keringkasan, tetapi harus tetap ada) ---
    private void loadBannerAd() { /* ... */ }
    private void loadInterstitialAd() { /* ... */ }
    
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
        
        // 🔥 FIX IKLAN NATIVE: Javascript Interface harus ditambahkan di sini.
        // Asumsi kelas AdPlacer ada di package yang sama.
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
        
        if (isInDetailView) {
            
            // Cek Riwayat WebView internal 
            if (webViewDetail.canGoBack()) {
                webViewDetail.goBack();
                return; 
            }
            
            // --- Logika Counter Interstitial ---
            backPressCount++;
            
            if (backPressCount >= 2) { 
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(this);
                    backPressCount = 0; 
                    
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // 🔥 FOKUS: Hanya GANTI VIEW dan CLEAR HISTORY. Tidak ada loadUrl Menu.
                            menuLayout.setVisibility(View.VISIBLE);
                            webViewDetail.setVisibility(View.GONE);
                            webViewDetail.clearHistory(); // Membersihkan riwayat Detail
                            loadInterstitialAd(); 
                            isInDetailView = false; 
                        }
                    });
                } else {
                    // Jika iklan TIDAK siap: Langsung kembali ke menu
                    // 🔥 FOKUS: Hanya GANTI VIEW dan CLEAR HISTORY. Tidak ada loadUrl Menu.
                    menuLayout.setVisibility(View.VISIBLE);
                    webViewDetail.setVisibility(View.GONE);
                    webViewDetail.clearHistory(); 
                    loadInterstitialAd(); 
                    backPressCount = 0; 
                    isInDetailView = false;
                }
            } 
            return; // Konsumsi event
        }

        // Kasus 2: Menu Utama (Keluar)
        super.onBackPressed(); 
    }
}
