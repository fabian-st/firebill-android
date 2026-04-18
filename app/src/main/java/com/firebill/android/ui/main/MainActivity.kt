package com.firebill.android.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebill.android.R
import com.firebill.android.data.api.models.Bill
import com.firebill.android.databinding.ActivityMainBinding
import com.firebill.android.ui.capture.CaptureActivity
import com.firebill.android.ui.detail.BillDetailActivity
import com.firebill.android.ui.settings.SettingsActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: BillsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, CaptureActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadBills()
    }

    private fun setupRecyclerView() {
        adapter = BillsAdapter(
            onItemClick = { bill -> openBillDetail(bill) },
            onDeleteClick = { bill -> confirmDelete(bill) }
        )
        binding.recyclerBills.layoutManager = LinearLayoutManager(this)
        binding.recyclerBills.adapter = adapter

        // Swipe-to-delete
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val bill = adapter.currentList[viewHolder.adapterPosition]
                confirmDelete(bill) {
                    // If cancelled, notify adapter to restore item
                    adapter.notifyItemChanged(viewHolder.adapterPosition)
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerBills)
    }

    private fun setupObservers() {
        viewModel.bills.observe(this) { bills ->
            adapter.submitList(bills)
            binding.textEmpty.visibility = if (bills.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.pendingCount.observe(this) { count ->
            binding.textPendingBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
            binding.textPendingBadge.text = getString(R.string.pending_uploads, count)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadBills()
        }
    }

    private fun openBillDetail(bill: Bill) {
        val intent = Intent(this, BillDetailActivity::class.java)
        intent.putExtra(BillDetailActivity.EXTRA_BILL_ID, bill.id)
        startActivity(intent)
    }

    private fun confirmDelete(bill: Bill, onCancel: (() -> Unit)? = null) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_bill)
            .setMessage(getString(R.string.delete_bill_confirm, bill.vendor ?: getString(R.string.unknown_vendor)))
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteBill(bill.id) }
            .setNegativeButton(R.string.cancel) { _, _ -> onCancel?.invoke() }
            .setOnCancelListener { onCancel?.invoke() }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                viewModel.loadBills()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
