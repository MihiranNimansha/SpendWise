package com.spendwise.app.ui.onboarding

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.spendwise.app.MainActivity
import com.spendwise.app.R
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPrefs: UserPreferences
    private lateinit var adapter: OnboardingAdapter
    private val dots = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userPrefs = UserPreferences(requireContext())
        (activity as? MainActivity)?.hideNavigation()

        val slides = listOf(
            OnboardingSlide(
                "splash_animation.json",
                "Track Every Rupee",
                "Log your daily expenses in seconds and know exactly where your money goes"
            ),
            OnboardingSlide(
                "warning_animation.json",
                "Set Smart Budgets",
                "Create monthly budget goals per category and get alerted before you overspend"
            ),
            OnboardingSlide(
                "splash_animation.json",
                "Understand Your Spending",
                "Beautiful charts and analytics show your spending patterns at a glance"
            )
        )

        adapter = OnboardingAdapter(slides)
        binding.viewPagerOnboarding.adapter = adapter

        setupDots()

        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                
                if (position == slides.size - 1) {
                    binding.btnNext.text = "Get Started 🚀"
                } else {
                    binding.btnNext.text = "Next"
                }
            }
        })

        binding.btnNext.setOnClickListener {
            if (binding.viewPagerOnboarding.currentItem < slides.size - 1) {
                binding.viewPagerOnboarding.currentItem += 1
            } else {
                finishOnboarding()
            }
        }

        binding.tvSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun setupDots() {
        binding.layoutDots.removeAllViews()
        dots.clear()
        
        val count = adapter.itemCount
        for (i in 0 until count) {
            val dot = View(requireContext())
            val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics).toInt()
            val layoutParams = LinearLayout.LayoutParams(size, size)
            layoutParams.setMargins(8, 0, 8, 0)
            dot.layoutParams = layoutParams
            
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(Color.WHITE)
            dot.background = drawable
            dot.alpha = 0.4f
            
            dots.add(dot)
            binding.layoutDots.addView(dot)
        }
        
        if (dots.isNotEmpty()) {
            updateDots(0)
        }
    }

    private fun updateDots(position: Int) {
        val activeSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
        val inactiveSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics).toInt()
        val activeColor = ContextCompat.getColor(requireContext(), R.color.colorAccent)

        for (i in dots.indices) {
            val dot = dots[i]
            val drawable = dot.background as GradientDrawable
            
            if (i == position) {
                drawable.setColor(activeColor)
                dot.alpha = 1f
                animateDotSize(dot, inactiveSize, activeSize)
            } else {
                drawable.setColor(Color.WHITE)
                dot.alpha = 0.4f
                animateDotSize(dot, dot.layoutParams.width, inactiveSize)
            }
        }
    }

    private fun animateDotSize(view: View, fromSize: Int, toSize: Int) {
        if (fromSize == toSize) return
        val animator = ValueAnimator.ofInt(fromSize, toSize)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            val size = animation.animatedValue as Int
            val lp = view.layoutParams
            lp.width = size
            lp.height = size
            view.layoutParams = lp
        }
        animator.start()
    }

    private fun finishOnboarding() {
        userPrefs.isOnboardingShown = true
        findNavController().navigate(
            R.id.loginFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.splashFragment, true)
                .build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
