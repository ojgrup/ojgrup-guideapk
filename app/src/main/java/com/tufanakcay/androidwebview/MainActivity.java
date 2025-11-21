package com.tufanakcay.androidwebview;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import android.webkit.JavascriptInterface; // Diperlukan untuk komunikasi HTML ke Java
import android.widget.Toast; // Diperlukan untuk logika "Tekan Dua Kali"
import android.os.Handler; // Diperlukan untuk logika "Tekan Dua Kali"

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

    // --- Konstanta ---
    // GANTI ID Iklan ini dengan ID Anda setelah pengujian!
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // ID Test
    private static final String SPLASH_URL = "file:///android_asset/1/sflash.html"; 
    private static final String MENU_URL = "file:///android_asset/1/index.html"; 
    private static final int EXIT_DELAY = 2000; // Delay 2 detik untuk keluar

    // --- Deklarasi Variabel ---
    private WebView webViewMenu;
    private WebView webViewDetail;
    private WebView webViewSplash;
    private LinearLayout menuLayout;
    private AdView adViewTopBanner;

    private InterstitialAd mInterstitialAd;
    
    // Variabel untuk logika "Tekan Dua Kali untuk Keluar"
    private boolean isBackPressedOnce = false; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuLayout = findViewById(R.id.menu_layout);
        webViewMenu = findViewById(R.id.webViewMenu);
        webViewDetail = findViewById(R.id.webViewDetail);
        webViewSplash = findViewById(R.id.webViewSplash); 
        adViewTopBanner = findViewById(R.id.ad_view_top_banner);

        // 1. Inisialisasi AdMob
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Log.i("AdMob", "AdMob Initialized. Loading Ads...");
                loadBannerAd();
                loadInterstitialAd();
            }
        });

        // 2. Tampilkan Splash Screen (Mode Manual)
        setupWebViewSplash(webViewSplash, SPLASH_URL); 

        // 3. Persiapan WebView Menu dan Detail
        setupWebViewMenu(webViewMenu, MENU_URL);
        setupWebViewDetail(webViewDetail);

        // 🔥 Catatan: Handler dihilangkan agar Splash Screen menunggu tombol diklik.
    }

    // --- FUNGSI BANTUAN ---

    private void showMainMenu() {
        // Harus berjalan di UI Thread karena memanipulasi View
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webViewSplash.setVisibility(View.GONE);
                menuLayout.setVisibility(View.VISIBLE);
                Log.i("Splash", "Peralihan ke Menu Utama berhasil.");
            }
        });
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
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID,
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

    // --- LOGIKA WEBVIEW ---

    // Splash: Ditambahkan interface untuk tombol manual
    private void setupWebViewSplash(WebView wv, String url) {
        wv.setWebViewClient(new WebViewClient());
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // 🔥 TAMBAHKAN INTERFACE SPLASH UNTUK TOMBOL MANUAL
        wv.addJavascriptInterface(new SplashInterface(this), "AndroidSplash"); 
        wv.setVisibility(View.VISIBLE);
        wv.loadUrl(url);
    }
    
    // Menu: Ditambahkan kembali interface AdPlacer untuk iklan native
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
        // 🔥 PASTIKAN ADPLACER/ANDROIDADS ADA DI SINI UNTUK IKLAN NATIVE
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
    }

    // --- LOGIKA TOMBOL BACK YANG DIPERBAIKI ---

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // Kasus 0: Jika Splash Screen masih terlihat
            if (webViewSplash.getVisibility() == View.VISIBLE) {
                return true; // Mengkonsumsi tombol back
            }

            // 🔥 KASUS 1: Halaman Detail ke Menu Utama (SATU KALI TEKAN)
            if (webViewDetail.getVisibility() == View.VISIBLE) {
                
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(this);
                    
                    // Setelah Iklan ditutup, kembali ke Menu Utama
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            webViewDetail.setVisibility(View.GONE);
                            menuLayout.setVisibility(View.VISIBLE);
                            loadInterstitialAd(); 
                        }
                    });
                } else {
                    // Jika iklan tidak siap, langsung kembali ke Menu Utama
                    Log.w("AdMob", "Interstitial Ad tidak siap. Langsung kembali ke menu.");
                    webViewDetail.setVisibility(View.GONE);
                    menuLayout.setVisibility(View.VISIBLE);
                    loadInterstitialAd(); 
                }
                
                // Konsumsi event back
                return true; 
            }

            // 🔥 KASUS 2: Menu Utama (Tekan Dua Kali untuk Keluar)
            if (menuLayout.getVisibility() == View.VISIBLE) {
                
                if (isBackPressedOnce) {
                    // Jika sudah ditekan sekali, biarkan aplikasi keluar
                    return super.onKeyDown(keyCode, event); 
                }

                this.isBackPressedOnce = true;
                Toast.makeText(this, "Tekan 'Back' sekali lagi untuk keluar.", Toast.LENGTH_SHORT).show();

                // Reset status 'isBackPressedOnce' setelah 2 detik
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isBackPressedOnce = false;
                    }
                }, EXIT_DELAY); 

                return true; // Konsumsi event back yang pertama (mencegah keluar)
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    // --- KELAS JAVASCRIPT INTERFACE ---
    
    // Interface untuk memicu peralihan dari Splash HTML ke Java
    public class SplashInterface {
        MainActivity activity;

        SplashInterface(MainActivity activity) {
            this.activity = activity;
        }

        @JavascriptInterface
        public void closeSplash() {
            activity.showMainMenu();
        }
    }
    
    // Catatan: Pastikan kelas AdPlacer juga didefinisikan secara terpisah (atau sebagai inner class)
    // agar kode wv.addJavascriptInterface(new AdPlacer(this, wv), "AndroidAds"); berfungsi.
}
