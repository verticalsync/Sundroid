package me.verticalsync.sundroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.*;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class VWebviewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        var url = request.getUrl();
        if ("discord.com".equals(url.getAuthority()) || "about:blank".equals(url.toString())) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, url);
        view.getContext().startActivity(intent);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        view.evaluateJavascript(HttpClient.VencordRuntime, null);
        view.evaluateJavascript(HttpClient.VencordMobileRuntime, null);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        view.setVisibility(View.VISIBLE);
        super.onPageFinished(view, url);
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
        var uri = req.getUrl();
        if (req.isForMainFrame() || req.getUrl().getPath().endsWith(".css")) {
            try {
                return doFetch(req);
            } catch (IOException ex) {
                Logger.e("Error during shouldInterceptRequest", ex);
            }
        }
        return null;
    }

    private WebResourceResponse doFetch(WebResourceRequest req) throws IOException {
        var url = req.getUrl().toString();
        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(req.getMethod());
        for (var h : req.getRequestHeaders().entrySet()) {
            conn.setRequestProperty(h.getKey(), h.getValue());
        }

        var code = conn.getResponseCode();
        var msg = conn.getResponseMessage();

        var headers = conn.getHeaderFields();
        var modifiedHeaders = new HashMap<String, String>(headers.size());
        for (var header : headers.entrySet()) {
            if (!"Content-Security-Policy".equalsIgnoreCase(header.getKey())) {
                modifiedHeaders.put(header.getKey(), header.getValue().get(0));
            }
        }
        if (url.endsWith(".css")) modifiedHeaders.put("Content-Type", "text/css");

        return new WebResourceResponse(modifiedHeaders.getOrDefault("Content-Type", "application/octet-stream"), "utf-8", code, msg, modifiedHeaders, conn.getInputStream());
    }
}
