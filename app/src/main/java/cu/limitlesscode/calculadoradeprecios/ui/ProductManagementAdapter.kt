package cu.limitlesscode.calculadoradeprecios.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import cu.limitlesscode.calculadoradeprecios.R
import cu.limitlesscode.calculadoradeprecios.data.Product
import cu.limitlesscode.calculadoradeprecios.databinding.ItemProductManagementBinding

class ProductManagementAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit,
    private val onToggleActive: (Product) -> Unit
) : ListAdapter<Product, ProductManagementAdapter.ManagementViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManagementViewHolder {
        val binding = ItemProductManagementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ManagementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ManagementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ManagementViewHolder(private val binding: ItemProductManagementBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvNameManage.text = buildDisplayName(product)
            binding.tvPriceManage.text = binding.root.context.getString(R.string.mgmt_label_price, product.precioUsd.toString())
            
            binding.tvStatus.text = if (product.isActive) 
                binding.root.context.getString(R.string.mgmt_label_status_active) 
            else 
                binding.root.context.getString(R.string.mgmt_label_status_inactive)
            binding.tvStatus.alpha = if (product.isActive) 1.0f else 0.5f

            if (product.imageUrl.isNotBlank()) {
                binding.ivProductThumb.visibility = View.VISIBLE
                binding.ivProductThumb.load(Uri.parse(product.imageUrl))
            } else {
                binding.ivProductThumb.visibility = View.GONE
            }

            if (product.garantia.isNotBlank()) {
                binding.tvGarantiaManage.visibility = View.VISIBLE
                binding.tvGarantiaManage.text = binding.root.context.getString(R.string.mgmt_label_garanty, product.garantia)
            } else {
                binding.tvGarantiaManage.visibility = View.GONE
            }

            binding.btnEdit.setOnClickListener { onEdit(product) }
            binding.btnDelete.setOnClickListener { onDelete(product) }
            binding.btnToggleActive.setOnClickListener { onToggleActive(product) }
            
            // Icono de visibilidad dinámico
            binding.btnToggleActive.setImageResource(
                if (product.isActive) android.R.drawable.ic_menu_close_clear_cancel 
                else android.R.drawable.ic_menu_view
            )
        }

        private fun buildDisplayName(product: Product): String {
            return listOf(product.equipo, product.marca, product.modelo, product.tipo)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem == newItem
    }
}
