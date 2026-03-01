package com.droidbert

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var loadingIndicator: View

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        loadingIndicator = findViewById(R.id.loading_indicator)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                webView.visibility = View.INVISIBLE
                loadingIndicator.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                removeSearchBar()
                webView.visibility = View.VISIBLE
                loadingIndicator.visibility = View.GONE
            }
        }

        if (savedInstanceState == null) {
            webView.loadUrl("https://dilbert.xo.nl/")
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    private fun removeSearchBar() {
        webView.evaluateJavascript(
            """
            (function() {
                const styleId = 'droidbert-hide-search';
                if (!document.getElementById(styleId)) {
                    const style = document.createElement('style');
                    style.id = styleId;
                    style.textContent = `
                        header form[role="search"],
                        header input[type="search"],
                        header .search,
                        header .search-form,
                        header .search-container,
                        nav form[role="search"],
                        nav input[type="search"],
                        nav .search,
                        nav .search-form,
                        nav .search-container,
                        [aria-label="Search"] {
                            display: none !important;
                        }
                    `;
                    document.head.appendChild(style);
                }
            })();
            """.trimIndent(),
            null
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
