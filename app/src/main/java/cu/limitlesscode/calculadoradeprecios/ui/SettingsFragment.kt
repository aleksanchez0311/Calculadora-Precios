package cu.limitlesscode.calculadoradeprecios.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cu.limitlesscode.calculadoradeprecios.MainViewModel
import cu.limitlesscode.calculadoradeprecios.data.BackupManager
import cu.limitlesscode.calculadoradeprecios.data.Product
import cu.limitlesscode.calculadoradeprecios.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val backupManager by lazy { BackupManager(requireContext()) }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val success = backupManager.exportBackup(
                    viewModel.products.value,
                    viewModel.exchangeRate.value,
                    it
                )
                if (success) {
                    Toast.makeText(requireContext(), "Respaldo guardado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val backupData = backupManager.importBackup(it)
                if (backupData != null) {
                    showRestoreConfirmationDialog(backupData)
                } else {
                    Toast.makeText(requireContext(), "Error al leer el respaldo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRestoreConfirmationDialog(data: cu.limitlesscode.calculadoradeprecios.data.BackupData) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Restauración")
            .setMessage("Esta acción eliminará TODOS los productos actuales y los reemplazará con los del respaldo. ¿Desea continuar?")
            .setPositiveButton("Restaurar") { _, _ ->
                viewModel.restoreBackup(data)
                Toast.makeText(requireContext(), "Restauración completada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInputs()
        setupBackup()
        observeViewModel()
    }

    private fun setupInputs() {
        binding.etExchangeRate.setText(viewModel.exchangeRate.value.toString().replace('.', ','))
        binding.etExchangeRate.addTextChangedListener { text ->
            val rate = text.toString().replace(',', '.').toDoubleOrNull()
            if (rate != null) {
                viewModel.updateExchangeRate(rate)
            }
        }
    }

    private fun setupBackup() {
        binding.btnSetupBackup.setOnClickListener {
            showBackupDialog()
        }
    }

    private fun showBackupDialog() {
        val options = arrayOf("Exportar productos", "Restaurar respaldo")
        AlertDialog.Builder(requireContext())
            .setTitle("Respaldo y restauración")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportLauncher.launch("productos-backup.zip")
                    1 -> importLauncher.launch(arrayOf("application/zip", "application/json"))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.exchangeRate.collect { rate ->
                        val currentText = binding.etExchangeRate.text.toString().replace(',', '.')
                        if (currentText.toDoubleOrNull() != rate) {
                            binding.etExchangeRate.setText(rate.toString().replace('.', ','))
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
