package com.firebill.android.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.firebill.android.data.api.models.Bill
import com.firebill.android.data.repository.BillRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BillRepository(application)

    private val _bills = MutableLiveData<List<Bill>>()
    val bills: LiveData<List<Bill>> = _bills

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    val pendingCount: LiveData<Int> = repository.getPendingCount()

    init {
        loadBills()
    }

    fun loadBills() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getBills()
                .onSuccess { _bills.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteBill(id: Int) {
        viewModelScope.launch {
            repository.deleteBill(id)
                .onSuccess { loadBills() }
                .onFailure { _error.value = it.message }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
