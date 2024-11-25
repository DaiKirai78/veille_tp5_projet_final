package com.example.veille_tp5_projet_final.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.veille_tp5_projet_final.viewModel.AccueilViewModel

class AccueilViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccueilViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccueilViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}