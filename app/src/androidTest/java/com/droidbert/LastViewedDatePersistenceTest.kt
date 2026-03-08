package com.droidbert

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LastViewedDatePersistenceTest {

    private lateinit var appContext: Context

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        appContext.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString(AppPrefs.KEY_API_BASE_URL, "http://127.0.0.1:9/api/current.php")
            .apply()
    }

    @After
    fun tearDown() {
        appContext.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun lastViewedDate_persistsAcrossActivityLaunchesAndRecreation() {
        val initialDate = "1989-04-16"
        val updatedDate = "1990-01-01"

        appContext.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(AppPrefs.KEY_LAST_VIEWED_DATE, initialDate)
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val stored = activity.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
                    .getString(AppPrefs.KEY_LAST_VIEWED_DATE, null)
                assertEquals(initialDate, stored)

                activity.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(AppPrefs.KEY_LAST_VIEWED_DATE, updatedDate)
                    .commit()
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                val storedAfterRecreate = activity.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
                    .getString(AppPrefs.KEY_LAST_VIEWED_DATE, null)
                assertEquals(updatedDate, storedAfterRecreate)
            }
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val persisted = activity.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
                    .getString(AppPrefs.KEY_LAST_VIEWED_DATE, null)
                assertEquals(updatedDate, persisted)
            }
        }
    }
}
