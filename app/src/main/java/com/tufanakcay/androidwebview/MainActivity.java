package com.tufanakcay.androidwebview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View; // Diperlukan untuk View.VISIBLE/GONE
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout; // Diperlukan untuk wadah iklan
// IKLAN ADMOB IMPORTS
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;


public class MainActivity extends AppCompatActivity {

    WebView webView;
    private AdView mAdView; // Objek AdMob Banner
    private FrameLayout adContainer; // Wadah AdMob di activity_main.xml

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // =================================================================
        // INISIALISASI ADMOB SDK (Wajib)
        // =================================================================
        MobileAds.initialize(this, initializationStatus -> {
            // Callback dipanggil saat SDK siap.
        });
        // =================================================================

        init();
        viewUrl();
        loadBannerAd(); // Panggil metode untuk memuat iklan
    }

    private void init() {
        // Asumsi: webView ada di R.id.webView
        webView = findViewById(R.id.webView); 
        
        // Asumsi: Wadah AdMob ada di R.id.ad_container dan R.id.ad_view (dari layout XML)
        mAdView = findViewById(R.id.ad_view);
        adContainer = findViewById(R.id.ad_container);
    }

    private void viewUrl() {
        
        // =================================================================
        // KODE KRITIS: MEMUAT KONTEN DARI FOLDER ASSETS
        // =================================================================
        String localAssetUrl = "file:///android_asset/index.html"; 
        
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); 
        webSettings.setCacheMode(WebSettings.LOAD_NORMAL);
        webSettings.setBuiltInZoomControls(false); 
        webSettings.setDisplayZoomControls(false);
        
        webView.setWebViewClient(new CustomWebViewClient()); 

        webView.loadUrl(localAssetUrl);
    }
    
    // =================================================================
    // LOGIKA ADMOB BANNER
    // =================================================================
    private void loadBannerAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Iklan berhasil dimuat, tampilkan wadah iklan Native (Android Layout)
                adContainer.setVisibility(View.VISIBLE); 
                // Suntikkan ruang kosong ke HTML agar konten tepat di bawah iklan Native
                injectAdPlaceholder(); 
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Iklan gagal dimuat, sembunyikan wadah
                adContainer.setVisibility(View.GONE);
                // Penting: Hapus placeholder HTML agar tidak ada ruang kosong yang mengganggu
                removeAdPlaceholder(); 
            }
        });
    }

    // Menyuntikkan padding ke placeholder HTML agar sebanding dengan tinggi iklan Native
    private void injectAdPlaceholder() {
        // Tinggi iklan banner standar adalah sekitar 50dp. Kita gunakan ini sebagai perkiraan.
        final int adHeightDp = 50; 
        float density = getResources().getDisplayMetrics().density;
        // Ubah DP ke Pixel dan tambahkan margin bawah 15px (seperti gap kartu)
        int adHeightPx = (int) (adHeightDp * density); 
        
        // JavaScript untuk menemukan ID="admob_placeholder" dan memberikannya tinggi
        String jsCode = "javascript:" +
            "var placeholder = document.getElementById('admob_placeholder');" +
            "if (placeholder) {" +
            "   placeholder.style.height = '" + adHeightPx + "px';" + // Beri tinggi sesuai iklan Native
            "   placeholder.style.marginBottom = '20px';" + // Sesuai dengan gap kartu di CSS Anda
            "}";
        
        // Eksekusi JavaScript
        webView.loadUrl(jsCode);
    }
    
    // Menghapus ruang kosong jika iklan gagal dimuat
    private void removeAdPlaceholder() {
        String jsCode = "javascript:" +
            "var placeholder = document.getElementById('admob_placeholder');" +
            "if (placeholder) {" +
            "   placeholder.style.display = 'none';" +
            "}";
        webView.loadUrl(jsCode);
    }
    
    // =================================================================
    
    
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("file:///android_asset/") || url.startsWith("http") || url.startsWith("https")) {
                 view.loadUrl(url);
                 return true; 
            }
            return false;
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
             super.onPageFinished(view, url);
             // Panggil injectAdPlaceholder() lagi jika halaman berganti (misalnya dari detail kembali ke index)
             if (url.contains("index.html") && adContainer.getVisibility() == View.VISIBLE) {
                 injectAdPlaceholder();
             }
        }
    }
    
    // =================================================================
    // PENANGANAN TOMBOL KEMBALI FISIK
    // =================================================================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Jika tombol back ditekan DAN WebView bisa kembali ke halaman sebelumnya
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true; // Tangani event back
        }
        // Biarkan sistem menangani (misalnya keluar dari aplikasi)
        return super.onKeyDown(keyCode, event);
    }
}
