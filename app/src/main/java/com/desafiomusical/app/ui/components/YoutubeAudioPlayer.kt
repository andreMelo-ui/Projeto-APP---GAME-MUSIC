package com.desafiomusical.app.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Reproduz apenas o áudio de um vídeo do YouTube via IFrame Player API oficial
 * dentro de um WebView — nunca extração de stream. O WebView fica com tamanho
 * quase zero (1dp) porque o jogo não deve exibir nada do vídeo (miniatura,
 * título), só o som: a mesma regra de privacidade que já mascara texto em
 * [com.desafiomusical.app.domain.model.MaskedSong] vale pro player. Precisa
 * ficar sempre anexado e nunca `View.GONE`, senão a Page Visibility API do
 * WebView pausa o vídeo sozinha.
 */
private class YoutubeErrorBridge(private val onError: () -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun reportError(code: Int) {
        mainHandler.post { onError() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeAudioPlayer(
    videoId: String?,
    isPlaying: Boolean,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnError by rememberUpdatedState(onError)
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            addJavascriptInterface(YoutubeErrorBridge { currentOnError() }, "AndroidBridge")
        }
    }

    var loadedVideoId by remember { mutableStateOf<String?>(null) }

    AndroidView(factory = { webView }, modifier = modifier.size(1.dp))

    LaunchedEffect(videoId) {
        if (videoId != null && videoId != loadedVideoId) {
            loadedVideoId = videoId
            webView.loadDataWithBaseURL(
                PLAYER_ORIGIN,
                buildPlayerHtml(videoId),
                "text/html",
                "utf-8",
                null
            )
        } else if (videoId == null && loadedVideoId != null) {
            loadedVideoId = null
            webView.loadUrl("about:blank")
        }
    }

    LaunchedEffect(isPlaying, loadedVideoId) {
        if (loadedVideoId != null) {
            val call = if (isPlaying) "playVideo();" else "pauseVideo();"
            webView.evaluateJavascript("try { $call } catch (e) {}", null)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webView.evaluateJavascript("try { pauseVideo(); } catch (e) {}", null)
                    webView.onPause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webView.onResume()
                    if (currentIsPlaying) {
                        webView.evaluateJavascript("try { playVideo(); } catch (e) {}", null)
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }
}

/**
 * Origem "fake" usada como baseURL do WebView — precisa ser qualquer https
 * que NÃO seja youtube.com: carregar a página como se já fosse
 * https://www.youtube.com faz o widget da IFrame API rejeitar o próprio
 * embed (loop de auto-embedding), retornando `onError` com um código não
 * documentado (152) quase instantaneamente, antes de qualquer tentativa
 * real de buffering.
 */
private const val PLAYER_ORIGIN = "https://desafiomusical.app"

private fun buildPlayerHtml(videoId: String): String = """
    <!DOCTYPE html>
    <html>
    <body style="margin:0;padding:0;background:#000;">
    <div id="player"></div>
    <script src="https://www.youtube.com/iframe_api"></script>
    <script>
        var player;
        function onYouTubeIframeAPIReady() {
            player = new YT.Player('player', {
                height: '1',
                width: '1',
                videoId: '$videoId',
                playerVars: { autoplay: 1, controls: 0, playsinline: 1, origin: '$PLAYER_ORIGIN' },
                events: {
                    'onReady': function(e) { e.target.playVideo(); },
                    'onError': function(e) { AndroidBridge.reportError(e.data); }
                }
            });
        }
        function playVideo() { if (player && player.playVideo) player.playVideo(); }
        function pauseVideo() { if (player && player.pauseVideo) player.pauseVideo(); }
    </script>
    </body>
    </html>
""".trimIndent()
