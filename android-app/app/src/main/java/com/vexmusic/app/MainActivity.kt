package com.vexmusic.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        setupWebView()

        // Request notification permission for Android 13+
        requestNotificationPermission()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            defaultTextEncodingName = "utf-8"
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false  // Allow autoplay
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // Add JavaScript interface for communication with Android
        webView.addJavascriptInterface(AndroidInterface(), "AndroidInterface")

        // Enable notifications for web content
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request?.grant(request.resources)
                }
            }
        }

        // Handle URL loading
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject JavaScript for notification handling
                injectNotificationScripts()
            }
        }

        // Load the HTML file
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun injectNotificationScripts() {
        webView.evaluateJavascript(
            """
            (function() {
                // Override Notification API to work with Android
                if ('Notification' in window) {
                    const originalNotification = window.Notification;
                    
                    // Create a bridge to communicate with Android
                    window.AndroidNotificationBridge = {
                        showNotification: function(title, body, icon) {
                            // This will be handled by the Android side
                        }
                    };
                    
                    // Override the Notification constructor
                    window.Notification = function(title, options) {
                        // Send notification data to Android
                        if (window.AndroidInterface) {
                            window.AndroidInterface.showNotification(
                                title || '',
                                options.body || '',
                                options.icon || ''
                            );
                        }
                        // Still create the original notification for web compatibility
                        return new originalNotification(title, options);
                    };
                    
                    // Copy static methods
                    window.Notification.requestPermission = originalNotification.requestPermission;
                    window.Notification.permission = originalNotification.permission;
                }
                
                // Override Service Worker registration to handle notifications
                if ('serviceWorker' in navigator) {
                    const originalRegister = navigator.serviceWorker.register;
                    navigator.serviceWorker.register = function(scriptURL, options) {
                        return originalRegister.call(this, scriptURL, options).then(function(registration) {
                            // Listen for push messages
                            registration.addEventListener('push', function(event) {
                                // Handle push events
                            });
                            
                            return registration;
                        });
                    };
                }
            })();
            """.trimIndent(), null
        )
    }

    // Interface for JavaScript to communicate with Android
    inner class AndroidInterface {
        @android.webkit.JavascriptInterface
        fun showNotification(title: String, body: String, icon: String) {
            val intent = Intent(this@MainActivity, NotificationService::class.java).apply {
                putExtra(NotificationService.TITLE_KEY, title)
                putExtra(NotificationService.BODY_KEY, body)
                putExtra(NotificationService.ICON_KEY, icon)
            }
            startService(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}