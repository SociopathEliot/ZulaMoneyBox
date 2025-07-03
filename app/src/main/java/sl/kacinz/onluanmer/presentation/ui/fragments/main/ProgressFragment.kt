package sl.kacinz.onluanmer.presentation.ui.fragments.main

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sl.kacinz.onluanmer.R
import sl.kacinz.onluanmer.databinding.FragmentProgressBinding
import sl.kacinz.onluanmer.domain.model.SampleData
import sl.kacinz.onluanmer.domain.model.Transaction
import sl.kacinz.onluanmer.presentation.ui.fragments.viewmodels.ProgressViewModel
import sl.kacinz.onluanmer.utils.TimeRange
import sl.kacinz.onluanmer.utils.toKString
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private val args: ProgressFragmentArgs by navArgs()
    private val viewModel: ProgressViewModel by viewModels()

    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private val ranges = listOf(
        TimeRange.LAST_WEEK,
        TimeRange.LAST_MONTH,
        TimeRange.LAST_YEAR,
        TimeRange.ALL_TIME
    )
    private var rangeIndex = 0

    private var allTransactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvGoal.text = args.goal.name
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnPrev.setOnClickListener { changeRange(-1) }
        binding.btnNext.setOnClickListener { changeRange(1) }
        binding.tvRange.text = ranges[rangeIndex].label

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (args.goal.id == SampleData.sampleGoal.id) {
                    allTransactions = SampleData.sampleTransactions
                    updateForRange()
                } else {
                    viewModel.transactions(args.goal.id).collect { list ->
                        allTransactions = list
                        updateForRange()
                    }
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            })
    }

    private fun changeRange(delta: Int) {
        rangeIndex = (rangeIndex + delta + ranges.size) % ranges.size
        binding.tvRange.text = ranges[rangeIndex].label
        updateForRange()
    }

    private fun updateForRange() {
        val filtered = filterTransactions(allTransactions, ranges[rangeIndex])
        updateInfo(filtered)
        updateChart(filtered)
    }

    private fun filterTransactions(
        transactions: List<Transaction>,
        range: TimeRange
    ): List<Transaction> {
        if (range.days == null) return transactions
        val cutoff = LocalDate.now().minusDays(range.days)
        return transactions.filter {
            val date = LocalDate.parse(it.date, formatter)
            !date.isBefore(cutoff)
        }
    }

    private fun updateInfo(transactions: List<Transaction>) {
        val total = transactions.sumOf { it.amount }
        val deposits = transactions.filter { it.amount > 0 }
        val average = if (deposits.isNotEmpty()) deposits.map { it.amount }.average() else 0.0
        binding.tvTotal.text = "${total}\$"
        binding.tvAverage.text = String.format("%.1f$", average)
        binding.tvCount.text = transactions.size.toString()
    }

    private fun updateChart(transactions: List<Transaction>) {
        // 1. Two formatters
        val parseFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val labelFmt = DateTimeFormatter.ofPattern("dd.MM")

        // 2. Parse & sort
        val parsedDates = transactions
            .map { LocalDate.parse(it.date, parseFmt) }
            .sorted()
        if (parsedDates.isEmpty()) return

        // 3. Range +1 day
        val startDate = parsedDates.first()
        val lastDate = parsedDates.last()
        val endForLabel = lastDate.plusDays(1)

        // 4. Build full date list
        val allDates = generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endForLabel) }
            .toList()

        // 5. Quick lookup
        val txByDate = transactions.associateBy { LocalDate.parse(it.date, parseFmt) }

        // 6. Snap y to nearest lower 100, shift right by 1 cell for that left‐gap
        val step = 100f
        val entries = allDates
            .dropLast(1)
            .mapIndexed { idx, date ->
                val raw = txByDate[date]?.amount?.toFloat() ?: 0f
                val snapped = (raw / step).toInt() * step
                Entry(idx + 1f, snapped)
            }

        // 7. Labels with that extra day on the right
        val labels = listOf("") + allDates.map { it.format(labelFmt) }

        // 8. DataSet styling (unchanged)
        val dataSet = LineDataSet(entries, "").apply {
            mode = LineDataSet.Mode.LINEAR
            setDrawValues(false)
            color = Color.parseColor("#FFC107")
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 6f
            circleHoleRadius = 3f
            setCircleColor(Color.parseColor("#FFC107"))
            circleHoleColor = Color.parseColor("#0D1B3D")
            setDrawCircleHole(true)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.gradient_chart_fill
            )
        }

        // 9. Chart setup (unchanged)
        binding.lineChart.apply {
            data = LineData(dataSet)
            setBackgroundColor(Color.parseColor("#0D1B3D"))
            setDrawGridBackground(false)
            setTouchEnabled(false)
            axisRight.isEnabled = false
            legend.isEnabled = false
// dp → px helper
            val dp = resources.displayMetrics.density

// inset: left 8dp, top 0, right 24dp, bottom 16dp
            setExtraOffsets(
                0f * dp,
                0f,
                0f * dp,
                0f * dp
            )


            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                isGranularityEnabled = true
                setLabelCount(labels.size, true)
                setAvoidFirstLastClipping(true)
                setAxisMinimum(0f)
                setAxisMaximum((labels.size - 1).toFloat())
                textSize = 12f
                textColor = Color.WHITE
                setDrawAxisLine(false)
                setDrawGridLines(true)
                gridColor = Color.parseColor("#4D6F7D9C")
                gridLineWidth = 1f
            }

            axisLeft.apply {
                val maxY = dataSet.yMax
                val axisStep = computeAxisStep(maxY)
                val top = ((maxY / axisStep).toInt() + 1) * axisStep
                setAxisMinimum(0f)
                setAxisMaximum(top)
                setLabelCount((top / axisStep).toInt() + 1, true)
                granularity = axisStep
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = value.toKString()
                }
                textSize = 12f
                textColor = Color.WHITE
                setDrawAxisLine(false)
                setDrawGridLines(true)
                gridColor = Color.parseColor("#4D6F7D9C")
                gridLineWidth = 1f
            }

            description.isEnabled = false
            animateX(500)
            invalidate()
        }
    }

    private fun computeAxisStep(maxValue: Float): Float {
        if (maxValue <= 0f) return 1f
        val raw = maxValue / 5f
        val magnitude = 10f.pow(floor(log10(raw)))
        val residual = raw / magnitude
        val step = when {
            residual <= 1f -> 1f
            residual <= 2f -> 2f
            residual <= 5f -> 5f
            else -> 10f
        }
        return step * magnitude
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
