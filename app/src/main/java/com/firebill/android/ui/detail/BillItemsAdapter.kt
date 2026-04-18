package com.firebill.android.ui.detail

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebill.android.data.api.models.BillItem
import com.firebill.android.databinding.ItemBillEditBinding

class BillItemsAdapter(
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<BillItemsAdapter.ItemViewHolder>() {

    private val items = mutableListOf<BillItem>()

    fun setItems(newItems: List<BillItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: BillItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun getItems(): List<BillItem> = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemBillEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size

    inner class ItemViewHolder(private val binding: ItemBillEditBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var descriptionWatcher: TextWatcher? = null
        private var priceWatcher: TextWatcher? = null

        fun bind(item: BillItem, position: Int) {
            // Remove watchers before setting text to avoid feedback loops
            descriptionWatcher?.let { binding.editDescription.removeTextChangedListener(it) }
            priceWatcher?.let { binding.editPrice.removeTextChangedListener(it) }

            binding.editDescription.setText(item.description)
            binding.editPrice.setText(if (item.price != 0.0) item.price.toString() else "")

            descriptionWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_ID.toInt() && pos < items.size) {
                        items[pos] = items[pos].copy(description = s?.toString() ?: "")
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            priceWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_ID.toInt() && pos < items.size) {
                        val price = s?.toString()?.toDoubleOrNull() ?: 0.0
                        items[pos] = items[pos].copy(price = price)
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }

            binding.editDescription.addTextChangedListener(descriptionWatcher)
            binding.editPrice.addTextChangedListener(priceWatcher)

            binding.buttonRemoveItem.setOnClickListener { onRemove(adapterPosition) }
        }
    }
}
