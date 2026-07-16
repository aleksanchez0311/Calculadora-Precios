package cu.limitlesscode.calculadoradeprecios.ui

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
import cu.limitlesscode.calculadoradeprecios.MainViewModel
import cu.limitlesscode.calculadoradeprecios.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

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
