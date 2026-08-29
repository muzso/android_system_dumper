package hu.muzso.android_system_dumper.network.download

import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.download.DownloadProgress
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.network.ArchiveGenerator
import hu.muzso.android_system_dumper.platform.ResourceProvider
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.meta
import kotlinx.html.onClick
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.ul
import kotlinx.html.unsafe
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpDownloadServer @Inject constructor(
    private val archiveGenerator: ArchiveGenerator,
    private val clock: Clock,
    private val logger: FileLogger,
    private val platformUtils: PlatformUtils,
    private val resourceProvider: ResourceProvider
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var port: Int = 0
    private val isDownloading = AtomicBoolean(false)
    private val successCount = AtomicInteger(0)
    private val downloadedUniqueFiles = ConcurrentHashMap.newKeySet<String>()
    private var totalUniqueFilesAvailable = 0
    private val totalDownloadedBytes = AtomicLong(0)
    private var startTime: Long = 0

    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    val progress = _progress.asStateFlow()

    /**
     * Starts the HTTP server on all local interfaces using a random port.
     * 
     * @param parameters The upload parameters to use for ZIP generation.
     * @param scanResult The scan results to be shared.
     * @return The port the server is listening on.
     */
    fun start(parameters: UploadParameters, scanResult: ScanResult): Int {
        if (server != null) return port

        prepareState(parameters, scanResult)
        port = (16384..32767).random()

        server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
            configureServer(this, parameters)
        }.start(wait = false)

        return port
    }

    internal fun prepareState(parameters: UploadParameters, scanResult: ScanResult) {
        archiveGenerator.prepare(parameters, scanResult)
        successCount.set(0)
        downloadedUniqueFiles.clear()
        totalDownloadedBytes.set(0)
        startTime = 0
        
        totalUniqueFilesAvailable = archiveGenerator.getBatchCount() + if (archiveGenerator.shouldGenerateMisc()) 1 else 0

        _progress.value = DownloadProgress(
            successCount = 0,
            totalCount = totalUniqueFilesAvailable,
            startTime = 0
        )
    }

    internal fun configureServer(application: Application, parameters: UploadParameters) {
        application.routing {
            get("/") {
                    call.respondHtml {
                        head {
                            title { resourceProvider.getString(R.string.app_name) }
                            meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                            style {
                                unsafe {
                                    +"""
                                    body {
                                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                                        line-height: 1.6;
                                        color: #333;
                                        background-color: #f4f7f9;
                                        margin: 0;
                                        padding: 0;
                                    }
                                    .container {
                                        width: 100%;
                                        max-width: 600px;
                                        margin: 0 auto;
                                        padding: 0 20px 40px;
                                        box-sizing: border-box;
                                        display: flex;
                                        flex-direction: column;
                                    }
                                    header {
                                        background-color: #0277C4;
                                        color: white;
                                        padding: 40px 20px;
                                        text-align: center;
                                        margin-bottom: 24px;
                                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                                    }
                                    h1 {
                                        margin: 0;
                                        font-size: 24px;
                                        font-weight: 600;
                                    }
                                    .info-card {
                                        background: white;
                                        border-radius: 12px;
                                        padding: 16px;
                                        margin-bottom: 24px;
                                        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
                                    }
                                    .info-card code {
                                        background: #eef2f7;
                                        padding: 2px 6px;
                                        border-radius: 4px;
                                        font-weight: bold;
                                        color: #0277C4;
                                    }
                                    .btn-container {
                                        display: flex;
                                        justify-content: center;
                                        margin-bottom: 24px;
                                    }
                                    .btn-download-all {
                                        background-color: #0277C4;
                                        color: white;
                                        border: none;
                                        padding: 12px 24px;
                                        border-radius: 8px;
                                        font-size: 16px;
                                        font-weight: 600;
                                        cursor: pointer;
                                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                                        transition: background-color 0.2s, transform 0.1s;
                                    }
                                    .btn-download-all:hover { background-color: #026bb1; }
                                    .btn-download-all:active { transform: scale(0.98); }
                                    .btn-download-all:disabled {
                                        background-color: #ccc;
                                        cursor: not-allowed;
                                        transform: none;
                                    }
                                    .status-message {
                                        text-align: center;
                                        font-size: 14px;
                                        color: #0277C4;
                                        font-weight: 600;
                                        margin-bottom: 24px;
                                        display: none;
                                    }
                                    ul {
                                        list-style: none;
                                        padding: 0;
                                        margin: 0;
                                    }
                                    li {
                                        margin-bottom: 30px;
                                    }
                                    a {
                                        display: block;
                                        background: white;
                                        padding: 20px;
                                        border-radius: 12px;
                                        text-decoration: none;
                                        color: #0277C4;
                                        font-weight: 600;
                                        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
                                        transition: transform 0.1s, box-shadow 0.1s;
                                        border-left: 6px solid #0277C4;
                                    }
                                    a:hover {
                                        background: #fafafa;
                                    }
                                    a:active {
                                        transform: scale(0.98);
                                        box-shadow: 0 1px 2px rgba(0,0,0,0.1);
                                    }
                                    .info-table {
                                        width: 100%;
                                        border-collapse: collapse;
                                        font-size: 14px;
                                    }
                                    .info-table td {
                                        padding-top: 4px;
                                        padding-bottom: 4px;
                                        vertical-align: middle;
                                    }
                                    .info-label {
                                        color: #666;
                                        width: 1%;
                                        white-space: nowrap;
                                        padding-right: 16px;
                                    }
                                    """.trimIndent()
                                }
                            }
                            script {
                                unsafe {
                                    +"""
                                    let isDownloadingAll = false;
                                    let dotInterval = null;
                                    
                                    function updateDots(filename) {
                                        const statusEl = document.getElementById('status-message');
                                        let dots = 0;
                                        if (dotInterval) clearInterval(dotInterval);
                                        dotInterval = setInterval(() => {
                                            dots = (dots % 3) + 1;
                                            statusEl.textContent = 'Downloading ' + filename + ' ' + '.'.repeat(dots);
                                        }, 500);
                                    }

                                    async function getStatus() {
                                        try {
                                            const response = await fetch('/downloadstatus');
                                            return await response.json();
                                        } catch (e) {
                                            return { isBusy: false, count: -1 };
                                        }
                                    }

                                    async function downloadAll() {
                                        if (isDownloadingAll) return;
                                        isDownloadingAll = true;
                                        const startTimestamp = Date.now();
                                        
                                        const btn = document.getElementById('btn-download-all');
                                        const statusEl = document.getElementById('status-message');
                                        const links = Array.from(document.querySelectorAll('ul li a'));
                                        
                                        btn.disabled = true;
                                        statusEl.style.display = 'block';
                                        
                                        for (const link of links) {
                                            const filename = link.textContent;
                                            updateDots(filename);
                                            
                                            const startStatus = await getStatus();
                                            
                                            // Trigger download
                                            const a = document.createElement('a');
                                            a.style.display = 'none';
                                            a.href = link.href;
                                            a.download = '';
                                            document.body.appendChild(a);
                                            a.click();
                                            document.body.removeChild(a);
                                            
                                            // 1. Wait for server to start processing OR finish immediately (for tiny files)
                                            // We poll for up to 30s to handle Chrome's permission prompt
                                            let startedOrFinished = false;
                                            for (let j = 0; j < 150; j++) {
                                                const currentStatus = await getStatus();
                                                if (currentStatus.isBusy || currentStatus.count > startStatus.count) {
                                                    startedOrFinished = true;
                                                    break;
                                                }
                                                await new Promise(r => setTimeout(r, 200));
                                            }
                                            
                                            // 2. If it started (but didn't finish yet), wait for server to become idle
                                            if (startedOrFinished) {
                                                while (true) {
                                                    const currentStatus = await getStatus();
                                                    if (!currentStatus.isBusy) break;
                                                    await new Promise(r => setTimeout(r, 1000));
                                                }
                                            }
                                            
                                            // Small gap between files
                                            await new Promise(r => setTimeout(r, 500));
                                        }
                                        
                                        if (dotInterval) clearInterval(dotInterval);
                                        const durationMinutes = ((Date.now() - startTimestamp) / 60000).toFixed(2);
                                        const template = '${resourceProvider.getString(R.string.downloads_finished)}';
                                        statusEl.innerHTML = template.replace('%minutes%', durationMinutes) + '<br/>' + '${
                                        resourceProvider.getString(R.string.verify_downloads)
                                    }';
                                        isDownloadingAll = false;
                                        btn.disabled = false;
                                    }
                                    """.trimIndent()
                                }
                            }
                        }
                        body {
                            header {
                                h1 { +resourceProvider.getString(R.string.app_name) }
                            }
                            div(classes = "container") {
                                div(classes = "info-card") {
                                    table(classes = "info-table") {
                                        tr {
                                            td(classes = "info-label") {
                                                + "${resourceProvider.getString(R.string.zip_encryption)}:"
                                            }
                                            td {
                                                code { +parameters.zipEncryption.toString() }
                                            }
                                        }
                                        if (archiveGenerator.getEncryptionPassphrase() != null) {
                                            tr {
                                                td(classes = "info-label") {
                                                    + "${resourceProvider.getString(R.string.zip_passphrase)}:"
                                                }
                                                td {
                                                    code { +archiveGenerator.getEncryptionPassphrase()!! }
                                                }
                                            }
                                        }
                                    }
                                }
                                div(classes = "btn-container") {
                                    button(classes = "btn-download-all") {
                                        id = "btn-download-all"
                                        onClick = "downloadAll()"
                                        +resourceProvider.getString(R.string.download_all)
                                    }
                                }
                                div(classes = "status-message") {
                                    id = "status-message"
                                }
                                ul {
                                    for (i in 1..archiveGenerator.getBatchCount()) {
                                        val displayName = archiveGenerator.getBatchFilename(i)
                                        li { a(href = "/$displayName", target = "_blank") { +displayName } }
                                    }
                                    if (archiveGenerator.shouldGenerateMisc()) {
                                        val displayName = archiveGenerator.getMiscZipFilename()
                                        li { a(href = "/$displayName", target = "_blank") { +displayName } }
                                    }
                                }
                            }
                        }
                    }
                }

                get("/downloadstatus") {
                    call.respondText(
                        "{\"isBusy\":${isDownloading.get()},\"count\":${successCount.get()}}",
                        ContentType.Application.Json
                    )
                }

                get("/{filename}") {
                    val filenameParam = call.parameters["filename"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                    val isMisc = archiveGenerator.shouldGenerateMisc() && filenameParam == archiveGenerator.getMiscZipFilename()
                    var batchIndex: Int? = null
                    if (!isMisc) {
                        for (i in 1..archiveGenerator.getBatchCount()) {
                            if (filenameParam == archiveGenerator.getBatchFilename(i)) {
                                batchIndex = i
                                break
                            }
                        }
                        if (batchIndex == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@get
                        }
                    }

                    if (downloadedUniqueFiles.size >= totalUniqueFilesAvailable) {
                        successCount.set(0)
                        downloadedUniqueFiles.clear()
                        totalDownloadedBytes.set(0)
                        startTime = 0
                        _progress.update {
                            it?.copy(
                                successCount = 0,
                                currentBytes = 0,
                                totalDownloadedBytes = 0,
                                isFinished = false,
                                statusText = "",
                                startTime = 0
                            )
                        }
                    }

                    if (startTime == 0L) {
                        val newStartTime = clock.monotonicTime()
                        startTime = newStartTime
                        _progress.update { it?.copy(startTime = newStartTime) }
                    }

                    if (!isDownloading.compareAndSet(false, true)) {
                        call.respond(HttpStatusCode.TooManyRequests, "")
                        return@get
                    }

                    try {
                        val zipResult = when {
                            isMisc -> archiveGenerator.generateMisc()
                            batchIndex != null -> {
                                _progress.update { it?.copy(statusText = "Preparing $filenameParam...") }
                                archiveGenerator.generateBatch(batchIndex)
                            }
                            else -> null // Should not happen due to check above
                        }

                        if (zipResult == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@get
                        }

                        when (zipResult) {
                            is DomainResult.Success -> {
                                val file = File(zipResult.data.path)
                                val responseFilename = zipResult.data.filename
                                val totalBytes = file.length()

                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, responseFilename).toString()
                                )

                                _progress.update { it?.copy(
                                    currentFileName = responseFilename,
                                    totalBytes = totalBytes,
                                    currentBytes = 0,
                                    statusText = "Downloading $responseFilename..."
                                ) }

                                var sentBytes = 0L
                                call.respond(object : OutgoingContent.WriteChannelContent() {
                                    override val contentLength = totalBytes
                                    override val contentType = ContentType.Application.Zip

                                    override suspend fun writeTo(channel: ByteWriteChannel) {
                                        file.inputStream().use { input ->
                                            val buffer = ByteArray(64 * 1024)
                                            var read = input.read(buffer)
                                            while (read != -1) {
                                                channel.writeFully(buffer, 0, read)
                                                sentBytes += read
                                                totalDownloadedBytes.addAndGet(read.toLong())
                                                _progress.update { it?.copy(currentBytes = sentBytes) }
                                                read = input.read(buffer)
                                            }
                                        }
                                    }
                                })
                                
                                if (downloadedUniqueFiles.add(responseFilename)) {
                                    successCount.incrementAndGet()
                                }
                                val currentSuccess = successCount.get()
                                val allFilesDownloaded = downloadedUniqueFiles.size >= totalUniqueFilesAvailable

                                val runtimeSeconds = (clock.monotonicTime() - startTime) / 1_000_000_000.0
                                val totalFormatted = platformUtils.formatBytes(totalDownloadedBytes.get())
                                
                                val status = if (allFilesDownloaded) {
                                    "Success! Downloaded all $totalUniqueFilesAvailable files, $totalFormatted in ${String.format(
                                        Locale.US, "%.2f", runtimeSeconds / 60.0)} minutes"
                                } else {
                                    "Successfully downloaded $responseFilename"
                                }

                                _progress.update { it?.copy(
                                    successCount = currentSuccess,
                                    statusText = status,
                                    totalDownloadedBytes = totalDownloadedBytes.get(),
                                    isFinished = allFilesDownloaded
                                ) }

                                archiveGenerator.cleanup(zipResult.data.path)
                            }
                            is DomainResult.Error -> {
                                logger.e("HttpDownloadServer", "Error generating ZIP: ${zipResult.error}")
                                call.respond(HttpStatusCode.InternalServerError, "Error generating ZIP")
                            }
                        }
                    } catch (e: Exception) {
                        logger.e("HttpDownloadServer", "Download failed", e)
                        // In Ktor, if we already started responding (WriteChannelContent), we can't respond again.
                        // But if it failed before respond call, we can.
                    } finally {
                        isDownloading.set(false)
                    }
                }
            }
    }

    /**
     * Stops the HTTP server and clears progress state.
     */
    fun stop() {
        server?.stop(500, 1000)
        server = null
        _progress.value = null
    }
}
