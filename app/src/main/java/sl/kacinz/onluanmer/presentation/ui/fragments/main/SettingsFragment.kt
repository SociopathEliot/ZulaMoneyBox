package sl.kacinz.onluanmer.presentation.ui.fragments.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import sl.kacinz.onluanmer.R
import sl.kacinz.onluanmer.databinding.FragmentSettingsBinding
import sl.kacinz.onluanmer.presentation.ui.fragments.legal.PrivacyPolicyFragment
import sl.kacinz.onluanmer.presentation.ui.fragments.viewmodels.SettingsViewModel
import sl.kacinz.onluanmer.utils.Constants.DEFAULT_DOMAIN_LINK
import sl.kacinz.onluanmer.utils.Constants.launchNewFragmentWithoutBackstackForFragmentContainer

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.goalListFragment)
                }
            })

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack(R.id.goalListFragment, false)
        }

        binding.btnResetAll.setOnClickListener {
            showConfirmResetDialog()
        }

        binding.llSelectCurrency.setOnClickListener {
            showSelectCurrencyDialog()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.currency.collectLatest { cur ->
                cur?.let { binding.tvCurrentCurrency.text = it }
            }
        }

        binding.btnPrivacy.setOnClickListener {
            parentFragmentManager.launchNewFragmentWithoutBackstackForFragmentContainer(
                PrivacyPolicyFragment(DEFAULT_DOMAIN_LINK)
            )
        }

        binding.btnRate.setOnClickListener {
            openAppInPlayStore()
        }

        binding.btnShare.setOnClickListener {
            shareAppLink()
        }
    }

    private fun openAppInPlayStore() {
        val packageName = requireContext().packageName
        val uri = "market://details?id=$packageName"
        val marketIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)).apply {
            setPackage("com.android.vending")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            startActivity(marketIntent)
        } catch (e: Exception) {
            val webUri = "https://play.google.com/store/apps/details?id=$packageName"
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(webUri)))
        }
    }

    private fun shareAppLink() {
        val packageName = requireContext().packageName
        val shareText = "Check out this app: https://play.google.com/store/apps/details?id=$packageName"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }


    @SuppressLint("SoonBlockedPrivateApi")
    private fun showSelectCurrencyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_currency, null, false)

        val currencies = arrayOf("CNY", "CAD", "AUD", "USD", "EUR", "GBP", "JPY")
        val np = dialogView.findViewById<NumberPicker>(R.id.np_currencies)

        np.minValue = 0
        np.maxValue = currencies.size - 1
        np.displayedValues = currencies
        np.wrapSelectorWheel = false
        np.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS

        val current = binding.tvCurrentCurrency.text.toString()
        val idx = currencies.indexOf(current).takeIf { it >= 0 } ?: 0
        np.value = idx

        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(dialogView)
        bottomSheetDialog.setCanceledOnTouchOutside(true)
        bottomSheetDialog.show()

        dialogView.findViewById<ImageView>(R.id.iv_close).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_ok).setOnClickListener {
            val selected = currencies[np.value]
            binding.tvCurrentCurrency.text = selected
            viewModel.setCurrency(selected)
            bottomSheetDialog.dismiss()
        }
    }


    private fun showConfirmResetDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_confirm_reset, null, false)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()

        view.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btn_delete).setOnClickListener {
            clearAllAppData()
            dialog.dismiss()
        }
    }

    private fun clearAllAppData() {
        viewModel.clearAll()
        Toast.makeText(requireContext(), "All data reset", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack(R.id.goalListFragment, false)
    }

}
