package sl.kacinz.onluanmer.presentation.ui.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sl.kacinz.onluanmer.R
import sl.kacinz.onluanmer.databinding.FragmentAddTransactionBinding
import sl.kacinz.onluanmer.domain.model.Transaction
import sl.kacinz.onluanmer.presentation.ui.fragments.viewmodels.AddTransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val args: AddTransactionFragmentArgs by navArgs()
    private val viewModel: AddTransactionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvDate.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        binding.btnSave.setOnClickListener { saveTransaction() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        })
    }

    private fun saveTransaction() {
        val amount = binding.etAmount.text.toString().toIntOrNull()
        if (amount == null) {
            Toast.makeText(requireContext(), "Enter amount", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = binding.etComment.text.toString()
        val goal = args.goal
        val isDeposit = args.isDeposit

        val newAmount = if (isDeposit) goal.currentAmount + amount else goal.currentAmount - amount

        // 🔍 Проверка: превышает ли целевую сумму
        if (isDeposit && newAmount > goal.targetAmount) {
            Toast.makeText(
                requireContext(),
                "Amount exceeds goal target of ${goal.targetAmount}",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val updatedGoal = goal.copy(currentAmount = newAmount)
        val transaction = Transaction(
            goalId = goal.id,
            amount = if (isDeposit) amount else -amount,
            comment = comment,
            date = binding.tvDate.text.toString()
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveTransaction(transaction, updatedGoal)
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                "updated_goal",
                updatedGoal
            )
            findNavController().popBackStack()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
