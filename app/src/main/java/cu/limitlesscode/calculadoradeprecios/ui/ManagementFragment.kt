package cu.limitlesscode.calculadoradeprecios.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import cu.limitlesscode.calculadoradeprecios.data.Product
import cu.limitlesscode.calculadoradeprecios.databinding.FragmentManagementBinding
import kotlinx.coroutines.launch

class ManagementFragment : Fragment() {

    private var _binding: FragmentManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: ProductManagementAdapter
    private var currentProduct: Product? = null
    private var imageUri: String = ""

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupForm()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ProductManagementAdapter(
            onEdit = { product -> editProduct(product) },
            onDelete = { product -> viewModel.deleteProduct(product) },
            onToggleActive = { product -> viewModel.saveProduct(product.copy(isActive = !product.isActive)) }
        )
        binding.rvManagement.layoutManager = LinearLayoutManager(requireContext())
        binding.rvManagement.adapter = adapter
    }

    private fun setupForm() {
        binding.fabAdd.setOnClickListener {
            showForm(true)
        }
        binding.layoutImagePicker.setOnClickListener {
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnChangeImage.setOnClickListener {
            imageUri = ""
            updateImagePreview()
        }
        binding.btnSave.setOnClickListener { saveProduct() }
        binding.btnCancel.setOnClickListener { resetForm() }
        
        binding.btnUseUsd.setOnClickListener {
            val cup = binding.etCupCalc.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
            val rate = viewModel.exchangeRate.value
            if (rate > 0) {
                val usd = cup / rate
                binding.etPrecio.setText(String.format("%.2f", usd).replace('.', ','))
            }
        }
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
        adapter.submitList(products)
        binding.tvListHeader.text = "Productos (${products.size})"
    }

    private fun editProduct(product: Product) {
        currentProduct = product
        binding.etEquipo.setText(product.equipo)
        binding.etMarca.setText(product.marca)
        binding.etModelo.setText(product.modelo)
        binding.etTipo.setText(product.tipo)
        binding.etPrecio.setText(product.precioUsd.toString())
        binding.etInfo.setText(product.infoAdicional)
        binding.etGarantia.setText(product.garantia)
        binding.etColores.setText(product.colores)
        imageUri = product.imageUrl
        updateImagePreview()
        showForm(true)
    }

    private fun saveProduct() {
        val equipo = binding.etEquipo.text.toString()
        val precio = binding.etPrecio.text.toString().replace(',', '.').toDoubleOrNull()
        
        if (equipo.isBlank() || precio == null || imageUri.isBlank()) {
            binding.tvError.text = "Equipo, Precio e Imagen son obligatorios"
            binding.tvError.visibility = View.VISIBLE
            return
        }

        val product = currentProduct?.copy(
            equipo = equipo, marca = binding.etMarca.text.toString(),
            modelo = binding.etModelo.text.toString(), tipo = binding.etTipo.text.toString(),
            precioUsd = precio, imageUrl = imageUri, garantia = binding.etGarantia.text.toString(),
            colores = binding.etColores.text.toString(), infoAdicional = binding.etInfo.text.toString()
        ) ?: Product(
            equipo = equipo, marca = binding.etMarca.text.toString(),
            modelo = binding.etModelo.text.toString(), tipo = binding.etTipo.text.toString(),
            precioUsd = precio, imageUrl = imageUri, garantia = binding.etGarantia.text.toString(),
            colores = binding.etColores.text.toString(), infoAdicional = binding.etInfo.text.toString()
        )
        
        viewModel.saveProduct(product)
        resetForm()
    }

    private fun resetForm() {
        currentProduct = null
        binding.etEquipo.text?.clear()
        binding.etMarca.text?.clear()
        binding.etModelo.text?.clear()
        binding.etTipo.text?.clear()
        binding.etPrecio.text?.clear()
        binding.etInfo.text?.clear()
        binding.etGarantia.text?.clear()
        binding.etColores.text?.clear()
        imageUri = ""
        updateImagePreview()
        binding.tvError.visibility = View.GONE
        showForm(false)
    }

    private fun showForm(show: Boolean) {
        binding.cardForm.visibility = if (show) View.VISIBLE else View.GONE
        binding.fabAdd.visibility = if (show) View.GONE else View.VISIBLE
        if (show) {
            binding.scrollView.smoothScrollTo(0, 0)
        }
    }

    private fun updateImagePreview() {
        if (imageUri.isNotBlank()) {
            binding.ivProductPreview.load(Uri.parse(imageUri))
            binding.layoutPickPrompt.visibility = View.GONE
            binding.btnChangeImage.visibility = View.VISIBLE
        } else {
            binding.ivProductPreview.setImageDrawable(null)
            binding.layoutPickPrompt.visibility = View.VISIBLE
            binding.btnChangeImage.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
