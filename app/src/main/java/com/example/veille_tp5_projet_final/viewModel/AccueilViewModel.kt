package com.example.veille_tp5_projet_final.viewModel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veille_tp5_projet_final.database.ObjectifRecord
import com.example.veille_tp5_projet_final.database.StepDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccueilViewModel(private val application: Application) : ViewModel() {
    private val database = StepDatabase.getDatabase(application)
    private val stepDao = database.stepDao()

    private val _objectif = MutableStateFlow(6000)
    val objectif: StateFlow<Int> = _objectif

    fun fetchObjectifForToday(today: String) {
        viewModelScope.launch {
            try {
                val objectifToday = stepDao.getObjectifForDate(today) ?: 6000
                _objectif.value = objectifToday
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateObjectif(today: String, newObjectif: Int) {
        viewModelScope.launch {
            try {
                stepDao.insertOrUpdateObjectif(ObjectifRecord(today, newObjectif))
                _objectif.value = newObjectif
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun areNotificationsEnabled(): Boolean {
        val sharedPreferences = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("notifications_enabled", true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        val sharedPreferences = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("notifications_enabled", enabled).apply()
    }
}
