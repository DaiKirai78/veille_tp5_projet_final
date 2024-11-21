package com.example.veille_tp5_projet_final.viewModel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel

class PodometreViewModel : ViewModel() {
    private val _stepCount = mutableStateOf(0)
    val stepCount: State<Int> = _stepCount

    fun updateStepCount(newStepCount: Int) {
        _stepCount.value = newStepCount
    }
}