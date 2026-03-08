package com.droidbert

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var comicImage: ImageView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var comicDateText: TextView
    private lateinit var statusText: TextView
    private lateinit var previousButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var latestButton: MaterialButton
    private lateinit var pickDateButton: MaterialButton

    private val httpClient = OkHttpClient()
    private val apiPathRegex = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})_")
    private val legacyComicPathRegex = Pattern.compile("\"(\\d{4}/\\d{4}-\\d{2}-\\d{2}[^\"\\\\]*?\\.gif)\"")
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private lateinit var imageLoader: ImageLoader
    private var currentDate: String? = null
    private var isLoading = false
    private var lastKnownApiBaseUrl: String? = null
    private var legacyIndexCacheOrigin: String? = null
    private var legacyIndexCacheFiles: List<String> = emptyList()

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshIfApiUrlChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        applyToolbarWindowInsets(toolbar)
        setSupportActionBar(toolbar)

        comicImage = findViewById(R.id.comic_image)
        loadingIndicator = findViewById(R.id.loading_indicator)
        comicDateText = findViewById(R.id.comic_date_text)
        statusText = findViewById(R.id.status_text)
        previousButton = findViewById(R.id.previous_button)
        nextButton = findViewById(R.id.next_button)
        latestButton = findViewById(R.id.latest_button)
        pickDateButton = findViewById(R.id.pick_date_button)

        imageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        previousButton.setOnClickListener { loadAdjacentComic(daysDelta = -1) }
        nextButton.setOnClickListener { loadAdjacentComic(daysDelta = 1) }
        latestButton.setOnClickListener { loadLatestComic() }
        pickDateButton.setOnClickListener { openDatePicker() }

        lastKnownApiBaseUrl = getApiBaseUrl()
        loadInitialComic()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshIfApiUrlChanged() {
        val currentApi = getApiBaseUrl()
        if (currentApi == lastKnownApiBaseUrl) {
            return
        }
        lastKnownApiBaseUrl = currentApi
        legacyIndexCacheOrigin = null
        legacyIndexCacheFiles = emptyList()
        val dateToReload = currentDate
        if (dateToReload.isNullOrBlank()) {
            loadInitialComic()
        } else {
            loadComic(dateToReload)
        }
    }

    private fun loadInitialComic() {
        val initialRequest = resolveInitialComicRequest(getLastViewedDate())
        loadComic(
            date = initialRequest.date,
            fallbackToLatestIfMissing = initialRequest.fallbackToLatestIfMissing
        )
    }

    private fun openDatePicker() {
        val selectedMillis = currentDate?.let(::parseDateToMillis)
        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.pick_date))
        if (selectedMillis != null) {
            builder.setSelection(selectedMillis)
        }
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            val selectedDate = isoDateFormat.format(Date(utcMillis))
            loadComic(date = selectedDate)
        }
        picker.show(supportFragmentManager, "pick_date")
    }

    private fun loadLatestComic() {
        setStaticStatus(getString(R.string.loading_latest))
        loadComic(date = null)
    }

    private fun loadAdjacentComic(daysDelta: Int) {
        val startDate = currentDate ?: return
        if (isLoading) return

        val baseDate = parseDate(startDate) ?: return
        val searchingText = if (daysDelta < 0) {
            getString(R.string.searching_previous)
        } else {
            getString(R.string.searching_next)
        }

        lifecycleScope.launch {
            setLoadingState(true, searchingText)
            val found = withContext(Dispatchers.IO) {
                findAdjacentComic(baseDate, daysDelta)
            }

            when (found) {
                is AdjacentComicResult.Found -> {
                    renderComic(found.payload)
                    setLoadingState(false, "")
                }

                is AdjacentComicResult.EndReached -> {
                    setLoadingState(false, "")
                    statusText.text = if (daysDelta < 0) {
                        getString(R.string.no_more_previous)
                    } else {
                        getString(R.string.no_more_next)
                    }
                }

                is AdjacentComicResult.Error -> {
                    setLoadingState(false, "")
                    statusText.text = found.message
                }
            }
        }
    }

    private fun loadComic(date: String?, fallbackToLatestIfMissing: Boolean = false) {
        if (isLoading) return
        lifecycleScope.launch {
            setLoadingState(true, getString(R.string.loading_comic))

            val result = withContext(Dispatchers.IO) {
                fetchComic(date)
            }

            result.fold(
                onSuccess = { payload ->
                    renderComic(payload)
                    setLoadingState(false, "")
                },
                onFailure = { throwable ->
                    setLoadingState(false, "")
                    val message = if (throwable is ComicApiException) {
                        when {
                            throwable.code == 404 && date != null && isDateNotFoundResponse(throwable.message) -> {
                                if (fallbackToLatestIfMissing) {
                                    statusText.text = getString(R.string.first_comic_missing_fallback)
                                    loadLatestComic()
                                    return@fold
                                }
                                getString(R.string.date_not_found, date)
                            }

                            throwable.code == 404 && date == null -> getString(
                                R.string.api_endpoint_not_found,
                                getApiBaseUrl()
                            )

                            throwable.code == 404 && throwable.message.contains("404 page not found", ignoreCase = true) -> getString(
                                R.string.server_not_ready,
                                getApiBaseUrl()
                            )

                            throwable.message.isNotBlank() -> throwable.message
                            else -> getString(R.string.network_error)
                        }
                    } else {
                        throwable.message ?: getString(R.string.network_error)
                    }
                    statusText.text = message
                }
            )
        }
    }

    private fun renderComic(payload: ComicPayload) {
        currentDate = payload.date
        saveLastViewedDate(payload.date)
        comicDateText.text = getString(R.string.date_title_prefix, payload.date)
        statusText.text = ""

        comicImage.load(payload.bytes, imageLoader) {
            crossfade(true)
            listener(
                onError = { _, _ ->
                    statusText.text = getString(R.string.network_error)
                }
            )
        }
    }

    private suspend fun fetchComic(date: String?): Result<ComicPayload> {
        return runCatching {
            val baseUrl = getApiBaseUrl().toHttpUrlOrNull()
                ?: throw IllegalStateException(getString(R.string.invalid_api_url))

            val requestUrl = baseUrl.newBuilder().apply {
                if (date.isNullOrBlank()) {
                    addQueryParameter("latest", "1")
                } else {
                    addQueryParameter("date", date)
                }
            }.build()

            val request = Request.Builder().url(requestUrl).get().build()
            httpClient.newCall(request).execute().use { response ->
                val bodyBytes = response.body?.bytes() ?: ByteArray(0)
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        val fallback = fetchLegacyComic(baseUrl, date)
                        if (fallback != null) {
                            return@use fallback
                        }
                    }
                    val apiError = extractApiError(bodyBytes)
                    throw ComicApiException(response.code, apiError)
                }

                val resolvedDate = extractDateFromHeader(response.header("X-Comic-Path"))
                    ?: date
                    ?: throw ComicApiException(500, "Comic date metadata missing")

                ComicPayload(
                    date = resolvedDate,
                    bytes = bodyBytes
                )
            }
        }
    }

    private fun findAdjacentComic(startDate: Calendar, daysDelta: Int): AdjacentComicResult {
        val probeDate = startDate.clone() as Calendar
        repeat(MAX_NAVIGATION_LOOKUPS) {
            probeDate.add(Calendar.DAY_OF_MONTH, daysDelta)
            val isoDate = isoDateFormat.format(probeDate.time)
            val fetchResult = fetchComicBlocking(isoDate)

            if (fetchResult.isSuccess) {
                return AdjacentComicResult.Found(fetchResult.getOrThrow())
            }

            val error = fetchResult.exceptionOrNull()
            if (error is ComicApiException && error.code == 404) {
                return@repeat
            }

            return AdjacentComicResult.Error(error?.message ?: getString(R.string.network_error))
        }

        return AdjacentComicResult.EndReached
    }

    private fun fetchComicBlocking(date: String): Result<ComicPayload> {
        return try {
            val baseUrl = getApiBaseUrl().toHttpUrlOrNull()
                ?: return Result.failure(IllegalStateException(getString(R.string.invalid_api_url)))
            val requestUrl = baseUrl.newBuilder()
                .addQueryParameter("date", date)
                .build()
            val request = Request.Builder().url(requestUrl).get().build()
            httpClient.newCall(request).execute().use { response ->
                val bodyBytes = response.body?.bytes() ?: ByteArray(0)
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        val fallback = fetchLegacyComic(baseUrl, date)
                        if (fallback != null) {
                            return Result.success(fallback)
                        }
                    }
                    val apiError = extractApiError(bodyBytes)
                    return Result.failure(ComicApiException(response.code, apiError))
                }
                val resolvedDate = extractDateFromHeader(response.header("X-Comic-Path")) ?: date
                Result.success(ComicPayload(resolvedDate, bodyBytes))
            }
        } catch (io: IOException) {
            Result.failure(ComicApiException(0, getString(R.string.network_error), io))
        }
    }

    private fun setLoadingState(loading: Boolean, loadingMessage: String) {
        isLoading = loading
        loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        previousButton.isEnabled = !loading
        nextButton.isEnabled = !loading
        latestButton.isEnabled = !loading
        pickDateButton.isEnabled = !loading
        if (loadingMessage.isNotBlank()) {
            statusText.text = loadingMessage
        }
    }

    private fun setStaticStatus(message: String) {
        statusText.text = message
    }

    private fun saveLastViewedDate(date: String) {
        getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(AppPrefs.KEY_LAST_VIEWED_DATE, date)
            .apply()
    }

    private fun getLastViewedDate(): String? {
        return getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .getString(AppPrefs.KEY_LAST_VIEWED_DATE, null)
    }

    private fun getApiBaseUrl(): String {
        val stored = getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .getString(AppPrefs.KEY_API_BASE_URL, getString(R.string.api_base_url_default))
            .orEmpty()
        return ApiUrlUtils.normalizeApiBaseUrl(stored)
    }

    private fun extractDateFromHeader(pathHeader: String?): String? {
        if (pathHeader.isNullOrBlank()) return null
        val matcher = apiPathRegex.matcher(pathHeader)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun fetchLegacyComic(baseUrl: HttpUrl, date: String?): ComicPayload? {
        val legacyRoots = buildLegacyRoots(baseUrl)
        val filesByRoot = legacyRoots
            .map { it to getLegacyComicFiles(it) }
            .firstOrNull { it.second.isNotEmpty() }
            ?: return null

        val root = filesByRoot.first
        val files = filesByRoot.second

        if (files.isEmpty()) {
            return null
        }

        val relativePath = if (date.isNullOrBlank()) {
            files.lastOrNull()
        } else {
            files.firstOrNull { extractDateFromHeader(it) == date }
        } ?: return null

        val resolvedDate = extractDateFromHeader(relativePath) ?: date ?: return null
        val imageUrl = root.newBuilder()
            .addPathSegment("comics")
            .addPathSegments(relativePath)
            .build()

        val imageRequest = Request.Builder().url(imageUrl).get().build()
        httpClient.newCall(imageRequest).execute().use { imageResponse ->
            if (!imageResponse.isSuccessful) {
                return null
            }

            val imageBytes = imageResponse.body?.bytes() ?: return null
            return ComicPayload(resolvedDate, imageBytes)
        }
    }

    private fun buildLegacyRoots(baseUrl: HttpUrl): List<HttpUrl> {
        val hostRoot = baseUrl.newBuilder()
            .encodedPath("/")
            .query(null)
            .build()

        val apiPath = baseUrl.encodedPath
        val matchedSuffix = API_PATH_SUFFIXES.firstOrNull { apiPath.endsWith(it) }
        val pathRoot = if (matchedSuffix != null) {
            val prefix = apiPath.removeSuffix(matchedSuffix).ifBlank { "/" }
            baseUrl.newBuilder()
                .encodedPath(if (prefix.endsWith('/')) prefix else "$prefix/")
                .query(null)
                .build()
        } else {
            hostRoot
        }

        return listOf(pathRoot, hostRoot).distinctBy { it.toString() }
    }

    private fun getLegacyComicFiles(root: HttpUrl): List<String> {
        val originKey = root.toString()
        if (legacyIndexCacheOrigin == originKey && legacyIndexCacheFiles.isNotEmpty()) {
            return legacyIndexCacheFiles
        }

        val basePath = root.encodedPath.trimEnd('/').ifBlank { "" }
        val indexPath = if (basePath.isBlank()) {
            "/get_comics.php"
        } else {
            "$basePath/get_comics.php"
        }
        val indexUrl = root.newBuilder().encodedPath(indexPath).query(null).build()
        val request = Request.Builder().url(indexUrl).get().build()
        val files = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return emptyList()
            }
            val body = response.body?.string().orEmpty()
            val matcher = legacyComicPathRegex.matcher(body)
            val matches = mutableListOf<String>()
            while (matcher.find()) {
                matcher.group(1)?.let(matches::add)
            }
            matches
        }

        if (files.isNotEmpty()) {
            legacyIndexCacheOrigin = originKey
            legacyIndexCacheFiles = files
        }

        return files
    }

    private fun extractApiError(body: ByteArray): String {
        val raw = body.toString(Charsets.UTF_8)
        val match = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)
        match?.groupValues?.getOrNull(1)?.let { return it }

        val plainText = raw.trim().replace(Regex("\\s+"), " ")
        if (plainText.isNotBlank() && plainText.length <= 200) {
            return plainText
        }

        return getString(R.string.network_error)
    }

    private fun isDateNotFoundResponse(message: String): Boolean {
        return message.contains("comic not found", ignoreCase = true)
    }

    private fun parseDate(date: String): Calendar? {
        val parsed = runCatching { isoDateFormat.parse(date) }.getOrNull() ?: return null
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
            time = parsed
        }
    }

    private fun parseDateToMillis(date: String): Long? {
        return parseDate(date)?.timeInMillis
    }

    private fun applyToolbarWindowInsets(toolbar: MaterialToolbar) {
        val initialPaddingTop = toolbar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, windowInsets ->
            val statusBarInsets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(
                view.paddingLeft,
                initialPaddingTop + statusBarInsets.top,
                view.paddingRight,
                view.paddingBottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(toolbar)
    }

    private sealed class AdjacentComicResult {
        data class Found(val payload: ComicPayload) : AdjacentComicResult()
        data class Error(val message: String) : AdjacentComicResult()
        data object EndReached : AdjacentComicResult()
    }

    private data class ComicPayload(
        val date: String,
        val bytes: ByteArray
    )

    private class ComicApiException(
        val code: Int,
        override val message: String,
        cause: Throwable? = null
    ) : IOException(message, cause)

    internal data class InitialComicRequest(
        val date: String,
        val fallbackToLatestIfMissing: Boolean
    )

    companion object {
        private const val MAX_NAVIGATION_LOOKUPS = 15000
        private const val FIRST_COMIC_DATE = "1989-04-16"
        private val API_PATH_SUFFIXES = listOf("/api/current.php", "/api/comic.php")

        internal fun resolveInitialComicRequest(storedDate: String?): InitialComicRequest {
            return if (storedDate.isNullOrBlank()) {
                InitialComicRequest(
                    date = FIRST_COMIC_DATE,
                    fallbackToLatestIfMissing = true
                )
            } else {
                InitialComicRequest(
                    date = storedDate,
                    fallbackToLatestIfMissing = false
                )
            }
        }
    }
}
