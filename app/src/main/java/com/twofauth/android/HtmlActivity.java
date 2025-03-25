package com.twofauth.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HtmlActivity extends BaseActivity {
    public static final String EXTRA_FILE_PATHNAME = "file-pathname";
    private final String INTERNAL_FILE_SCHEMA = "file";

    @Override
    protected void onCreate(@Nullable final Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        setContentView(R.layout.html_activity);
        final Intent intent = getIntent();
        if (intent == null) {
            throw new RuntimeException("Intent isn't defined!");
        }
        final String file_pathname = intent.getStringExtra(EXTRA_FILE_PATHNAME);
        if (file_pathname == null) throw new RuntimeException("File pathname hasn't been received!");
        initializeWebView(file_pathname);
    }

    private Activity getActivity() {
        return this;
    }

    private void initializeWebView(@NotNull final String url) {
        ((WebView) findViewById(R.id.web_view)).setWebChromeClient(new WebChromeClient());
        ((WebView) findViewById(R.id.web_view)).setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(@NotNull final WebView view, @NotNull final WebResourceRequest request) {
                final Uri uri = request.getUrl();
                if (! INTERNAL_FILE_SCHEMA.equals(uri.getScheme())) {
                    openInWebBrowser(getActivity(), uri);
                }
                return false;
            }
        });
        ((WebView) findViewById(R.id.web_view)).loadUrl(url);
    }

    @Override
    protected void processOnBackPressed() {
        if (((WebView) findViewById(R.id.web_view)).canGoBack()) {
            ((WebView) findViewById(R.id.web_view)).goBack();
        }
        else {
            finish();
        }
    }

    public static void openInWebBrowser(@NotNull final Activity activity, @NotNull final Uri uri) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW).setData(uri).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG_NAME, String.format("Exception trying to open Uri: %s", uri), e);
        }
    }
}
