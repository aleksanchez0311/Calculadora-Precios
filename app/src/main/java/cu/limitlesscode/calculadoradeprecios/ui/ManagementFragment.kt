package cu.limitlesscode.calculadoradeprecios.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import cu.limitlesscode.calculadoradeprecios.MainViewModel
import cu.limitlesscode.calculadoradeprecios.data.BackupManager
import cu.limitlesscode.calculadoradeprecios.data.Product
import cu.limitlesscode.calculadoradeprecios.databinding.FragmentManagementBinding
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ManagementFragment : Fragment() {

    private var _binding: FragmentManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: ProductManagementAdapter
    private var currentProduct: Product? = null
    private var imageUri: String = ""
    private var activeView = "products"

    private val backupManager by lazy { BackupManager(requireContext()) }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {}
            imageUri = it.toString()
            updateImagePreview()
        }
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val success = backupManager.exportBackup(viewModel.products.value, it)
                if (success) {
                    Toast.makeText(requireContext(), "Respaldo guardado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val mimeType = requireContext().contentResolver.getType(it)
                val restored = if (mimeType == "application/zip") {
                    backupManager.importBackup(it)
                } else {
                    val text = requireContext().contentResolver.openInputStream(it)?.bufferedReader().use { r -> r?.readText() }
                    if (!text.isNullOrBlank()) {
                        val json = JSONObject(text)
                        val items = json.optJSONArray("products") ?: JSONArray()
                        val list = mutableListOf<Product>()
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            list.add(Product(
                                id = item.optLong("id", 0L),
                                equipo = item.optString("equipo", ""),
                                marca = item.optString("marca", ""),
                                modelo = item.optString("modelo", ""),
                                tipo = item.optString("tipo", ""),
                                precioUsd = item.optDouble("precioUsd", 0.0),
                                isActive = item.optBoolean("isActive", true),
                                imageUrl = item.optString("imageUrl", ""),
                                garantia = item.optString("garantia", ""),
                                colores = item.optString("colores", ""),
                                infoAdicional = item.optString("infoAdicional", "")
                            ))
                        }
                        list
                    } else null
                }
                restored?.forEach { p -> viewModel.saveProduct(p) }
                if (restored != null) Toast.makeText(requireContext(), "Importación completada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupForm()
        setupNavButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ProductManagementAdapter(
            onEdit = { product -> editProduct(product) },
            onDelete = { product -> viewModel.deleteProduct(product) },
            onToggleActive = { product -> viewModel.saveProduct(product.copy(isActive = !product.isActive)) }
        )
        binding.rv_management.layoutManager = LinearLayoutManager(requireContext())
        binding.rv_management.adapter = adapter
    }

    private fun setupForm() {
        binding.layout_image_picker.setOnClickListener {
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btn_change_image.setOnClickListener {
            imageUri = ""
            updateImagePreview()
        }
        binding.btn_save.setOnClickListener { saveProduct() }
        binding.btn_cancel.setOnClickListener { resetForm() }
        
        binding.btn_use_usd.setOnClickListener {
            val cup = binding.et_cup_calc.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
            val rate = viewModel.exchangeRate.value
            if (rate > 0) {
                val usd = cup / rate
                binding.et_precio.setText(String.format("%.2f", usd).replace('.', ','))
            }
        }
    }

    private fun setupNavButtons() {
        binding.btn_view_products.setOnClickListener { switchView("products") }
        binding.btn_view_backup.setOnClickListener { switchView("backup") }
        binding.btn_view_disabled.setOnClickListener { switchView("disabled") }
        
        binding.btn_export.setOnClickListener { exportLauncher.launch("productos-backup.zip") }
        binding.btn_import.setOnClickListener { importLauncher.launch(arrayOf("application/zip", "application/json")) }
    }

    private fun switchView(view: String) {
        activeView = view
        binding.card_form.visibility = if (view == "products") View.VISIBLE else View.GONE
        binding.card_backup.visibility = if (view == "backup") View.VISIBLE else View.GONE
        binding.tv_list_header.visibility = if (view == "backup") View.GONE else View.VISIBLE
        binding.rv_management.visibility = if (view == "backup") View.GONE else View.VISIBLE
        
        updateList()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.products.collect { updateList() }
            }
        }
    }

    private fun updateList() {
        val products = viewModel.products.value
        val filtered = when (activeView) {
            "disabled" -> products.filter { !it.isActive }
            else -> products
        }
        adapter.submitList(filtered)
        binding.tv_list_header.text = "Productos (${filtered.size})"
    }

    private fun editProduct(product: Product) {
        currentProduct = product
        binding.et_equipo.setText(product.equipo)
        binding.et_marca.setText(product.marca)
        binding.et_modelo.setText(product.modelo)
        binding.et_tipo.setText(product.tipo)
        binding.et_precio.setText(product.precioUsd.toString())
        binding.et_info.setText(product.infoAdicional)
        binding.et_garantia.setText(product.garantia)
        binding.et_colores.setText(product.colores)
        imageUri = product.imageUrl
        updateImagePreview()
        switchView("products")
        binding.scrollView.smoothScrollTo(0, 0)
    }

    private fun saveProduct() {
        val equipo = binding.et_equipo.text.toString()
        val precio = binding.et_precio.text.toString().replace(',', '.').toDoubleOrNull()
        
        if (equipo.isBlank() || precio == null || imageUri.isBlank()) {
            binding.tv_error.text = "Equipo, Precio e Imagen son obligatorios"
            binding.tv_error.visibility = View.VISIBLE
            return
        }

        val product = currentProduct?.copy(
            equipo = equipo, marca = binding.et_marca.text.toString(),
            modelo = binding.et_modelo.text.toString(), tipo = binding.et_tipo.text.toString(),
            precioUsd = precio, imageUrl = imageUri, garantia = binding.et_garantia.text.toString(),
            colores = binding.et_colores.text.toString(), infoAdicional = binding.et_info.text.toString()
        ) ?: Product(
            equipo = equipo, marca = binding.et_marca.text.toString(),
            modelo = binding.et_modelo.text.toString(), tipo = binding.et_tipo.text.toString(),
            precioUsd = precio, imageUrl = imageUri, garantia = binding.et_garantia.text.toString(),
            colores = binding.et_colores.text.toString(), infoAdicional = binding.et_info.text.toString()
        )
        
        viewModel.saveProduct(product)
        resetForm()
    }

    private fun resetForm() {
        currentProduct = null
        binding.et_equipo.text?.clear()
        binding.et_marca.text?.clear()
        binding.et_modelo.text?.clear()
        binding.et_tipo.text?.clear()
        binding.et_precio.text?.clear()
        binding.et_info.text?.clear()
        binding.et_garantia.text?.clear()
        binding.et_colores.text?.clear()
        imageUri = ""
        updateImagePreview()
        binding.tv_error.visibility = View.GONE
    }

    private fun updateImagePreview() {
        if (imageUri.isNotBlank()) {
            binding.iv_product_preview.load(Uri.parse(imageUri))
            binding.layout_pick_prompt.visibility = View.GONE
            binding.btn_change_image.visibility = View.VISIBLE
        } else {
            binding.iv_product_preview.setImageDrawable(null)
            binding.layout_pick_prompt.visibility = View.VISIBLE
            binding.btn_change_image.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
