package dkv.thermo.ui.devices

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dkv.thermo.databinding.FragmentThermoBinding

class ThermoFragment(private val deviceId: Int) : Fragment() {
    private var _binding: FragmentThermoBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: ThermoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThermoBinding.inflate(inflater, container, false)
        val root: View = binding.root
        viewModel.location.observe(viewLifecycleOwner) {
            binding.location.text = it
        }
        viewModel.tempLabel.observe(viewLifecycleOwner) {
            binding.temp.text = it
        }
        viewModel.humidLabel.observe(viewLifecycleOwner) {
            binding.humid.text = it
        }
        viewModel.launch(deviceId)
        return root
    }

    override fun onDestroyView() {
        viewModel.stop()
        super.onDestroyView()
    }
}