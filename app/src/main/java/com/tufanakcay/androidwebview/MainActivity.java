package com.tufanakcay.androidwebview; 

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
// 🔥 PENTING: import android.graphics.Insets; DIHAPUS

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.tufanakcay.androidwebview.R; 
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
        
        // Memastikan aplikasi menggunakan layout fullscreen
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
        
        // 5. 🔥 KOREKSI BLOK INSETS: Menggunakan FQCN AndroidX
        final FrameLayout mainLayout = findViewById(R.id.main_layout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                
                // GANTI INSETS DENGAN androidx.core.graphics.Insets
                androidx.core.graphics.Insets systemWindowInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()); 
                int topInset = systemWindowInsets.top;
                
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
    
    // ... (sisa kode tetap sama) ...
}
