package com.example.webviewapp; // Ganti dengan package name Anda

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private InterstitialAd mInterstitialAd;
    private int backPressCount = 0;
    private static final int AD_SHOW_THRESHOLD = 2; 
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // TEST ID

    private FrameLayout[] adContainers = new FrameLayout[3];
    private AdView[] adViews = new AdView[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Memastikan aplikasi menggunakan layout fullscreen untuk menghitung insets dengan benar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        
        setContentView(R.layout.activity_main);

        // 1. Inisialisasi WebView
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        
        // 2. Muat URL AWAL (Splash Screen)
        webView.setWebViewClient(new WebViewClient());
        // MEMUAT SPLASH.HTML SAAT START
        webView.loadUrl("file:///android_asset/splash.html"); 

        // 3. Inisialisasi AdMob
        MobileAds.initialize(this, initializationStatus -> {
            Log.d("AdMob", "AdMob initialized successfully.");
            loadInterstitialAd(); 
        });

        // 4. Inisialisasi Ad Native Containers
        adContainers[0] = findViewById(R.id.ad_container_inline_1);
        adContainers[1] = findViewById(R.id.ad_container_inline_2);
        adContainers[2] = findViewById(R.id.ad_container_inline_3);

        adViews[0] = findViewById(R.id.ad_view_inline_1);
        adViews[1] = findViewById(R.id.ad_view_inline_2);
        adViews[2] = findViewById(R.id.ad_view_inline_3);
        
        // 5. KOREKSI: Penyesuaian Insets untuk Memposisikan Konten di bawah Status Bar
        final FrameLayout mainLayout = findViewById(R.id.main_layout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                WindowInsetsCompat systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                
                int topInset = systemInsets.top;
                
                // Terapkan padding atas pada WebView sama dengan tinggi Status Bar
                webView.setPadding(
                    webView.getPaddingLeft(), 
                    topInset, 
                    webView.getPaddingRight(), 
                    webView.getPaddingBottom()
                );
                
                return insets;
            });
        }
    }
    
    // =======================================================
    // JAVA INTERFACE & NAVIGATION LOGIC
    // =======================================================
    public class WebAppInterface {
        
        // Dipanggil dari splash.html
        @JavascriptInterface
        public void loadMainContent() {
            webView.post(() -> {
                // Memuat index.html (Daftar Kartu Konten)
                webView.loadUrl("file:///android_asset/index.html");
                
                // Iklan dimuat di sini setelah index.html dimuat
                loadAllInlineAds();
            });
        }
        
        // Dipanggil setelah index.html dimuat
        @JavascriptInterface
        public void loadAllInlineAds() {
            for (int i = 0; i < 3; i++) {
                loadInlineAd(adViews[i], adContainers[i], i + 1);
            }
        }

        @JavascriptInterface
        public void setAdPosition(int adIndex, int yOffset) {
            webView.post(() -> {
                if (adIndex >= 1 && adIndex <= 3) {
                    FrameLayout targetContainer = adContainers[adIndex - 1];
                    if (targetContainer != null) {
                        if (targetContainer.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) targetContainer.getLayoutParams();
                            params.topMargin = yOffset;
                            targetContainer.setLayoutParams(params);
                            targetContainer.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
    }

    private void loadInlineAd(AdView adView, FrameLayout adContainer, int adIndex) {
        if (adView == null || adContainer == null) {
            Log.e("AdMob", "AdView or Container for index " + adIndex + " is null.");
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Iklan berhasil dimuat, panggil JS untuk pemosisian
                String jsCode = "javascript:(function(){" +
                    "  var p = document.getElementById('native_ad_placeholder_" + adIndex + "');" +
                    "  if(p && p.offsetParent !== null) {" + 
                    "    var rect = p.getBoundingClientRect();" +
                    "    var y = rect.top + window.scrollY;" +
                    "    Android.setAdPosition(" + adIndex + ", Math.round(y));" + 
                    "  }" +
                    "})()";
                webView.post(() -> webView.loadUrl(jsCode));
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                adContainer.setVisibility(View.GONE); 
                Log.e("AdMob", "Inline Ad " + adIndex + " failed to load: " + loadAdError.getMessage());
            }
        });

        adView.loadAd(adRequest);
    }
    
    // =======================================================
    // IKLAN INTERSTITIAL & BACK BUTTON LOGIC
    // =======================================================
    
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    Log.i("AdMob", "Interstitial Ad was loaded.");
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.d("AdMob", "Interstitial Ad failed to load: " + loadAdError.getMessage());
                    mInterstitialAd = null;
                }
            });
    }

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
