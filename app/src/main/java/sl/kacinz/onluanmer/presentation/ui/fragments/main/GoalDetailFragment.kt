package sl.kacinz.onluanmer.presentation.ui.fragments.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sl.kacinz.onluanmer.R
import sl.kacinz.onluanmer.databinding.FragmentGoalDetailBinding
import sl.kacinz.onluanmer.domain.model.Goal
import sl.kacinz.onluanmer.domain.model.Transaction

import sl.kacinz.onluanmer.presentation.ui.adapters.TransactionAdapter
import sl.kacinz.onluanmer.presentation.ui.fragments.viewmodels.GoalDetailViewModel
import java.text.NumberFormat

@AndroidEntryPoint
class GoalDetailFragment : Fragment() {

    private var _binding: FragmentGoalDetailBinding? = null
    private val binding get() = _binding!!

    private val args: GoalDetailFragmentArgs by navArgs()
    private val viewModel: GoalDetailViewModel by viewModels()
    private val adapter = TransactionAdapter()
    private var currentGoal: Goal? = null
    private val numberFormatter: NumberFormat =
        NumberFormat.getIntegerInstance().apply { isGroupingUsed = true }

    // список всех транзакций и флаг, показываем ли мы все
    private var transactions = listOf<Transaction>()
    private var showingAll = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentGoal = args.goal
        updateGoalUI(currentGoal!!)

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Goal>("updated_goal")
            ?.observe(viewLifecycleOwner) { goal ->
                updateGoalUI(goal)
                currentGoal = goal
            }

        binding.rvTransactions.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transactions(args.goal.id)
                    .collectLatest { list ->
                        transactions = list
                        showingAll = false
                        updateList()
                    }
            }
        }

        binding.btnProgress.setOnClickListener {
            currentGoal?.let { goal ->
                val action = GoalDetailFragmentDirections
                    .actionGoalDetailFragmentToProgressFragment(goal)
                findNavController().navigate(action)
            }
        }



        // обработчик «Show more / Show less»
        binding.tvShowMore.setOnClickListener {
            showingAll = !showingAll
            updateList()
        }

        binding.btnAddSaving.setOnClickListener {
            currentGoal?.let { goal ->
                val action = GoalDetailFragmentDirections
                    .actionGoalDetailFragmentToAddTransactionFragment(goal, true)
                findNavController().navigate(action)
            }
        }

        binding.btnWithdraw.setOnClickListener {
            currentGoal?.let { goal ->
                val action = GoalDetailFragmentDirections
                    .actionGoalDetailFragmentToWithdrawTransactionFragment(goal, false)
                findNavController().navigate(action)
            }
        }

        listOf(binding.ivBack, binding.btnBack).forEach { view ->
            view.setOnClickListener { findNavController().popBackStack() }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.goalListFragment)
            }
        })
    }

    private fun updateList() {
        // формируем список для адаптера
        val toShow = if (showingAll) transactions else transactions.take(3)
        adapter.submitList(toShow)

        // если всего элементов ≤ 3 — прячем кнопку
        if (transactions.size <= 3) {
            binding.tvShowMore.visibility = View.GONE
        } else {
            binding.tvShowMore.visibility = View.VISIBLE
            binding.tvShowMore.text = if (showingAll) "Show less" else "Show more..."
        }
    }

    private fun updateGoalUI(goal: Goal) {
        binding.tvGoalName.text = goal.name
        Glide.with(this)
            .load(goal.imageUri)
            .into(binding.ivGoalImage)

        val target = goal.targetAmount
        val current = goal.currentAmount
        binding.pbGoal.max = target
        binding.pbGoal.progress = current

        val currentStr = numberFormatter.format(current)
        val targetStr = numberFormatter.format(target)
        binding.tvProgressText.text = "$currentStr$ / $targetStr$"

        val percent = if (target > 0) (current * 100 / target) else 0
        binding.tvPercent.text = "$percent%"

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}