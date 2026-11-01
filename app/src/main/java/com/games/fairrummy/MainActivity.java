
package com.games.fairrummy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private static final String START_URL =
            "https://onlineexamvzm25.github.io/fairgamesind/";

    private static final int MICROPHONE_PERMISSION_CODE = 1001;

    private static final String TRUSTED_HOST =
            "onlineexamvzm25.github.io";

    private WebView webView;

    private PermissionRequest pendingPermissionRequest;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(0, 43, 41));
        getWindow().setNavigationBarColor(Color.rgb(0, 43, 41));

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(0, 43, 41));

        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);


        // ==========================================
        // WEBVIEW PERMISSION HANDLING
        // ==========================================

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(
                    PermissionRequest request
            ) {

                runOnUiThread(() -> {

                    // Allow microphone only from our trusted website.
                    Uri origin = request.getOrigin();

                    boolean trustedOrigin =
                            origin != null &&
                            "https".equalsIgnoreCase(origin.getScheme()) &&
                            TRUSTED_HOST.equalsIgnoreCase(origin.getHost());

                    if (!trustedOrigin) {
                        request.deny();
                        return;
                    }

                    boolean microphoneRequested = false;

                    for (String resource : request.getResources()) {

                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                            microphoneRequested = true;
                            break;
                        }
                    }

                    // Do not grant camera or other resources.
                    if (!microphoneRequested) {
                        request.deny();
                        return;
                    }

                    // Android runtime permission already granted?
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {

                        grantMicrophonePermission(request);
                        return;
                    }

                    // Save the WebView request while Android asks the user.
                    if (pendingPermissionRequest != null) {
                        pendingPermissionRequest.deny();
                    }

                    pendingPermissionRequest = request;

                    requestPermissions(
                            new String[]{
                                    Manifest.permission.RECORD_AUDIO
                            },
                            MICROPHONE_PERMISSION_CODE
                    );
                });
            }

            @Override
            public void onPermissionRequestCanceled(
                    PermissionRequest request
            ) {
                if (pendingPermissionRequest == request) {
                    pendingPermissionRequest = null;
                }
            }
        });


        // ==========================================
        // EXISTING WEBVIEW NAVIGATION
        // ==========================================

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {

                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });


        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }


    // ==========================================
    // GRANT MICROPHONE TO WEBVIEW
    // ==========================================

    private void grantMicrophonePermission(
            PermissionRequest request
    ) {

        try {
            request.grant(
                    new String[]{
                            PermissionRequest.RESOURCE_AUDIO_CAPTURE
                    }
            );

        } catch (Exception e) {
            android.util.Log.e(
                    "FairRummy",
                    "Unable to grant WebView microphone permission",
                    e
            );
        }
    }


    // ==========================================
    // ANDROID RUNTIME PERMISSION RESULT
    // ==========================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode != MICROPHONE_PERMISSION_CODE) {
            return;
        }

        PermissionRequest request = pendingPermissionRequest;
        pendingPermissionRequest = null;

        if (request == null) {
            return;
        }

        if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            grantMicrophonePermission(request);

        } else {

            request.deny();

            android.util.Log.w(
                    "FairRummy",
                    "Android microphone permission denied"
            );
        }
    }


    // ==========================================
    // EXISTING BACK BUTTON
    // ==========================================

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    // ==========================================
    // EXISTING STATE SAVE
    // ==========================================

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        if (webView != null) {
            webView.saveState(outState);
        }

        super.onSaveInstanceState(outState);
    }


    // ==========================================
    // CLEANUP
    // ==========================================

    @Override
    protected void onDestroy() {

        if (pendingPermissionRequest != null) {
            pendingPermissionRequest.deny();
            pendingPermissionRequest = null;
        }

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
