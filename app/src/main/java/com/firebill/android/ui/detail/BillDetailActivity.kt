package com.firebill.android.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.firebill.android.R
import com.firebill.android.data.api.ApiClient
import com.firebill.android.data.api.models.BillItem
import com.firebill.android.data.api.models.BillItemRequest
import com.firebill.android.databinding.ActivityBillDetailBinding
import com.firebill.android.ui.export.ExportActivity
import com.google.android.material.snackbar.Snackbar

class BillDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BILL_ID = "extra_bill_id"
    }

    private lateinit var binding: ActivityBillDetailBinding
    private val viewModel: BillDetailViewModel by viewModels()
    private lateinit var itemsAdapter: BillItemsAdapter
    private var billId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        billId = intent.getIntExtra(EXTRA_BILL_ID, -1)
        if (billId == -1) {
            finish()
            return
        }

        setupRecyclerView()
        setupObservers()

        binding.buttonSave.setOnClickListener { saveBill() }
        binding.buttonAddItem.setOnClickListener { addNewItem() }
        binding.buttonExport.setOnClickListener {
            startActivity(Intent(this, ExportActivity::class.java).apply {
                putExtra(ExportActivity.EXTRA_BILL_ID, billId)
            })
        }

        viewModel.loadBill(billId)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupRecyclerView() {
        itemsAdapter = BillItemsAdapter(onRemove = { position ->
            val items = itemsAdapter.getItems().toMutableList()
            items.removeAt(position)
            itemsAdapter.setItems(items)
        })
        binding.recyclerItems.layoutManager = LinearLayoutManager(this)
        binding.recyclerItems.adapter = itemsAdapter
    }

    private fun setupObservers() {
        viewModel.bill.observe(this) { bill ->
            supportActionBar?.title = bill.vendor ?: getString(R.string.bill_details)
            binding.editVendor.setText(bill.vendor ?: "")
            binding.editDate.setText(bill.billDate ?: "")

            bill.imageUrl?.let { path ->
                val baseUrl = ApiClient.getServerUrl(this)
                val fullUrl = if (path.startsWith("http")) path else "$baseUrl$path".trimEnd('/')
                Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(binding.imageBill)
                binding.imageBill.visibility = View.VISIBLE
            }

            itemsAdapter.setItems(bill.items)

            if (bill.fireflyTransactionUrl != null) {
                binding.buttonViewTransaction.visibility = View.VISIBLE
                binding.buttonViewTransaction.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bill.fireflyTransactionUrl)))
                }
            } else {
                binding.buttonViewTransaction.visibility = View.GONE
            }

            binding.chipExportStatus.apply {
                visibility = View.VISIBLE
                if (bill.exported) {
                    setText(R.string.exported)
                    setChipBackgroundColorResource(com.google.android.material.R.color.design_default_color_primary)
                } else {
                    setText(R.string.not_exported)
                    setChipBackgroundColorResource(android.R.color.darker_gray)
                }
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.buttonSave.isEnabled = !loading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.saveSuccess.observe(this) { success ->
            if (success == true) {
                Snackbar.make(binding.root, R.string.saved_successfully, Snackbar.LENGTH_SHORT).show()
                viewModel.clearSaveSuccess()
            }
        }
    }

    private fun saveBill() {
        val vendor = binding.editVendor.text?.toString()?.takeIf { it.isNotBlank() }
        val date = binding.editDate.text?.toString()?.takeIf { it.isNotBlank() }
        val itemRequests = itemsAdapter.getItems().map { item ->
            BillItemRequest(id = item.id, description = item.description, price = item.price)
        }
        viewModel.saveBill(billId, vendor, date, itemRequests)
    }

    private fun addNewItem() {
        itemsAdapter.addItem(BillItem(id = null, billId = billId, position = null, description = "", price = 0.0))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bill_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                startActivity(Intent(this, ExportActivity::class.java).apply {
                    putExtra(ExportActivity.EXTRA_BILL_ID, billId)
                })
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
