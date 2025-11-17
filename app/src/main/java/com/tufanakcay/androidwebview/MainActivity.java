package com.tufanakcay.androidwebview; 

// ... (Import yang sudah ada)
import android.widget.ScrollView;
import android.widget.FrameLayout;
import com.google.android.gms.ads.adloader.AdLoader;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private WebView webView1, webView2, webView3;
    private FrameLayout nativeAdPlaceholder1, nativeAdPlaceholder2, nativeAdPlaceholder3; // Tambah placeholder 3
    
    // ... Variabel AdMob lainnya
    private static final String NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ... (Pengaturan Status Bar)
        
        setContentView(R.layout.activity_main);

        // 1. Inisialisasi AdMob
        MobileAds.initialize(this, initializationStatus -> {
            loadInterstitialAd(); 
            loadAppOpenAd(); 
        });

        // 2. Inisialisasi Views
        // ... (adViewTopBanner)
        
        webView1 = findViewById(R.id.webView1);
        webView2 = findViewById(R.id.webView2);
        webView3 = findViewById(R.id.webView3);
        
        nativeAdPlaceholder1 = findViewById(R.id.native_ad_placeholder_1);
        nativeAdPlaceholder2 = findViewById(R.id.native_ad_placeholder_2);
        nativeAdPlaceholder3 = findViewById(R.id.native_ad_placeholder_3); // Inisialisasi placeholder 3

        // 3. Muat Iklan Banner
        loadTopBannerAd();

        // 4. Konfigurasi WebView dan Muat Fragment HTML
        // Ganti "splash.html" dengan file fragment yang sebenarnya
        setupWebView(webView1, "file:///android_asset/fragment_1.html");
        setupWebView(webView2, "file:///android_asset/fragment_2.html");
        setupWebView(webView3, "file:///android_asset/fragment_3.html"); 
        
        // 5. Muat 3 Iklan Native ke Placeholder Asli
        loadNativeAd(nativeAdPlaceholder1);
        loadNativeAd(nativeAdPlaceholder2);
        loadNativeAd(nativeAdPlaceholder3); // Muat iklan ke placeholder 3
        
        // Catatan: Jika Anda masih menggunakan splash.html, Anda harus mengganti logika
        // loadMainContent di WebAppInterface untuk memuat WebView fragment di sini.
    }
    
    private void setupWebView(WebView wv, String url) {
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // Penting: Memastikan WebView mengambil tinggi konten untuk tata letak yang benar
        wv.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZE); 
        wv.setWebViewClient(new WebViewClient());
        wv.loadUrl(url);
    }
    
    // =======================================================
    // FUNGSI IKLAN NATIVE (Menggunakan View Asli)
    // =======================================================

    private void loadNativeAd(final FrameLayout placeholder) {
        AdLoader adLoader = new AdLoader.Builder(this, NATIVE_AD_UNIT_ID)
            .forNativeAd(nativeAd -> {
                displayNativeAd(nativeAd, placeholder);
            })
            .withAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    Log.e("NativeAd", "Native Ad failed to load: " + adError.getMessage());
                    placeholder.setVisibility(View.GONE);
                }
            })
            .build();
            
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void displayNativeAd(NativeAd nativeAd, FrameLayout placeholder) {
        // PERHATIAN: Anda HARUS MEMBUAT FILE XML native_ad_template.xml 
        // yang berisi NativeAdView dan elemen-elemennya.
        
        // 1. Inflate layout Native Ad View Anda
        // GANTI R.layout.native_ad_template DENGAN XML ANDA
        NativeAdView adView = (NativeAdView) LayoutInflater.from(this)
            .inflate(R.layout.native_ad_template, null); 
            
        // 2. Isi data ke dalam adView (Ini hanya contoh penamaan ID)
        try {
            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
            adView.setIconView(adView.findViewById(R.id.ad_app_icon));
            
            ((TextView)adView.getHeadlineView()).setText(nativeAd.getHeadline());
            if (nativeAd.getBody() == null) {
                adView.getBodyView().setVisibility(View.INVISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            }

            if (nativeAd.getCallToAction() == null) {
                adView.getCallToActionView().setVisibility(View.INVISIBLE);
            } else {
                adView.getCallToActionView().setVisibility(View.VISIBLE);
                ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            }

            if (nativeAd.getIcon() == null) {
                adView.getIconView().setVisibility(View.GONE);
            } else {
                ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }
            
            // Daftarkan NativeAd ke NativeAdView
            adView.setNativeAd(nativeAd);
            
            // 3. Tambahkan View Iklan Native ke dalam FrameLayout placeholder
            placeholder.removeAllViews();
            placeholder.addView(adView);
            placeholder.setVisibility(View.VISIBLE);
            
        } catch (Exception e) {
            Log.e("NativeAd", "Gagal mengisi data iklan native: " + e.getMessage());
            placeholder.setVisibility(View.GONE);
        }
    }
    
    // ... (Fungsi loadInterstitialAd, loadAppOpenAd, onKeyDown tetap sama)
}
