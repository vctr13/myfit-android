package com.example.myfit.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import com.example.myfit.data.db.entity.FoodEntry
import com.example.myfit.data.db.model.DailyNutrition
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

    private val app = MyFitApp.from(application)

    val dates: StateFlow<List<String>> =
        app.database.foodEntryDao().getDistinctDatesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadEntries(date: String): StateFlow<List<FoodEntry>> =
        app.database.foodEntryDao().getByDate(date)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadTotals(date: String): StateFlow<DailyNutrition> =
        app.database.foodEntryDao().getDailyTotalsFlow(date)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyNutrition())

    fun deleteEntry(entry: FoodEntry) {
        viewModelScope.launch { app.database.foodEntryDao().delete(entry) }
    }
}
