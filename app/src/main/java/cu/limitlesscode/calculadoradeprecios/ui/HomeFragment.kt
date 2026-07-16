package cu.limitlesscode.calculadoradeprecios.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cu.limitlesscode.calculadoradeprecios.FilterField
import cu.limitlesscode.calculadoradeprecios.MainViewModel
import cu.limitlesscode.calculadoradeprecios.R
import cu.limitlesscode.calculadoradeprecios.SortField
import cu.limitlesscode.calculadoradeprecios.createDecimalFormat
import cu.limitlesscode.calculadoradeprecios.data.Product
import cu.limitlesscode.calculadoradeprecios.databinding.FragmentHomeBinding
import cu.limitlesscode.calculadoradeprecios.launchOverlayBatchShareIntent
import cu.limitlesscode.calculadoradeprecios.launchPdfCatalogShareIntent
import cu.limitlesscode.calculadoradeprecios.launchShareIntent
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class HomeFragment : Fragment() {

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
                launchShareIntent(
                    requireContext(), 
                    product, 
                    viewModel.exchangeRate.value, 
                    format,
                    viewModel.whatsappNumber.value
                )
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
        binding.etSearch.setText(viewModel.searchQuery.value)
        binding.etSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text.toString())
        }
    }

    private fun setupButtons() {
        binding.btnFilter.setOnClickListener { showFilterDialog() }
        binding.btnSort.setOnClickListener { showSortDialog() }

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
                showShareOptionsDialog(selectedProducts)
            }
        }
    }

    private fun showShareOptionsDialog(products: List<Product>) {
        val options = arrayOf(
            getString(R.string.share_option_individual),
            getString(R.string.share_option_overlay),
            getString(R.string.share_option_pdf_catalog)
        )

        val target = viewModel.whatsappNumber.value

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.share_dialog_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Individual
                        viewModel.startMultipleSharing(products)
                        val first = viewModel.consumeNextProduct()
                        if (first != null) {
                            launchShareIntent(requireContext(), first, viewModel.exchangeRate.value, format, target)
                        }
                    }
                    1 -> { // Overlay (Lote con Info)
                        viewLifecycleOwner.lifecycleScope.launch {
                            launchOverlayBatchShareIntent(requireContext(), products, viewModel.exchangeRate.value, format, target)
                            exitSelectionMode()
                        }
                    }
                    2 -> { // PDF Catalog
                        viewLifecycleOwner.lifecycleScope.launch {
                            launchPdfCatalogShareIntent(requireContext(), products, viewModel.exchangeRate.value, format, target)
                            exitSelectionMode()
                        }
                    }
                }
            }
            .show()
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
                        adapter.updateExchangeRate(rate)
                    }
                }
            }
        }
    }

    private fun enterSelectionMode(firstId: Long? = null) {
        selectionMode = true
        adapter.selectionMode = true
        binding.btnSelectAll.visibility = View.VISIBLE
  //      binding.tvListTitle.text = if (selectionAction == "hide") getString(R.string.home_selection_hide) else getString(R.string.home_selection_share)
        
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
       // binding.tvListTitle.text = getString(R.string.home_list_active)
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

    private fun showFilterDialog() {
        val options = arrayOf(
            getString(R.string.filter_all),
            getString(R.string.filter_equipo),
            getString(R.string.filter_brand),
            getString(R.string.filter_model),
            getString(R.string.filter_type)
        )
        val fields = FilterField.values()
        val current = fields.indexOf(viewModel.filterField.value)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_filter)
            .setSingleChoiceItems(options, current) { dialog, which ->
                viewModel.setFilterField(fields[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf(
            getString(R.string.sort_equipo_asc),
            getString(R.string.sort_equipo_desc),
            getString(R.string.sort_brand_asc),
            getString(R.string.sort_brand_desc),
            getString(R.string.sort_model),
            getString(R.string.sort_type),
            getString(R.string.sort_price_asc),
            getString(R.string.sort_price_desc)
        )

        // Map index to (Field, Ascending)
        val sortConfigs = arrayOf(
            SortField.EQUIPO to true,
            SortField.EQUIPO to false,
            SortField.MARCA to true,
            SortField.MARCA to false,
            SortField.MODELO to true,
            SortField.TIPO to true,
            SortField.PRECIO to true,
            SortField.PRECIO to false
        )

        // Find current selection
        var current = -1
        for (i in sortConfigs.indices) {
            if (sortConfigs[i].first == viewModel.sortField.value && 
                sortConfigs[i].second == viewModel.sortAscending.value) {
                current = i
                break
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_sort)
            .setSingleChoiceItems(options, current) { dialog, which ->
                val (field, asc) = sortConfigs[which]
                viewModel.setSort(field, asc)
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Lógica de compartir secuencial al regresar al fragmento
        if (viewModel.isSharingActive()) {
            val product = viewModel.consumeNextProduct()
            if (product != null) {
                launchShareIntent(
                    requireContext(), 
                    product, 
                    viewModel.exchangeRate.value, 
                    format,
                    viewModel.whatsappNumber.value
                )
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
