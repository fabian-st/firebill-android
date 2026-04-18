package com.firebill.android.ui.export

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebill.android.data.api.models.BillItem
import com.firebill.android.data.api.models.FireflyCategory
import com.firebill.android.data.api.models.FireflyTag
import com.firebill.android.databinding.ItemExportBinding

data class ExportItemState(
    val item: BillItem,
    var selected: Boolean = true,
    var category: String? = null,
    var tags: MutableList<String> = mutableListOf(),
    var notes: String? = null
)

class ExportItemsAdapter(
    private val categories: List<FireflyCategory>,
    private val tags: List<FireflyTag>
) : RecyclerView.Adapter<ExportItemsAdapter.ExportViewHolder>() {

    private val exportItems = mutableListOf<ExportItemState>()

    fun setItems(items: List<BillItem>) {
        exportItems.clear()
        exportItems.addAll(items.map { ExportItemState(it) })
        notifyDataSetChanged()
    }

    fun getExportStates(): List<ExportItemState> = exportItems.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExportViewHolder {
        val binding = ItemExportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExportViewHolder, position: Int) {
        holder.bind(exportItems[position], position)
    }

    override fun getItemCount() = exportItems.size

    inner class ExportViewHolder(private val binding: ItemExportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(state: ExportItemState, position: Int) {
            binding.checkboxItem.isChecked = state.selected
            binding.checkboxItem.text = "${state.item.description}  €${"%.2f".format(state.item.price)}"
            binding.editNotes.setText(state.notes ?: "")

            // Category spinner
            val categoryNames = listOf("") + categories.map { it.name }
            val spinnerAdapter = android.widget.ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_spinner_item,
                categoryNames
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            binding.spinnerCategory.adapter = spinnerAdapter

            val selectedIndex = categories.indexOfFirst { it.name == state.category }.let {
                if (it == -1) 0 else it + 1
            }
            binding.spinnerCategory.setSelection(selectedIndex)

            binding.checkboxItem.setOnCheckedChangeListener { _, checked ->
                exportItems[adapterPosition].selected = checked
            }

            binding.spinnerCategory.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?,
                        view: android.view.View?,
                        pos: Int,
                        id: Long
                    ) {
                        exportItems[adapterPosition].category =
                            if (pos == 0) null else categories[pos - 1].name
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }

            binding.editNotes.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    exportItems[adapterPosition].notes =
                        binding.editNotes.text?.toString()?.takeIf { it.isNotBlank() }
                }
            }
        }
    }
}
