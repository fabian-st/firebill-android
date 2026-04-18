package com.firebill.android.ui.export

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.firebill.android.data.api.models.*
import com.firebill.android.data.repository.BillRepository
import kotlinx.coroutines.launch

class ExportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BillRepository(application)

    private val _bill = MutableLiveData<Bill>()
    val bill: LiveData<Bill> = _bill

    private val _exportOptions = MutableLiveData<ExportOptions>()
    val exportOptions: LiveData<ExportOptions> = _exportOptions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _exportResult = MutableLiveData<ExportResponse?>()
    val exportResult: LiveData<ExportResponse?> = _exportResult

    fun loadData(billId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getBill(billId)
                .onSuccess { _bill.value = it }
                .onFailure { _error.value = it.message }

            repository.getExportOptions(billId)
                .onSuccess { _exportOptions.value = it }
                .onFailure { _error.value = it.message }

            _isLoading.value = false
        }
    }

    fun export(billId: Int, request: ExportRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.exportBill(billId, request)
                .onSuccess { _exportResult.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
