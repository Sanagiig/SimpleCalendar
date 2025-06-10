package com.example.simple_calendar.activities

import android.os.Bundle
import android.widget.Toast

import com.example.simple_calendar.BuildConfig
import com.example.simple_calendar.R
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.example.simple_calendar.databinding.ActivityMainBinding
import com.example.simple_calendar.dialogs.SelectEventTypesDialog
import com.example.simple_calendar.extensions.config
import com.example.simple_calendar.extensions.eventsHelper
import com.example.simple_calendar.extensions.updateWidgets
import com.example.simple_calendar.fragments.*
import com.example.simple_calendar.helpers.*
import com.example.simple_calendar.models.*
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.appLaunched
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
import java.util.Formatter

class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private var currentFragments = ArrayList<MyFragmentHolder>()

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
    }

    override fun refreshItems() {
        TODO("Not yet implemented")
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
                R.id.change_view -> showViewDialog()
                R.id.go_to_today -> goToToday()
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