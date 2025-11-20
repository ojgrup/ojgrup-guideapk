package com.tufanakcay.androidwebview;
// Sesuaikan dengan package Anda

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

public class AdPlacer {
    private final Activity activity;
    private final WebView webView;
    private final Context context;

    public AdPlacer(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.context = activity.getApplicationContext();
    }

    @JavascriptInterface
    public void requestNativeAd(final String placeholderId) {
        activity.runOnUiThread(() -> {
            // ID Unit Iklan Native (Ganti dengan ID Asli Anda)
            String adUnitId = "ca-app-pub-3940256099942544/2247696110"; 

            AdLoader adLoader = new AdLoader.Builder(context, adUnitId)
                .forNativeAd(nativeAd -> {
                    // Ketika iklan berhasil dimuat, inflate dan konversi ke HTML
                    String adHtml = inflateNativeAdToHtmlString(nativeAd);
                    
                    // Suntikkan HTML Iklan ke placeholder di WebView
                    String js = "javascript:document.getElementById('" + placeholderId + "_web').innerHTML = '" + 
                                escapeJavaScript(adHtml) + "';"; 
                    webView.evaluateJavascript(js, null);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e("AdPlacer", "Ad failed to load: " + adError.getMessage());
                        // Sembunyikan placeholder jika iklan gagal dimuat
                        webView.evaluateJavascript("javascript:document.getElementById('" + placeholderId + "_web').style.display='none';", null);
                    }
                })
                .build();

            adLoader.loadAd(new AdRequest.Builder().build());
        });
    }

    private String escapeJavaScript(String html) {
        // Mengubah karakter khusus agar aman di string JavaScript
        return html.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "");
    }

    private String inflateNativeAdToHtmlString(NativeAd nativeAd) {
        // --- INI ADALAH TUGAS SANGAT KOMPLEKS ---
        // Anda harus membuat String HTML yang akan menggambar ulang iklan native.
        
        // UNTUK CONTOH INI, kita akan menggunakan HTML sederhana dan teks untuk demonstrasi.
        // Dalam implementasi nyata, Anda perlu menyertakan gambar, ikon, dan styling AdChoices.
        
        String headline = nativeAd.getHeadline() != null ? nativeAd.getHeadline() : "Iklan Unggulan";
        String body = nativeAd.getBody() != null ? nativeAd.getBody() : "Lihat penawaran menarik ini!";
        String callToAction = nativeAd.getCallToAction() != null ? nativeAd.getCallToAction() : "KLIK DI SINI";

        // Pastikan HTML ini memiliki styling yang sama dengan kartu Anda (misal padding 0 20px)
        return "<a href='#' onclick=\"window.location.href='" + callToAction + "';\" style='text-decoration: none; color: inherit; display: block;'>" +
               "<div style='background-color: #fff; border-radius: 12px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); padding: 15px 25px; display: flex; align-items: center; margin-bottom: 20px;'>" + 
               "<div style='flex-grow: 1;'>" + 
               "<div style='font-weight: bold; font-size: 1.2em; color: #000; margin-bottom: 5px;'>" + headline + "</div>" +
               "<div style='font-size: 0.9em; color: #555;'>" + body + "</div>" +
               "</div>" +
               "<span style='background-color: #4CAF50; color: white; padding: 5px 10px; border-radius: 5px; font-weight: bold; margin-left: 10px;'>" + callToAction + "</span>" +
               "</div>" +
               "</a>";
    }
}
