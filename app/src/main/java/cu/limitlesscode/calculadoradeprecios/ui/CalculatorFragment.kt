package cu.limitlesscode.calculadoradeprecios.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import cu.limitlesscode.calculadoradeprecios.MainViewModel
import cu.limitlesscode.calculadoradeprecios.buildShareMessage
import cu.limitlesscode.calculadoradeprecios.createDecimalFormat
import cu.limitlesscode.calculadoradeprecios.data.Product
import cu.limitlesscode.calculadoradeprecios.databinding.FragmentHomeBinding
import cu.limitlesscode.calculadoradeprecios.launchShareIntent
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class CalculatorFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: ProductAdapter
    private val format: DecimalFormat by lazy { createDecimalFormat() }

    private var selectionMode = false
    private var selectionAction: String? = null
    private val selectedIds = mutableSetOf<Long>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupInputs()
        setupButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            exchangeRate = viewModel.exchangeRate.value,
            format = format,
            onShareClick = { product, precioCup ->
                launchShareIntent(requireContext(), product, viewModel.exchangeRate.value, format)
            },
            onLongClick = { product ->
                enterSelectionMode(product.id)
            },
            onToggleSelection = { product ->
                toggleSelection(product.id)
            }
        )
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter
    }

    private fun setupInputs() {
        binding.etExchangeRate.setText(viewModel.exchangeRate.value.toString().replace('.', ','))
        binding.etExchangeRate.addTextChangedListener { text ->
            val rate = text.toString().replace(',', '.').toDoubleOrNull()
            if (rate != null) {
                viewModel.updateExchangeRate(rate)
            }
        }

        binding.etSearch.setText(viewModel.searchQuery.value)
        binding.etSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text.toString())
        }
    }

    private fun setupButtons() {

        binding.btnSelectAll.setOnClickListener {
            val currentList = adapter.currentList
            selectedIds.clear()
            selectedIds.addAll(currentList.map { it.id })
            adapter.updateSelection(selectedIds)
            updateFab()
        }

        binding.fabAction.setOnClickListener {
            val selectedProducts = adapter.currentList.filter { selectedIds.contains(it.id) }
            if (selectionAction == "hide") {
                selectedProducts.forEach { viewModel.saveProduct(it.copy(isActive = false)) }
                exitSelectionMode()
            } else if (selectionAction == "share") {
                viewModel.startMultipleSharing(selectedProducts)
                val first = viewModel.consumeNextProduct()
                if (first != null) {
                    launchShareIntent(requireContext(), first, viewModel.exchangeRate.value, format)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.products.collect { products ->
                        val visible = products.filter { it.isActive }
                        adapter.submitList(visible)
                        binding.tvEmpty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.exchangeRate.collect { rate ->
                        // Evitar bucle de actualización si ya es el mismo
                        val currentText = binding.etExchangeRate.text.toString().replace(',', '.')
                        if (currentText.toDoubleOrNull() != rate) {
                            binding.etExchangeRate.setText(rate.toString().replace('.', ','))
                        }
                    }
                }
            }
        }
    }

    private fun enterSelectionMode(firstId: Long? = null) {
        selectionMode = true
        adapter.selectionMode = true
        binding.btnSelectAll.visibility = View.VISIBLE
        binding.tvListTitle.text = if (selectionAction == "hide") "Selecciona para ocultar" else "Selecciona para compartir"
        
        if (firstId != null) {
            selectionAction = "share"
            toggleSelection(firstId)
        }
        updateFab()
    }

    private fun exitSelectionMode() {
        selectionMode = false
        adapter.selectionMode = false
        selectedIds.clear()
        adapter.updateSelection(selectedIds)
        binding.btnSelectAll.visibility = View.GONE
        binding.tvListTitle.text = "Productos activos"
        binding.fabAction.visibility = View.GONE
        selectionAction = null
        viewModel.cancelSharing()
    }

    private fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        adapter.updateSelection(selectedIds)
        updateFab()
    }

    private fun updateFab() {
        if (selectedIds.isNotEmpty()) {
            binding.fabAction.visibility = View.VISIBLE
            val icon = if (selectionAction == "share") android.R.drawable.ic_menu_share else android.R.drawable.ic_menu_view
            binding.fabAction.setImageResource(icon)
        } else {
            binding.fabAction.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // Lógica de compartir secuencial al regresar al fragmento
        if (viewModel.isSharingActive()) {
            val product = viewModel.consumeNextProduct()
            if (product != null) {
                launchShareIntent(requireContext(), product, viewModel.exchangeRate.value, format)
            } else {
                exitSelectionMode()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
