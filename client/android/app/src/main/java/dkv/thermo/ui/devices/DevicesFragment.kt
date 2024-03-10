package dkv.thermo.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
//import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dkv.thermo.R
import dkv.thermo.databinding.FragmentDevicesBinding

class DevicesFragment : Fragment() {

    private var _binding: FragmentDevicesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private fun addDevices(root: View) {
        val tableLayout = root.findViewById<TableLayout>(R.id.devices_table)
        val fragmentTransaction = parentFragmentManager.beginTransaction()
        repeat(10) {
            val tableRow = TableRow(context)
            tableRow.id = View.generateViewId()
            val tf = ThermoFragment(it)
            fragmentTransaction.add(tableRow.id, tf)
            tableLayout.addView(tableRow)
        }
        fragmentTransaction.commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel =
            ViewModelProvider(this).get(DevicesViewModel::class.java)

        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        addDevices(root)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}