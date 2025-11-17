package com.tufanakcay.androidwebview; 

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Build;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager; 
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout; // Ganti LinearLayout menjadi FrameLayout jika Anda menggunakan Native Ad
import android.widget.LinearLayout; 

import com.tufanakcay.androidwebview.R; 
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd; 
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

// Native Ad Imports
import com.google.android.gms.ads.adloader.AdLoader;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Button;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private InterstitialAd mInterstitialAd;
    private int backPressCount = 0;
    private static final int AD_SHOW_THRESHOLD = 2; 
    
    // ID IKLAN TEST ADMOB
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; 
    private static final String APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"; 
    // 🔥 ID NATIVE AD (TEST)
    private static final String NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"; 

    private AdView adViewTopBanner; 
    private AppOpenAd appOpenAd = null; 
    private boolean isAdShown = false; 
    
    // 🔥 Peta untuk menyimpan iklan native yang sudah dimuat
    private Map<Integer, NativeAd> loadedNativeAds = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // PENGATURAN STATUS BAR SOLID (Putih)
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS); 
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); 
            
            window.setStatusBarColor(Color.WHITE); 
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        
        setContentView(R.layout.activity_main);

        // 1. Inisialisasi WebView
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android"); 
        
        webView.setWebViewClient(new WebViewClient());
        
        // 2. Inisialisasi AdMob
        MobileAds.initialize(this, initializationStatus -> {
            Log.d("AdMob", "AdMob initialized successfully.");
            loadInterstitialAd(); 
            
            if (!isAdShown) {
                loadAppOpenAd();
            }
        });
        
        // 3. Muat Iklan Banner Tetap Atas
        loadTopBannerAd();

        // 4. Muat URL AWAL
        webView.loadUrl("file:///android_asset/splash.html"); 
        
        // 5. PENANGANAN INSETS BAWAH
        final LinearLayout mainLayout = findViewById(R.id.main_layout);
        if (mainLayout != null) {
            mainLayout.setFitsSystemWindows(false); 
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemWindowInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()); 
                int bottomInset = systemWindowInsets.bottom;
                
                // Terapkan padding HANYA PADA BAGIAN BAWAH WebView
                if (webView.getPaddingBottom() != bottomInset) {
                    webView.setPadding(
                        webView.getPaddingLeft(), 
                        0, // Atas di-reset ke 0 (karena Status Bar Solid)
                        webView.getPaddingRight(), 
                        bottomInset // Padding Bawah (Navigation Bar)
                    );
                }
                return insets; 
            });
        }
    }
    
    // =======================================================
    // FUNGSI IKLAN UTAMA (App Open, Interstitial, Top Banner)
    // =======================================================

    private void loadTopBannerAd() {
        adViewTopBanner = findViewById(R.id.ad_view_top_banner);
        AdRequest adRequest = new AdRequest.Builder().build();
        adViewTopBanner.loadAd(adRequest);
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                }
            });
    }

    private void loadAppOpenAd() {
        if (appOpenAd != null) return;

        AppOpenAd.load(
            this,
            APP_OPEN_AD_UNIT_ID,
            new AdRequest.Builder().build(),
            AppOpenAd.ORIENTATION_PORTRAIT,
            new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    appOpenAd = ad;
                    showAppOpenAdIfReady();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    appOpenAd = null;
                }
            }
        );
    }
    
    private void showAppOpenAdIfReady() {
        if (appOpenAd != null && !isAdShown) {
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    appOpenAd = null;
                    isAdShown = true; 
                    loadAppOpenAd(); 
                }
            });
            appOpenAd.show(this);
        }
    }
    
    // =======================================================
    // 🔥 FUNGSI IKLAN NATIVE (INLINE)
    // =======================================================

    // Memuat iklan Native
    private void loadNativeAd(int adId) {
        AdLoader adLoader = new AdLoader.Builder(this, NATIVE_AD_UNIT_ID)
            .forNativeAd(nativeAd -> {
                Log.d("NativeAd", "Native Ad " + adId + " loaded successfully.");
                loadedNativeAds.put(adId, nativeAd);
                
                // Setelah iklan dimuat, tampilkan di WebView
                showNativeAd(adId, nativeAd);
            })
            .withAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    Log.e("NativeAd", "Native Ad " + adId + " failed to load: " + adError.getMessage());
                }
            })
            .build();
            
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    // Menampilkan iklan Native ke dalam WebView
    private void showNativeAd(int adId, NativeAd nativeAd) {
        runOnUiThread(() -> {
            // Kita akan menggunakan HTML/JS untuk menyuntikkan iklan Native
            // WebView akan mencari element dengan ID 'native_ad_placeholder_' + adId
            
            // Inflate template iklan native
            LayoutInflater inflater = getLayoutInflater();
            // Anda harus membuat layout XML untuk native ad, contohnya 'native_ad_template.xml'
            // Karena tidak ada, kita akan menggunakan cara paling sederhana: FrameLayout berisi TextView (hanya demonstrasi)
            
            // 🔥 Karena menyuntikkan View secara langsung ke WebView itu kompleks, 
            // kita akan menggunakan JavasScript untuk menandai bahwa iklan siap.
            
            // Ini adalah cara paling umum: render iklan native di Android (luar WebView)
            // lalu beri tahu WebView agar mengalokasikan ruang (height) untuknya.
            
            String adHtml = getNativeAdHtml(nativeAd, adId);
            
            // Suntikkan HTML Iklan Native ke dalam placeholder
            String script = "javascript:document.getElementById('native_ad_placeholder_" + adId + "').innerHTML = '" + adHtml + "';";
            
            webView.evaluateJavascript(script, null);
            
            Log.d("NativeAd", "Native Ad " + adId + " injected into HTML.");
        });
    }
    
    // Fungsi sederhana untuk membuat tampilan HTML iklan native (Anda harus menyesuaikannya)
    private String getNativeAdHtml(NativeAd nativeAd, int adId) {
        // PERHATIAN: Ini adalah placeholder. Implementasi iklan native sesungguhnya
        // memerlukan layout XML terpisah (misal native_ad_template.xml) dan
        // harus di-inflate dan di-populate di sisi Java, BUKAN di HTML.
        
        // Karena kita tidak memiliki XML Native Ad Template, kita hanya mengirimkan HTML sederhana.
        // Iklan ini mungkin tidak bisa diklik. Harap ganti dengan implementasi native ad yang benar.
        
        String headline = nativeAd.getHeadline() != null ? nativeAd.getHeadline() : "Iklan Uji Coba";
        String callToAction = nativeAd.getCallToAction() != null ? nativeAd.getCallToAction() : "KLIK";
        
        return "<div style='background-color:#fff; padding:15px; border-radius:12px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
               "<div style='font-weight:bold; color:#007bff; margin-bottom: 5px; font-size: 14px;'>AD</div>" +
               "<div style='font-weight:bold; font-size:16px;'>" + headline + "</div>" +
               "<button style='background-color:#4CAF50; color:white; padding:8px 12px; border:none; border-radius:6px; margin-top:10px;'>" + callToAction + "</button>" +
               "</div>";
    }

    // =======================================================
    // JAVA INTERFACE (Disederhanakan untuk memuat halaman)
    // =======================================================
    public class WebAppInterface {
        
        @JavascriptInterface
        public void loadMainContent() {
            webView.post(() -> {
                webView.loadUrl("file:///android_asset/index.html");
            });
        }
        
        // 🔥 Dipanggil oleh HTML setelah index.html dimuat
        @JavascriptInterface
        public void loadInlineAd(int adId) {
            Log.d("WebAppInterface", "Requesting Native Ad for ID: " + adId);
            loadNativeAd(adId);
        }
    }
    
    // =======================================================
    // BACK BUTTON
    // =======================================================

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (webView.canGoBack()) {
                backPressCount++;
                if (backPressCount >= AD_SHOW_THRESHOLD && mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            webView.goBack();
                            loadInterstitialAd(); 
                        }
                    });
                    backPressCount = 0;
                    return true;
                } else {
                    webView.goBack();
                    return true;
                }
            } else {
                finish(); 
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
