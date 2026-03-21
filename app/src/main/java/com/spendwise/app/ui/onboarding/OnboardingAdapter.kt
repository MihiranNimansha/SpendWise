package com.spendwise.app.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.spendwise.app.R

data class OnboardingSlide(
    val lottieFile: String,
    val title: String,
    val description: String
)

class OnboardingAdapter(private val slides: List<OnboardingSlide>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lottieAnimation: LottieAnimationView = view.findViewById(R.id.lottieOnboarding)
        val tvTitle: TextView = view.findViewById(R.id.tvSlideTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvSlideDesc)
        
        fun bind(slide: OnboardingSlide) {
            lottieAnimation.setAnimation(slide.lottieFile)
            tvTitle.text = slide.title
            tvDesc.text = slide.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_slide, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount() = slides.size
}
