package com.jfdedit3.x

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jfdedit3.x.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val allowedHosts = setOf(
        "x.com",
        "www.x.com",
        "twitter.com",
        "www.twitter.com",
        "mobile.twitter.com",
        "t.co"
    )

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = filePathCallback
        if (callback == null) return@registerForActivityResult

        val results = when {
            result.resultCode != Activity.RESULT_OK -> null
            result.data == null -> null
            result.data?.clipData != null -> {
                val clipData = result.data?.clipData ?: return@registerForActivityResult
                Array(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
            }
            result.data?.data != null -> arrayOf(result.data!!.data!!)
            else -> WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        }

        callback.onReceiveValue(results)
        filePathCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enterFullscreen()
        setupIntro()

        with(binding.webView) {
            overScrollMode = View.OVER_SCROLL_NEVER
            alpha = 0f
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = settings.userAgentString + " XAndroidWrapper/1.3"

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = filePathCallback

                    return try {
                        val intent = fileChooserParams?.createIntent()?.apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                            if (type.isNullOrBlank() || type == "*/*") {
                                type = "*/*"
                                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                            }
                        } ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                        }

                        fileChooserLauncher.launch(Intent.createChooser(intent, getString(R.string.file_chooser_title)))
                        true
                    } catch (_: ActivityNotFoundException) {
                        this@MainActivity.filePathCallback = null
                        false
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (binding.introOverlay.visibility == View.VISIBLE) {
                        playIntroExit()
                    } else {
                        binding.webView.animate().alpha(1f).setDuration(220).start()
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    return if (isAllowed(uri)) {
                        false
                    } else {
                        openExternal(uri)
                        true
                    }
                }
            }
        }

        binding.swipeRefresh.setProgressViewOffset(false, 120, 220)
        binding.swipeRefresh.setColorSchemeResources(android.R.color.white)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(android.R.color.black)
        binding.swipeRefresh.setOnRefreshListener {
            binding.webView.reload()
        }

        binding.webView.setDownloadListener { url, _, _, _, _ ->
            openExternal(Uri.parse(url))
        }

        if (savedInstanceState == null) {
            binding.webView.loadUrl("https://x.com")
        } else {
            binding.webView.restoreState(savedInstanceState)
            binding.webView.alpha = 1f
            binding.introOverlay.visibility = View.GONE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterFullscreen()
    }

    override fun onResume() {
        super.onResume()
        binding.swipeRefresh.isRefreshing = false
        enterFullscreen()
    }

    override fun onDestroy() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    private fun setupIntro() {
        binding.introOverlay.alpha = 1f
        binding.introLogo.alpha = 0f
        binding.introLogo.scaleX = 0.72f
        binding.introLogo.scaleY = 0.72f
        binding.introLogo.translationY = 36f
        binding.introSubtitle.alpha = 0f
        binding.introSubtitle.translationY = 24f

        binding.introLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setStartDelay(120)
            .setDuration(650)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.introSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(300)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun playIntroExit() {
        binding.webView.animate()
            .alpha(1f)
            .setDuration(260)
            .start()

        binding.introContent.animate()
            .alpha(0f)
            .scaleX(1.06f)
            .scaleY(1.06f)
            .setDuration(280)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.introOverlay.animate()
            .alpha(0f)
            .setStartDelay(120)
            .setDuration(360)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.introOverlay.visibility = View.GONE
                    binding.introOverlay.alpha = 1f
                    binding.introContent.alpha = 1f
                    binding.introContent.scaleX = 1f
                    binding.introContent.scaleY = 1f
                }
            })
            .start()
    }

    private fun enterFullscreen() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun isAllowed(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return allowedHosts.any { host == it || host.endsWith(".$it") }
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
        }
    }
}
