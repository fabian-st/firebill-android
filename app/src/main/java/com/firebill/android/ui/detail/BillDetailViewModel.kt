package com.firebill.android.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.firebill.android.data.api.models.Bill
import com.firebill.android.data.api.models.BillItemRequest
import com.firebill.android.data.api.models.UpdateBillRequest
import com.firebill.android.data.repository.BillRepository
import kotlinx.coroutines.launch

class BillDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BillRepository(application)

    private val _bill = MutableLiveData<Bill>()
    val bill: LiveData<Bill> = _bill

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    fun loadBill(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getBill(id)
                .onSuccess { _bill.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun saveBill(id: Int, vendor: String?, billDate: String?, items: List<BillItemRequest>) {
        viewModelScope.launch {
            _isLoading.value = true
            val request = UpdateBillRequest(vendor = vendor, billDate = billDate, items = items)
            repository.updateBill(id, request)
                .onSuccess {
                    _bill.value = it
                    _saveSuccess.value = true
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
}
