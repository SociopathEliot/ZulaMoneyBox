package sl.kacinz.onluanmer.presentation.ui.fragments.main

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import sl.kacinz.onluanmer.R
import sl.kacinz.onluanmer.databinding.FragmentCreateGoalBinding
import sl.kacinz.onluanmer.domain.model.Goal
import sl.kacinz.onluanmer.presentation.ui.fragments.viewmodels.CreateGoalViewModel
import java.util.Calendar

@AndroidEntryPoint
class CreateGoalFragment : Fragment() {

    private var _binding: FragmentCreateGoalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateGoalViewModel by viewModels()

    private var imageUri: Uri? = null
    private var selectedImageView: ImageView? = null
    private var placeholderView: View? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            val iv = ImageView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(it)
            }
            selectedImageView = iv

            binding.imagePicker.removeAllViews()
            binding.imagePicker.addView(iv)
            binding.imagePicker.addView(binding.btnDelete)
            binding.btnDelete.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        placeholderView = binding.imagePicker.getChildAt(1)

        binding.etDate.setOnClickListener { openDatePicker() }
        binding.imagePicker.setOnClickListener { imagePickerLauncher.launch("image/*") }
        binding.btnGetStarted.setOnClickListener { saveGoal() }
        binding.btnDelete.setOnClickListener { removeImage() }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.goalListFragment)
            }
        })
    }

    private fun openDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val formatted = String.format("%02d.%02d.%04d", day, month + 1, year)
                binding.tvDate.setText(formatted)
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveGoal() {
        val name = binding.etGoalName.text.toString().trim()
        val amountStr = binding.etTargetAmount.text.toString().trim()
        val date = binding.tvDate.text.toString().trim()
        val img = imageUri?.toString()

        // Проверяем ввод
        if (name.isEmpty() || amountStr.isEmpty() || date.isEmpty() || img.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
            return
        }

        // Парсим сумму в Int
        val amount = amountStr.toIntOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Enter a valid numeric amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Создаём и сохраняем цель
        val goal = Goal(
            name = name,
            targetAmount = amount,
            date = date,
            imageUri = img
        )
        viewModel.createGoal(goal)
        findNavController().popBackStack()
    }

    private fun removeImage() {
        imageUri = null
        selectedImageView = null
        binding.imagePicker.removeAllViews()
        binding.btnDelete.visibility = View.GONE
        binding.imagePicker.addView(binding.btnDelete)
        placeholderView?.let { binding.imagePicker.addView(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
