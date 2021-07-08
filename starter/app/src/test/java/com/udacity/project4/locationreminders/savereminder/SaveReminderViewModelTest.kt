package com.udacity.project4.locationreminders.savereminder

import MainCoroutineRule
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.android.architecture.blueprints.todoapp.getOrAwaitValue
import org.hamcrest.MatcherAssert.assertThat
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.core.Is.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O])
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {


    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var fakeDataSource: FakeDataSource

    // Executes each task synchronously using Architecture Components.
    // This rule ensures that the test results happen synchronously and in a repeatable order.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )
    }

    private fun getReminder(): ReminderDataItem {
        return ReminderDataItem(
            title = "title",
            description = "description",
            location = "location",
            latitude = 44.3837422503473,
            longitude = 26.11452427588019)
    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest{

        val reminder = getReminder()

        // The loading animation appeared.
        mainCoroutineRule.pauseDispatcher()

        saveReminderViewModel.validateAndSaveReminder(reminder)
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // The loading animation disappeared.
        mainCoroutineRule.resumeDispatcher()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun saveReminder() = mainCoroutineRule.runBlockingTest {
        val reminder = getReminder()
        saveReminderViewModel.saveReminder(reminder)
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue(), `is`("Reminder Saved !"))
    }

    @Test
    fun saveReminder_noTitle() = mainCoroutineRule.runBlockingTest {
        val reminder = ReminderDataItem(
            title = "",
            description = "description",
            location = "location",
            latitude = 8.9806,
            longitude = 38.7578)

        saveReminderViewModel.validateAndSaveReminder(reminder)
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), notNullValue())
    }

    @Test
    fun saveReminder_noLocation() = mainCoroutineRule.runBlockingTest {
        val reminder = ReminderDataItem(
            title = "title",
            description = "description",
            location = "",
            latitude = 44.3837422503473,
            longitude = 26.11452427588019)

        saveReminderViewModel.validateAndSaveReminder(reminder)
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), notNullValue())

    }


}