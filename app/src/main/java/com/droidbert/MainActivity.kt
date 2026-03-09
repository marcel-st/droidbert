package com.droidbert

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
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
import kotlinx.coroutines.Job
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
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MAX_NAVIGATION_LOOKUPS = 15000
        private const val FIRST_COMIC_DATE = "1989-04-16"
        private val API_PATH_SUFFIXES = listOf("/api/current.php", "/api/comic.php")
        private const val DEFAULT_ACCEPT_HEADER = "*/*"
        private const val DEFAULT_ACCEPT_LANGUAGE_HEADER = "en-US,en;q=0.9"

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

    private lateinit var comicImage: ImageView
    private lateinit var panelContainer: LinearLayout
    private lateinit var comicScrollView: NestedScrollView
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
    private var renderPanelsJob: Job? = null
    private var currentDate: String? = null
    private var lastRenderedComicBytes: ByteArray? = null
    private var isLoading = false
    private var lastKnownApiBaseUrl: String? = null
    private var lastKnownAutoSplitPanels: Boolean = true
    private var lastKnownInvertSwipeDirection: Boolean = false
    private var legacyIndexCacheOrigin: String? = null
    private var legacyIndexCacheFiles: List<String> = emptyList()

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshIfSettingsChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        applyToolbarWindowInsets(toolbar)
        setSupportActionBar(toolbar)

        comicImage = findViewById(R.id.comic_image)
        panelContainer = findViewById(R.id.panel_container)
        comicScrollView = findViewById(R.id.comic_scroll_view)
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
        setupSwipeNavigation()

        lastKnownApiBaseUrl = getApiBaseUrl()
        lastKnownAutoSplitPanels = isAutoSplitPanelsEnabled()
        lastKnownInvertSwipeDirection = isInvertSwipeDirectionEnabled()
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

    private fun refreshIfSettingsChanged() {
        val currentApi = getApiBaseUrl()
        val currentAutoSplitPanels = isAutoSplitPanelsEnabled()
        val currentInvertSwipeDirection = isInvertSwipeDirectionEnabled()
        val apiChanged = currentApi != lastKnownApiBaseUrl
        val autoSplitChanged = currentAutoSplitPanels != lastKnownAutoSplitPanels
        val swipeDirectionChanged = currentInvertSwipeDirection != lastKnownInvertSwipeDirection
        if (!apiChanged && !autoSplitChanged && !swipeDirectionChanged) {
            return
        }
        lastKnownApiBaseUrl = currentApi
        lastKnownAutoSplitPanels = currentAutoSplitPanels
        lastKnownInvertSwipeDirection = currentInvertSwipeDirection

        if (apiChanged) {
            legacyIndexCacheOrigin = null
            legacyIndexCacheFiles = emptyList()
        }

        val dateToReload = currentDate
        if (autoSplitChanged && !apiChanged && !dateToReload.isNullOrBlank()) {
            val bytes = lastRenderedComicBytes
            if (bytes != null) {
                renderComic(
                    ComicPayload(
                        date = dateToReload,
                        bytes = bytes
                    )
                )
                return
            }
        }

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
        renderPanelsJob?.cancel()
        currentDate = payload.date
        lastRenderedComicBytes = payload.bytes
        saveLastViewedDate(payload.date)
        comicDateText.text = getString(R.string.date_title_prefix, payload.date)
        statusText.text = ""
        resetComicScrollToTop()

        if (!isAutoSplitPanelsEnabled()) {
            showFullComic(payload.bytes)
            return
        }

        renderPanelsJob = lifecycleScope.launch {
            val panels = withContext(Dispatchers.Default) {
                splitComicIntoPanels(payload.bytes)
            }

            if (panels.size > 1) {
                showPanels(panels)
            } else {
                showFullComic(payload.bytes)
            }
        }
    }

    private fun setupSwipeNavigation() {
        val gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (isLoading || e1 == null) return false

                    val deltaX = e2.x - e1.x
                    val deltaY = e2.y - e1.y
                    val isHorizontalFling =
                        abs(deltaX) > 120f &&
                            abs(velocityX) > 350f &&
                            abs(deltaX) > abs(deltaY) * 1.2f

                    if (!isHorizontalFling) return false

                    val nextDelta = if (isInvertSwipeDirectionEnabled()) -1 else 1
                    val previousDelta = -nextDelta

                    if (deltaX < 0) {
                        loadAdjacentComic(daysDelta = nextDelta)
                    } else {
                        loadAdjacentComic(daysDelta = previousDelta)
                    }
                    return true
                }
            }
        )

        comicScrollView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun resetComicScrollToTop() {
        comicScrollView.post {
            comicScrollView.scrollTo(0, 0)
        }
    }

    private fun showFullComic(bytes: ByteArray) {
        panelContainer.removeAllViews()
        panelContainer.visibility = View.GONE
        comicImage.visibility = View.VISIBLE

        comicImage.load(bytes, imageLoader) {
            crossfade(true)
            listener(
                onError = { _, _ ->
                    statusText.text = getString(R.string.network_error)
                }
            )
        }
    }

    private fun showPanels(panels: List<Bitmap>) {
        comicImage.setImageDrawable(null)
        comicImage.visibility = View.GONE
        panelContainer.removeAllViews()

        val marginPx = (8 * resources.displayMetrics.density).toInt()
        panels.forEach { panel ->
            val panelView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { lp ->
                    lp.bottomMargin = marginPx
                }
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageBitmap(panel)
            }
            panelContainer.addView(panelView)
        }
        panelContainer.visibility = View.VISIBLE
    }

    private fun splitComicIntoPanels(bytes: ByteArray): List<Bitmap> {
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return emptyList()
        if (source.width < 80 || source.height < 80) return emptyList()

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val blackThreshold = 36
        val brightThreshold = 236
        val rowBlackRatios = FloatArray(height)
        val colBlackRatios = FloatArray(width)
        val rowLightRatios = FloatArray(height)
        val colLightRatios = FloatArray(width)

        for (y in 0 until height) {
            var rowBlack = 0
            var rowLight = 0
            val rowOffset = y * width
            for (x in 0 until width) {
                val pixel = pixels[rowOffset + x]
                if (isDarkPixel(pixel, blackThreshold)) {
                    rowBlack++
                    colBlackRatios[x] += 1f
                }
                if (isBrightPixel(pixel, brightThreshold)) {
                    rowLight++
                    colLightRatios[x] += 1f
                }
            }
            rowBlackRatios[y] = rowBlack.toFloat() / width
            rowLightRatios[y] = rowLight.toFloat() / width
        }

        for (x in 0 until width) {
            colBlackRatios[x] /= height.toFloat()
            colLightRatios[x] /= height.toFloat()
        }

        val rowDarkBars = collectSeparatorBands(
            ratios = rowBlackRatios,
            ratioThreshold = 0.88f,
            minThickness = 2,
            minEdgeDistance = (height * 0.02f).toInt(),
            maxThickness = maxOf(8, (height * 0.1f).toInt())
        )
        val rowLightBars = collectSeparatorBands(
            ratios = rowLightRatios,
            ratioThreshold = 0.94f,
            minThickness = 2,
            minEdgeDistance = (height * 0.02f).toInt(),
            maxThickness = maxOf(8, (height * 0.12f).toInt())
        )
        val colDarkBars = collectSeparatorBands(
            ratios = colBlackRatios,
            ratioThreshold = 0.88f,
            minThickness = 2,
            minEdgeDistance = (width * 0.02f).toInt(),
            maxThickness = maxOf(8, (width * 0.1f).toInt())
        )
        val colLightBars = collectSeparatorBands(
            ratios = colLightRatios,
            ratioThreshold = 0.94f,
            minThickness = 2,
            minEdgeDistance = (width * 0.02f).toInt(),
            maxThickness = maxOf(8, (width * 0.12f).toInt())
        )

        val rowBars = mergeBands(rowDarkBars + rowLightBars, maxGap = 4)
        val colBars = mergeBands(colDarkBars + colLightBars, maxGap = 4)

        if (rowBars.isEmpty() && colBars.isEmpty()) {
            return emptyList()
        }

        val yCuts = buildCuts(height, rowBars)
        val xCuts = buildCuts(width, colBars)

        val minPanelWidth = maxOf(24, (width * 0.1f).toInt())
        val minPanelHeight = maxOf(24, (height * 0.08f).toInt())
        val minInkRatio = 0.012f

        val panels = mutableListOf<Pair<RectKey, Bitmap>>()
        for (yi in 0 until yCuts.lastIndex) {
            for (xi in 0 until xCuts.lastIndex) {
                val left = xCuts[xi]
                val right = xCuts[xi + 1]
                val top = yCuts[yi]
                val bottom = yCuts[yi + 1]

                val cellWidth = right - left
                val cellHeight = bottom - top
                if (cellWidth < minPanelWidth || cellHeight < minPanelHeight) {
                    continue
                }

                val inkRatio = estimateInkRatio(
                    pixels = pixels,
                    imageWidth = width,
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    darkThreshold = blackThreshold,
                    sampleStep = 3
                )
                if (inkRatio < minInkRatio) {
                    continue
                }

                val bitmap = Bitmap.createBitmap(source, left, top, cellWidth, cellHeight)
                panels.add(RectKey(top = top, left = left) to bitmap)
            }
        }

        if (panels.size <= 1) {
            return emptyList()
        }

        return panels
            .sortedWith(compareBy<Pair<RectKey, Bitmap>> { it.first.top }.thenBy { it.first.left })
            .map { it.second }
    }

    private fun isDarkPixel(pixel: Int, darkThreshold: Int): Boolean {
        return Color.red(pixel) <= darkThreshold &&
            Color.green(pixel) <= darkThreshold &&
            Color.blue(pixel) <= darkThreshold
    }

    private fun isBrightPixel(pixel: Int, brightThreshold: Int): Boolean {
        return Color.red(pixel) >= brightThreshold &&
            Color.green(pixel) >= brightThreshold &&
            Color.blue(pixel) >= brightThreshold
    }

    private fun collectSeparatorBands(
        ratios: FloatArray,
        ratioThreshold: Float,
        minThickness: Int,
        minEdgeDistance: Int,
        maxThickness: Int
    ): List<Band> {
        val bands = mutableListOf<Band>()
        var start = -1
        for (i in ratios.indices) {
            if (ratios[i] >= ratioThreshold) {
                if (start == -1) {
                    start = i
                }
            } else if (start != -1) {
                val end = i
                if (end - start >= minThickness) {
                    bands.add(Band(start, end))
                }
                start = -1
            }
        }
        if (start != -1 && ratios.size - start >= minThickness) {
            bands.add(Band(start, ratios.size))
        }

        return bands.filter {
            val thickness = it.end - it.start
            it.start > minEdgeDistance &&
                it.end < (ratios.size - minEdgeDistance) &&
                thickness <= maxThickness
        }
    }

    private fun mergeBands(bands: List<Band>, maxGap: Int): List<Band> {
        if (bands.isEmpty()) return emptyList()

        val sorted = bands.sortedBy { it.start }
        val merged = mutableListOf<Band>()
        var currentStart = sorted.first().start
        var currentEnd = sorted.first().end

        for (i in 1 until sorted.size) {
            val band = sorted[i]
            if (band.start <= currentEnd + maxGap) {
                currentEnd = maxOf(currentEnd, band.end)
            } else {
                merged.add(Band(currentStart, currentEnd))
                currentStart = band.start
                currentEnd = band.end
            }
        }
        merged.add(Band(currentStart, currentEnd))
        return merged
    }

    private fun buildCuts(size: Int, bars: List<Band>): List<Int> {
        val cuts = mutableListOf(0)
        bars.sortedBy { it.start }.forEach { band ->
            val center = (band.start + band.end) / 2
            if (center > cuts.last()) {
                cuts.add(center)
            }
        }
        if (cuts.last() < size) {
            cuts.add(size)
        }
        return cuts
    }

    private fun estimateInkRatio(
        pixels: IntArray,
        imageWidth: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        darkThreshold: Int,
        sampleStep: Int
    ): Float {
        var sampled = 0
        var dark = 0
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                sampled++
                if (isDarkPixel(pixels[y * imageWidth + x], darkThreshold)) {
                    dark++
                }
                x += sampleStep
            }
            y += sampleStep
        }
        if (sampled == 0) return 0f
        return dark.toFloat() / sampled
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

            val request = buildRequest(requestUrl)
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
            val request = buildRequest(requestUrl)
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

    private fun isAutoSplitPanelsEnabled(): Boolean {
        return getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .getBoolean(AppPrefs.KEY_AUTO_SPLIT_PANELS, true)
    }

    private fun isInvertSwipeDirectionEnabled(): Boolean {
        return getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .getBoolean(AppPrefs.KEY_INVERT_SWIPE_DIRECTION, false)
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

        val imageRequest = buildRequest(imageUrl)
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
        val request = buildRequest(indexUrl)
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

    private fun buildRequest(url: HttpUrl): Request {
        return Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", buildUserAgent())
            .header("Accept", DEFAULT_ACCEPT_HEADER)
            .header("Accept-Language", DEFAULT_ACCEPT_LANGUAGE_HEADER)
            .build()
    }

    private fun buildUserAgent(): String {
        val osVersion = Build.VERSION.RELEASE ?: "unknown"
        val model = Build.MODEL ?: "Android"
        return "Mozilla/5.0 (Linux; Android $osVersion; $model) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 " +
            "Droidbert/Android"
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

    private data class Band(
        val start: Int,
        val end: Int
    )

    private data class RectKey(
        val top: Int,
        val left: Int
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

}
