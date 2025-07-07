package sl.kacinz.onluanmer.presentation.ui.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import sl.kacinz.onluanmer.R
import sl.kacinz.onluanmer.databinding.FragmentGoalListBinding
import sl.kacinz.onluanmer.domain.model.Goal
import sl.kacinz.onluanmer.presentation.ui.adapters.GoalAdapter
import sl.kacinz.onluanmer.presentation.ui.fragments.viewmodels.GoalListViewModel

@AndroidEntryPoint
class GoalListFragment : Fragment() {

    private var _binding: FragmentGoalListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalListViewModel by viewModels()
    private val adapter = GoalAdapter { onGoalClick(it) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentGoalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvGoals.adapter = adapter
        binding.btnAddGoal.setOnClickListener {
            findNavController().navigate(R.id.createGoalFragment)
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.goals.collectLatest { goals ->
                adapter.submitList(goals)
            }
        }
    }

    private fun onGoalClick(goal: Goal) {
        val action = GoalListFragmentDirections
            .actionGoalListFragmentToGoalDetailFragment(goal)
        findNavController().navigate(action)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
