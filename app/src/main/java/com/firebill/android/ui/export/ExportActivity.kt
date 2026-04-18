package com.firebill.android.ui.export

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebill.android.R
import com.firebill.android.data.api.models.ExportItemRequest
import com.firebill.android.data.api.models.ExportRequest
import com.firebill.android.databinding.ActivityExportBinding
import com.google.android.material.snackbar.Snackbar

class ExportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BILL_ID = "extra_bill_id"
    }

    private lateinit var binding: ActivityExportBinding
    private val viewModel: ExportViewModel by viewModels()
    private var exportAdapter: ExportItemsAdapter? = null
    private var billId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.export_bill)

        billId = intent.getIntExtra(EXTRA_BILL_ID, -1)
        if (billId == -1) {
            finish()
            return
        }

        setupObservers()
        binding.buttonExport.setOnClickListener { doExport() }

        viewModel.loadData(billId)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupObservers() {
        viewModel.bill.observe(this) { bill ->
            supportActionBar?.title = getString(R.string.export_bill_title, bill.vendor ?: "")
        }

        viewModel.exportOptions.observe(this) { options ->
            // Set up account spinner
            val accountNames = options.accounts.map { "${it.name} (${it.type})" }
            val accountAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountNames)
            accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerAccount.adapter = accountAdapter

            // Set up items adapter
            exportAdapter = ExportItemsAdapter(options.categories, options.tags)
            binding.recyclerExportItems.layoutManager = LinearLayoutManager(this)
            binding.recyclerExportItems.adapter = exportAdapter

            viewModel.bill.value?.items?.let { items ->
                exportAdapter?.setItems(items)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.buttonExport.isEnabled = !loading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.exportResult.observe(this) { result ->
            result ?: return@observe
            binding.cardExportSuccess.visibility = View.VISIBLE
            binding.buttonExport.isEnabled = false

            if (result.transactionUrl != null) {
                binding.buttonViewFirefly.visibility = View.VISIBLE
                binding.buttonViewFirefly.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.transactionUrl)))
                }
            }
            Snackbar.make(binding.root, R.string.export_success, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun doExport() {
        val options = viewModel.exportOptions.value ?: run {
            Snackbar.make(binding.root, R.string.error_no_options, Snackbar.LENGTH_SHORT).show()
            return
        }
        val selectedAccountIndex = binding.spinnerAccount.selectedItemPosition
        if (selectedAccountIndex < 0 || selectedAccountIndex >= options.accounts.size) {
            Snackbar.make(binding.root, R.string.error_select_account, Snackbar.LENGTH_SHORT).show()
            return
        }
        val accountId = options.accounts[selectedAccountIndex].id
        val bill = viewModel.bill.value
        val itemStates = exportAdapter?.getExportStates() ?: emptyList()

        val itemRequests = itemStates.mapNotNull { state ->
            val itemId = state.item.id ?: return@mapNotNull null
            ExportItemRequest(
                id = itemId,
                selected = state.selected,
                category = state.category,
                tags = state.tags,
                notes = state.notes
            )
        }

        val request = ExportRequest(
            sourceAccountId = accountId,
            billDate = bill?.billDate,
            items = itemRequests
        )
        viewModel.export(billId, request)
    }
}
