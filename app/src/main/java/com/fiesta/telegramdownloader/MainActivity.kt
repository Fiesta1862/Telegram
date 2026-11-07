package com.fiesta.telegramdownloader

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

private val Ink = Color(0xFF0B1220)
private val Panel = Color(0xFF17263A)
private val Mint = Color(0xFF67E5CF)
private val Muted = Color(0xFF9DB0C5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TeleSaveScreen(initialLink = readIncoming(intent)) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val incoming = readIncoming(intent)
        if (incoming.isNotBlank()) setContent { TeleSaveScreen(initialLink = incoming) }
    }

    private fun readIncoming(intent: Intent?): String = when (intent?.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        Intent.ACTION_VIEW -> intent.dataString.orEmpty()
        else -> ""
    }
}

@Composable
private fun TeleSaveScreen(initialLink: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var link by remember { mutableStateOf(initialLink) }
    var status by remember { mutableStateOf("Paste a public video link to get started.") }
    var busy by remember { mutableStateOf(false) }
    var lastFile by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val pulse by animateFloatAsState(if (busy) 0.92f else 1f, animationSpec = spring(), label = "downloadPulse")

    fun pasteClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        link = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        status = if (link.isBlank()) "Clipboard is empty." else "Link detected. Ready to download."
    }

    fun startDownload() {
        if (busy) return
        if (link.isBlank()) {
            status = "Paste or share a link first."
            return
        }
        scope.launch {
            busy = true
            status = "Checking the public link…"
            try {
                val mediaUrl = withContext(Dispatchers.IO) { MediaResolver.resolve(link.trim()) }
                val uri = Uri.parse(mediaUrl)
                val filename = URLUtil.guessFileName(mediaUrl, null, "video/mp4")
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .let { if (it.contains('.')) it else "$it.mp4" }
                val request = DownloadManager.Request(uri)
                    .setTitle(filename)
                    .setDescription("Downloading with TeleSave")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "TeleSave/$filename")
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                manager.enqueue(request)
                lastFile = filename
                status = "Download added. Check your notification shade."
            } catch (e: Exception) {
                status = e.message ?: "Couldn't find a downloadable video on this page."
            } finally {
                busy = false
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, onPrimary = Ink, background = Ink, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF0A1423), Color(0xFF142A40), Color(0xFF101B2C)))
            )
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Mint, Color(0xFF68AFFF)))),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Download, null, tint = Ink, modifier = Modifier.size(27.dp)) }
                    Column {
                        Text("TeleSave", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("VIDEO DOWNLOADER", fontSize = 10.sp, letterSpacing = 2.sp, color = Muted)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = .08f)).padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Text("BETA", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save videos.
Keep it simple.", fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Share a public Telegram post or paste a direct video link.", color = Muted, fontSize = 14.sp)
                }

                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Link, null, tint = Mint)
                        Text("VIDEO LINK", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = link, onValueChange = { link = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://t.me/…", color = Muted) },
                        minLines = 2, maxLines = 4,
                        shape = RoundedCornerShape(17.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Mint, unfocusedBorderColor = Color.White.copy(alpha = .15f),
                            cursorColor = Mint, focusedContainerColor = Color.White.copy(alpha = .04f),
                            unfocusedContainerColor = Color.White.copy(alpha = .03f)
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton("Paste link", Icons.Rounded.ContentPaste, Modifier.weight(1f), onClick = ::pasteClipboard)
                        GlassButton("Clear", null, Modifier.weight(1f), onClick = { link = ""; status = "Link cleared." })
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = ::startDownload, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(56.dp).shadow(12.dp, RoundedCornerShape(18.dp)).graphicsScale(pulse),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Ink, strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.Download, null)
                        Spacer(Modifier.width(9.dp))
                        Text(if (busy) "Checking link…" else "Download video", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                AnimatedVisibility(status.isNotBlank()) {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.TipsAndUpdates, null, tint = Mint)
                            Column(Modifier.weight(1f)) {
                                Text("STATUS", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text(status, color = Color.White, fontSize = 13.sp)
                                if (lastFile.isNotBlank()) Text(lastFile, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Speed, null, tint = Mint, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Background downloads", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Android manages the transfer and notification.", color = Muted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Movie, null, tint = Mint, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Public media only", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Private or protected Telegram posts aren't supported.", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
                Text("TeleSave is an independent app, not affiliated with Telegram. Download only content you have permission to save.", color = Muted.copy(alpha = .75f), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .background(Brush.linearGradient(listOf(Color.White.copy(.12f), Color.White.copy(.045f))))
            .border(1.dp, Color.White.copy(.16f), RoundedCornerShape(25.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun GlassButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(.2f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, containerColor = Color.White.copy(.06f))
    ) {
        if (icon != null) { Icon(icon, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)) }
        Text(label, fontSize = 13.sp)
    }
}

private fun Modifier.graphicsScale(scale: Float) = graphicsLayer {
    scaleX = scale
    scaleY = scale
}

private object MediaResolver {
    fun resolve(input: String): String {
        val uri = runCatching { URI(input) }.getOrNull()
            ?: throw IllegalArgumentException("That doesn't look like a valid URL.")
        if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank())
            throw IllegalArgumentException("Only HTTP and HTTPS links are supported.")
        val host = uri.host.lowercase()
        if (host == "t.me" || host.endsWith(".t.me")) {
            val path = uri.path.orEmpty()
            if (path.startsWith("/c/") || path.startsWith("/+") || path.startsWith("/joinchat/"))
                throw IllegalArgumentException("Private channels and invite-only links aren't supported.")
        }
        val path = uri.path.orEmpty().lowercase()
        if (listOf(".mp4", ".m4v", ".mov", ".webm", ".mkv").any { path.endsWith(it) }) return uri.toString()

        val connection = (URL(input).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 12_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36")
            setRequestProperty("Accept", "text/html,video/*;q=0.9,*/*;q=0.8")
        }
        try {
            connection.connect()
            val type = connection.contentType.orEmpty().lowercase()
            val finalUrl = connection.url.toString()
            if (type.startsWith("video/")) return finalUrl
            if (!type.contains("html")) throw IllegalArgumentException("The link doesn't return a video or a readable public webpage.")
            val html = connection.inputStream.bufferedReader().use { it.readText().take(2_000_000) }
            val candidates = listOf(
                Regex("""(?i)<meta[^>]+(?:property|name)=["'](?:og:video(?::url)?|twitter:player:stream)["'][^>]+content=["']([^"']+)["']"""),
                Regex("""(?i)<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["'](?:og:video(?::url)?|twitter:player:stream)["']"""),
                Regex("""(?i)<source[^>]+src=["']([^"']+)["']"""),
                Regex("""(?i)<video[^>]+src=["']([^"']+)["']""")
            )
            for (pattern in candidates) {
                val match = pattern.find(html)?.groupValues?.getOrNull(1)?.replace("&amp;", "&") ?: continue
                val resolved = runCatching { URL(URL(finalUrl), match).toString() }.getOrNull() ?: continue
                val mediaUri = runCatching { URI(resolved) }.getOrNull() ?: continue
                if (mediaUri.scheme in listOf("http", "https") && !mediaUri.host.isNullOrBlank()) return resolved
            }
            throw IllegalArgumentException("No public downloadable video was found. Some Telegram posts only play inside Telegram.")
        } finally {
            connection.disconnect()
        }
    }
}
