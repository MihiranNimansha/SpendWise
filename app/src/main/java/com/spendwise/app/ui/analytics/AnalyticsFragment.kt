package com.spendwise.app.ui.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.spendwise.app.MainActivity
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentAnalyticsBinding
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.hide
import com.spendwise.app.utils.show
import com.spendwise.app.viewmodel.ExpenseViewModel

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val expenseViewModel: ExpenseViewModel by activityViewModels()
    private lateinit var userPreferences: UserPreferences
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences(requireContext())
        currentUserId = userPreferences.userEmail

        (activity as? MainActivity)?.showNavigation()

        setupPieChart()
        observeData()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.isEnabled = false
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
        }
    }

    private fun observeData() {
        expenseViewModel.categoryTotalsThisMonth.observe(viewLifecycleOwner) { categoryMap ->
            if (categoryMap.isNullOrEmpty()) {
                binding.pieChart.hide()
                binding.layoutEmptyAnalytics.show()
                binding.tvTotalSpending.text = CurrencyFormatter.format(0.0, userPreferences.preferredCurrency)
            } else {
                binding.layoutEmptyAnalytics.hide()
                binding.pieChart.show()
                updatePieChart(categoryMap)
                
                val total = categoryMap.values.sum()
                binding.tvTotalSpending.text = CurrencyFormatter.format(total, userPreferences.preferredCurrency)
            }
        }
    }

    private fun updatePieChart(categoryMap: Map<String, Double>) {
        val entries = ArrayList<PieEntry>()
        categoryMap.forEach { (category, amount) ->
            if (amount > 0) {
                entries.add(PieEntry(amount.toFloat(), category))
            }
        }

        val dataSet = PieDataSet(entries, "Spending by Category")
        dataSet.setDrawIcons(false)
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        val colors = ArrayList<Int>()
        colors.add(Color.parseColor("#4CAF50"))
        colors.add(Color.parseColor("#2196F3"))
        colors.add(Color.parseColor("#FF9800"))
        colors.add(Color.parseColor("#F44336"))
        colors.add(Color.parseColor("#9C27B0"))
        colors.add(Color.parseColor("#E91E63"))
        colors.add(Color.parseColor("#607D8B"))
        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(binding.pieChart))
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.BLACK)
        
        binding.pieChart.data = data
        binding.pieChart.highlightValues(null)
        binding.pieChart.invalidate()
        binding.pieChart.animateY(1400)
    }

    override fun onResume() {
        super.onResume()
        expenseViewModel.refreshMonth()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
