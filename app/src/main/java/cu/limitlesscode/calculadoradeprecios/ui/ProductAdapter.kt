package cu.limitlesscode.calculadoradeprecios.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import cu.limitlesscode.calculadoradeprecios.data.Product
import cu.limitlesscode.calculadoradeprecios.databinding.ItemProductBinding
import java.text.DecimalFormat

class ProductAdapter(
    private var exchangeRate: Double,
    private val format: DecimalFormat,
    private val onShareClick: (Product, Double) -> Unit,
    private val onLongClick: (Product) -> Unit,
    private val onToggleSelection: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    var selectionMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    
    fun updateExchangeRate(newRate: Double) {
        exchangeRate = newRate
        notifyDataSetChanged()
    }
    
    private val selectedIds = mutableSetOf<Long>()

    fun updateSelection(ids: Set<Long>) {
        selectedIds.clear()
        selectedIds.addAll(ids)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            val precioCup = product.precioUsd * exchangeRate
            
            binding.tvName.text = buildDisplayName(product)
            binding.tvPriceUsd.text = "💵 USD: ${format.format(product.precioUsd)}"
            binding.tvPriceCup.text = "💰 CUP: ${format.format(precioCup)}"

            if (product.imageUrl.isNotBlank()) {
                binding.ivProduct.visibility = View.VISIBLE
                binding.ivProduct.load(Uri.parse(product.imageUrl))
            } else {
                binding.ivProduct.visibility = View.GONE
            }

            val hasExtra = product.garantia.isNotBlank() || product.colores.isNotBlank() || product.infoAdicional.isNotBlank()
            binding.layoutExtraInfo.visibility = if (hasExtra) View.VISIBLE else View.GONE
            
            binding.tvGarantia.visibility = if (product.garantia.isNotBlank()) View.VISIBLE else View.GONE
            binding.tvGarantia.text = "📝 ${product.garantia}"
            
            binding.tvColores.visibility = if (product.colores.isNotBlank()) View.VISIBLE else View.GONE
            binding.tvColores.text = "🌈 ${product.colores}"
            
            binding.tvInfoAdicional.visibility = if (product.infoAdicional.isNotBlank()) View.VISIBLE else View.GONE
            binding.tvInfoAdicional.text = "ℹ️ ${product.infoAdicional}"

            binding.btnShare.visibility = if (selectionMode) View.GONE else View.VISIBLE
            binding.cbSelection.visibility = if (selectionMode) View.VISIBLE else View.GONE
            binding.cbSelection.isChecked = selectedIds.contains(product.id)

            binding.btnShare.setOnClickListener { onShareClick(product, precioCup) }
            
            binding.root.setOnClickListener {
                if (selectionMode) {
                    onToggleSelection(product)
                }
            }
            
            binding.root.setOnLongClickListener {
                onLongClick(product)
                true
            }
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
