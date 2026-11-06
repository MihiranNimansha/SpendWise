package com.spendwise.app.ui.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.core.widget.doAfterTextChanged
import com.spendwise.app.R
import com.spendwise.app.databinding.FragmentAddGoalBottomSheetBinding
import com.spendwise.app.utils.Constants
import com.spendwise.app.utils.hide
import com.spendwise.app.utils.show

class AddGoalBottomSheet(
    private val availableCategories: List<String>,
    private val isEditMode: Boolean,
    private val initialCategory: String = "",
    private val initialLimit: Double = 0.0,
    private val initialGoalId: Int = -1,
    private val currency: String = "LKR"
) : BottomSheetDialogFragment() {

    private var _binding: FragmentAddGoalBottomSheetBinding? = null
    private val binding get() = _binding!!

    var onGoalSaved: ((category: String, limit: Double) -> Unit)? = null
    var onGoalDeleted: ((goalId: Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddGoalBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tilLimit.prefixText = when(currency) {
            "LKR" -> "Rs. "
            "USD" -> "$ "
            "EUR" -> "€ "
            "GBP" -> "£ "
            else -> ""
        }

        if (isEditMode) {
            binding.tvBottomSheetTitle.text = "Edit Category Budget"
            binding.actvCategory.setText(initialCategory)
            binding.actvCategory.isEnabled = false
            binding.tilCategory.isEnabled = false
            binding.etLimit.setText(initialLimit.toString())
            binding.btnRemoveGoal.show()
        } else {
            binding.tvBottomSheetTitle.text = "Set Category Budget"
            binding.btnRemoveGoal.hide()
            
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availableCategories)
            binding.actvCategory.setAdapter(adapter)
            if (availableCategories.isNotEmpty()) {
                binding.actvCategory.setText(availableCategories.first(), false)
            }
        }

        binding.etLimit.doAfterTextChanged { binding.tilLimit.error = null }
        binding.actvCategory.doAfterTextChanged { binding.tilCategory.error = null }

        binding.btnSaveGoal.setOnClickListener {
            val limit = binding.etLimit.text.toString().toDoubleOrNull()
            val category = binding.actvCategory.text.toString()

            var valid = true
            if (category.isEmpty() || category !in Constants.CATEGORIES) {
                binding.tilCategory.error = getString(R.string.error_category_required)
                valid = false
            }
            if (limit == null || limit <= 0.0) {
                binding.tilLimit.error = getString(R.string.error_invalid_amount)
                binding.etLimit.requestFocus()
                valid = false
            }
            if (!valid) return@setOnClickListener

            onGoalSaved?.invoke(category, limit!!)
            dismiss()
        }

        binding.btnRemoveGoal.setOnClickListener {
            onGoalDeleted?.invoke(initialGoalId)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
