package com.droidbert

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class SettingsActivity : AppCompatActivity() {

    private lateinit var apiInputLayout: TextInputLayout
    private lateinit var apiInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.settings_toolbar)
        applyToolbarWindowInsets(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        apiInputLayout = findViewById(R.id.api_input_layout)
        apiInput = findViewById(R.id.api_input)
        val saveButton = findViewById<MaterialButton>(R.id.save_button)

        apiInput.setText(getStoredApiBaseUrl())

        saveButton.setOnClickListener {
            saveApiBaseUrl()
        }
    }

    private fun saveApiBaseUrl() {
        val url = apiInput.text?.toString()?.trim().orEmpty()
        if (url.isBlank() || url.toHttpUrlOrNull() == null) {
            apiInputLayout.error = getString(R.string.invalid_api_url)
            return
        }

        val normalizedUrl = ApiUrlUtils.normalizeApiBaseUrl(url)

        apiInputLayout.error = null
        getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(AppPrefs.KEY_API_BASE_URL, normalizedUrl)
            .apply()

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun getStoredApiBaseUrl(): String {
        return getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .getString(AppPrefs.KEY_API_BASE_URL, getString(R.string.api_base_url_default))
            .orEmpty()
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
}
