package com.firebill.android.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.firebill.android.R
import com.firebill.android.data.api.models.Bill
import com.firebill.android.databinding.ItemBillBinding
import java.text.NumberFormat
import java.util.Locale

class BillsAdapter(
    private val onItemClick: (Bill) -> Unit,
    private val onDeleteClick: (Bill) -> Unit
) : ListAdapter<Bill, BillsAdapter.BillViewHolder>(BillDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding = ItemBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BillViewHolder(private val binding: ItemBillBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bill: Bill) {
            binding.textVendor.text = bill.vendor ?: binding.root.context.getString(R.string.unknown_vendor)
            binding.textDate.text = bill.billDate ?: "-"
            binding.textTotal.text = bill.total?.let {
                NumberFormat.getCurrencyInstance(Locale.getDefault()).format(it)
            } ?: "-"
            binding.chipExported.apply {
                if (bill.exported) {
                    setText(R.string.exported)
                    setChipBackgroundColorResource(com.google.android.material.R.color.design_default_color_primary)
                } else {
                    setText(R.string.not_exported)
                    setChipBackgroundColorResource(android.R.color.darker_gray)
                }
            }
            binding.root.setOnClickListener { onItemClick(bill) }
            binding.buttonDelete.setOnClickListener { onDeleteClick(bill) }
        }
    }

    class BillDiffCallback : DiffUtil.ItemCallback<Bill>() {
        override fun areItemsTheSame(oldItem: Bill, newItem: Bill) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Bill, newItem: Bill) = oldItem == newItem
    }
}
