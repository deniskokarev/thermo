package dkv.thermo.ui.devices

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import dkv.thermo.databinding.FragmentThermoEditBinding
import dkv.thermo.db.Device

//private const val TAG = "ThermoFragment"

class ThermoEditFragment(private val device: Device) : Fragment() {
    private var _binding: FragmentThermoEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: ThermoModel by viewModels { ThermoModel.create(device) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThermoEditBinding.inflate(inflater, container, false)
        val root: View = binding.root
        // location is r/w
        viewModel.location.observe(viewLifecycleOwner) {
            if (binding.location.text.toString() != it) {
                binding.location.text.apply {
                    clear()
                    append(it)
                }
            }
        }
        binding.location.doOnTextChanged { text, _, _, _ ->
            viewModel.location.value = text.toString()
        }
        // temp is r/o
        viewModel.tempLabel.observe(viewLifecycleOwner) {
            binding.temp.text = it
        }
        // humidity is r/o
        viewModel.humidLabel.observe(viewLifecycleOwner) {
            binding.humid.text = it
        }
        // enabled is r/w
        viewModel.enabledCheckbox.observe(viewLifecycleOwner) {
            if (binding.enabledCheckbox.isChecked != it) {
                binding.enabledCheckbox.isChecked = it
            }
        }
        binding.enabledCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enabledCheckbox.value = isChecked
        }
        viewModel.launchUpdater()
        return root
    }

    override fun onDestroyView() {
        viewModel.stop()
        super.onDestroyView()
    }
}
