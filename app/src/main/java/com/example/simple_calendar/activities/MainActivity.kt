package com.example.simple_calendar.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast

import com.example.simple_calendar.BuildConfig
import com.example.simple_calendar.R
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.example.simple_calendar.databinding.ActivityMainBinding
import com.example.simple_calendar.dialogs.SelectEventTypesDialog
import com.example.simple_calendar.extensions.config
import com.example.simple_calendar.extensions.eventsHelper
import com.example.simple_calendar.extensions.getFirstDayOfWeek
import com.example.simple_calendar.extensions.updateWidgets
import com.example.simple_calendar.fragments.*
import com.example.simple_calendar.helpers.*
import com.example.simple_calendar.helpers.Formatter
import com.example.simple_calendar.models.*
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.appLaunched
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.fadeOut
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.isVisible
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.commons.models.RadioItem
import org.joda.time.DateTime
import java.util.ArrayList
import java.util.ConcurrentModificationException


class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private var currentFragments = ArrayList<MyFragmentHolder>()

    private var goToTodayButton: MenuItem? = null

    private var shouldGoToTodayBeVisible = false
    private var mShouldFilterBeVisible = false

    // search results have endless scrolling, so reaching the top/bottom fetches further results
    private var minFetchedSearchTS = 0L
    private var maxFetchedSearchTS = 0L
    private var searchResultEvents = ArrayList<Event>()
    private var bottomItemAtRefresh: ListItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()
        updateMaterialActivityViews(binding.mainCoordinator, binding.mainHolder, useTransparentNavigation = false, useTopSearchMenu = true)

        binding.calendarFab.beVisibleIf(config.storedView != YEARLY_VIEW && config.storedView != WEEKLY_VIEW)
    }

    override fun refreshItems() {
        TODO("Not yet implemented")
    }

    fun refreshMenuItems() {
        if (binding.fabExtendedOverlay.isVisible()) {
            hideExtendedFab()
        }

        shouldGoToTodayBeVisible = currentFragments.lastOrNull()?.shouldGoToTodayBeVisible() ?: false
        binding.mainMenu.getToolbar().menu.apply {
            goToTodayButton = findItem(R.id.go_to_today)
            findItem(R.id.print).isVisible = config.storedView != MONTHLY_DAILY_VIEW
            findItem(R.id.filter).isVisible = mShouldFilterBeVisible
            findItem(R.id.go_to_today).isVisible = shouldGoToTodayBeVisible && !binding.mainMenu.isSearchOpen
            findItem(R.id.go_to_date).isVisible = config.storedView != EVENTS_LIST_VIEW
            findItem(R.id.refresh_caldav_calendars).isVisible = config.caldavSync
            findItem(R.id.more_apps_from_us).isVisible = !resources.getBoolean(com.simplemobiletools.commons.R.bool.hide_google_relations)
        }
    }

    private fun setupOptionsMenu() = binding.apply {
        mainMenu.getToolbar().inflateMenu(R.menu.menu_main)
        mainMenu.toggleHideOnScroll(false)
        mainMenu.setupMenu()

        mainMenu.onSearchTextChangedListener = { text ->
            searchQueryChanged(text)
        }

        mainMenu.getToolbar().setOnMenuItemClickListener { menuItem ->
            if (fabExtendedOverlay.isVisible()) {
                hideExtendedFab()
            }

            when (menuItem.itemId) {
                R.id.go_to_today -> goToToday()
                R.id.change_view -> showViewDialog()
                R.id.go_to_date -> showGoToDateDialog()
                R.id.print -> printView()
                R.id.filter -> showFilterDialog()
//                R.id.refresh_caldav_calendars -> refreshCalDAVCalendars(true)
//                R.id.add_holidays -> addHolidays()
//                R.id.add_birthdays -> tryAddBirthdays()
//                R.id.add_anniversaries -> tryAddAnniversaries()
//                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
//                R.id.settings -> launchSettings()
//                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun setupQuickFilter() {

    }

    private fun checkSwipeRefreshAvailability() {
        binding.swipeRefreshLayout.isEnabled = config.caldavSync && config.pullToRefresh && config.storedView != WEEKLY_VIEW
        if (!binding.swipeRefreshLayout.isEnabled) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun getDateCodeToDisplay(newView: Int): String? {
        val fragment = currentFragments.last()
        val currentView = fragment.viewType
        if (newView == EVENTS_LIST_VIEW || currentView == EVENTS_LIST_VIEW) {
            return null
        }

        val fragmentDate = fragment.getCurrentDate()
        val viewOrder = arrayListOf(DAILY_VIEW, WEEKLY_VIEW, MONTHLY_VIEW, YEARLY_VIEW)
        val currentViewIndex = viewOrder.indexOf(if (currentView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else currentView)
        val newViewIndex = viewOrder.indexOf(if (newView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else newView)

        return if (fragmentDate != null && currentViewIndex <= newViewIndex) {
            getDateCodeFormatForView(newView, fragmentDate)
        } else {
            getDateCodeFormatForView(newView, DateTime())
        }
    }

    private fun getDateCodeFormatForView(view: Int, date: DateTime): String {
        return when (view) {
            WEEKLY_VIEW -> getFirstDayOfWeek(date)
            YEARLY_VIEW -> date.toString()
            else -> Formatter.getDayCodeFromDateTime(date)
        }
    }

    private fun getFragmentsHolder() = when (config.storedView) {
        DAILY_VIEW -> DayFragmentsHolder()
        MONTHLY_VIEW -> MonthFragmentsHolder()
        MONTHLY_DAILY_VIEW -> MonthDayFragmentsHolder()
        YEARLY_VIEW -> YearFragmentsHolder()
        EVENTS_LIST_VIEW -> EventListFragment()
        else -> WeekFragmentsHolder()
    }

    private fun searchQueryChanged(text: String) {

    }

    private fun showViewDialog() {
        val items = arrayListOf(
            RadioItem(DAILY_VIEW, getString(R.string.daily_view)),
            RadioItem(WEEKLY_VIEW, getString(R.string.weekly_view)),
            RadioItem(MONTHLY_VIEW, getString(R.string.monthly_view)),
            RadioItem(MONTHLY_DAILY_VIEW, getString(R.string.monthly_daily_view)),
            RadioItem(YEARLY_VIEW, getString(R.string.yearly_view)),
            RadioItem(EVENTS_LIST_VIEW, getString(R.string.simple_event_list))
        )

        RadioGroupDialog(this, items, config.storedView) {
            resetActionBarTitle()
            closeSearch()
            updateView(it as Int)
            shouldGoToTodayBeVisible = false
            refreshMenuItems()
        }
    }

    private fun updateView(view: Int) {
        binding.calendarFab.beVisibleIf(view != YEARLY_VIEW && view != WEEKLY_VIEW)
        val dateCode = getDateCodeToDisplay(view)
        config.storedView = view
        checkSwipeRefreshAvailability()
        updateViewPager(dateCode)
        if (goToTodayButton?.isVisible == true) {
            shouldGoToTodayBeVisible = false
            refreshMenuItems()
        }
    }

    private fun goToToday() {
        currentFragments.last().goToToday()
    }

    fun showGoToDateDialog() {
        currentFragments.last().showGoToDateDialog()
    }

    private fun printView() {
        currentFragments.last().printView()
    }

    private fun resetActionBarTitle() {
        binding.mainMenu.updateHintText(getString(com.simplemobiletools.commons.R.string.search))
    }

    private fun showFilterDialog() {
        SelectEventTypesDialog(this, config.displayEventTypes) {
            if (config.displayEventTypes != it) {
                config.displayEventTypes = it

                refreshViewPager()
                setupQuickFilter()
                updateWidgets()
            }
        }
    }

    private fun updateViewPager(dayCode: String? = null) {
        val fragment = getFragmentsHolder()
        currentFragments.forEach {
            try {
                supportFragmentManager.beginTransaction().remove(it).commitNow()
            } catch (ignored: Exception) {
                return
            }
        }

        currentFragments.clear()
        currentFragments.add(fragment)
        val bundle = Bundle()
        val fixedDayCode = fixDayCode(dayCode)

        when (config.storedView) {
            DAILY_VIEW -> bundle.putString(DAY_CODE, fixedDayCode ?: Formatter.getTodayCode())
            WEEKLY_VIEW -> bundle.putString(
                WEEK_START_DATE_TIME,
                fixedDayCode ?: getFirstDayOfWeek(DateTime())
            )

            MONTHLY_VIEW, MONTHLY_DAILY_VIEW -> bundle.putString(
                DAY_CODE,
                fixedDayCode ?: Formatter.getTodayCode()
            )

            YEARLY_VIEW -> bundle.putString(YEAR_TO_OPEN, fixedDayCode)
        }

        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        binding.mainMenu.toggleForceArrowBackIcon(false)
    }

    private fun fixDayCode(dayCode: String? = null): String? = when {
        config.storedView == WEEKLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> getFirstDayOfWeek(
            Formatter.getDateTimeFromCode(dayCode))
        config.storedView == YEARLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> Formatter.getYearFromDayCode(dayCode)
        else -> dayCode
    }

    private fun refreshViewPager() {
        runOnUiThread {
            if (!isDestroyed) {
                currentFragments.last().refreshEvents()
            }
        }
    }

    private fun hideExtendedFab() {
        animateFabIcon(true)
        binding.apply {
            arrayOf(fabEventLabel, fabExtendedOverlay, fabTaskIcon, fabTaskLabel).forEach {
                it.fadeOut()
            }
        }
    }

    private fun closeSearch() {
        binding.mainMenu.closeSearch()
        minFetchedSearchTS = 0L
        maxFetchedSearchTS = 0L
        searchResultEvents.clear()
        bottomItemAtRefresh = null
    }

    private fun animateFabIcon(showPlus: Boolean) {
        val newDrawableId = if (showPlus) {
            com.simplemobiletools.commons.R.drawable.ic_plus_vector
        } else {
            R.drawable.ic_today_vector
        }
        val newDrawable =
            resources.getColoredDrawableWithColor(newDrawableId, getProperPrimaryColor())
        binding.calendarFab.setImageDrawable(newDrawable)
    }

}