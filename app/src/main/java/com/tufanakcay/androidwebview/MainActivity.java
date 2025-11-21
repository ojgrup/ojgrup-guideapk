package com.tufanakcay.androidwebview;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler; 
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import android.widget.Toast; // Diperlukan untuk logika "Tekan Dua Kali"

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
    // Hapus penggunaan backPressCount, kecuali jika kamu masih ingin menggunakannya untuk logika lain.
    // private int backPressCount = 0; 
    
    // 🔥 VARIABEL BARU UNTUK LOGIKA BACK DI MENU UTAMA
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

        // 2. Tampilkan Splash Screen (Otomatis 3 detik)
        setupWebViewSplash(webViewSplash, SPLASH_URL); 

        // 3. Persiapan WebView Menu dan Detail
        setupWebViewMenu(webViewMenu, MENU_URL);
        setupWebViewDetail(webViewDetail);

        // 4. Handler untuk menyembunyikan Splash (Logika Otomatis)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Sembunyikan Splash setelah durasi yang ditentukan
                webViewSplash.setVisibility(View.GONE);
                // Tampilkan Menu
                menuLayout.setVisibility(View.VISIBLE);
            }
        }, 3000); 
    }

    // --- loadBannerAd dan loadInterstitialAd (Tidak Berubah) ---

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

    // --- Logika WebView (Tidak Berubah) ---

    private void setupWebViewSplash(WebView wv, String url) {
        wv.setWebViewClient(new WebViewClient());
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        wv.setVisibility(View.VISIBLE);
        wv.loadUrl(url);
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
        // Pastikan AdPlacer (jika kamu menggunakannya) masih ada
        // wv.addJavascriptInterface(new AdPlacer(this, wv), "AndroidAds"); 
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
        // Hapus backPressCount = 0; karena backPressCount tidak digunakan di detail
    }

    // --- Logika Tombol Back YANG DIPERBAIKI ---

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // Kasus 0: Jika Splash Screen masih terlihat
            if (webViewSplash.getVisibility() == View.VISIBLE) {
                return true; // Mengkonsumsi tombol back
            }

            // 🔥 KASUS 1: Kita berada di Halaman Detail (webViewDetail)
            if (webViewDetail.getVisibility() == View.VISIBLE) {
                
                // Cek apakah Iklan Interstisial sudah siap
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(this);
                    
                    // Atur callback: Setelah Iklan ditutup, BARU kembali ke Menu Utama
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Setelah Iklan ditutup, kembali ke menu
                            webViewDetail.setVisibility(View.GONE);
                            menuLayout.setVisibility(View.VISIBLE);
                            loadInterstitialAd(); // Load iklan baru
                        }
                    });
                } else {
                    // Jika iklan tidak siap/null, langsung kembali ke Menu Utama
                    Log.w("AdMob", "Interstitial Ad tidak siap. Langsung kembali ke menu.");
                    webViewDetail.setVisibility(View.GONE);
                    menuLayout.setVisibility(View.VISIBLE);
                    loadInterstitialAd(); // Tetap load iklan baru
                }
                
                // Konsumsi event back: SATU KALI TEKAN dari detail selalu kembali ke menu
                return true; 
            }

            // 🔥 KASUS 2: Kita berada di Menu Utama (menuLayout)
            if (menuLayout.getVisibility() == View.VISIBLE) {
                
                if (isBackPressedOnce) {
                    // Jika sudah ditekan sekali dalam 2 detik, biarkan aplikasi keluar
                    return super.onKeyDown(keyCode, event); 
                }

                this.isBackPressedOnce = true;
                // Tampilkan pesan kepada pengguna
                Toast.makeText(this, "Tekan 'Back' sekali lagi untuk keluar.", Toast.LENGTH_SHORT).show();

                // Reset status 'isBackPressedOnce' setelah jeda waktu (2 detik)
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
    
    // Jika kamu punya AdPlacer atau SplashInterface, pastikan mereka didefinisikan di sini
}
