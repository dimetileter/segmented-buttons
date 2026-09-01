package com.dimetileter.segmentedbuttons

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dimetileter.segmentedbuttons.databinding.ActivityMainBinding

/**
 * SegmentedButtonBar bileşenlerinin kullanımını gösteren örnek etkinlik.
 * Demo activity showcasing the usage of SegmentedButtonBar components.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.horizontalBar2.setOnButton1Click {
            binding.statusText.text = "Status: 2-Bar -> Button 1 (Camera) clicked"
        }
        binding.horizontalBar2.setOnButton2Click {
            binding.statusText.text = "Status: 2-Bar -> Button 2 (Gallery) clicked"
        }

        binding.horizontalBar3.setOnButton1Click {
            binding.statusText.text = "Status: 3-Bar -> Button 1 (All) clicked"
        }
        binding.horizontalBar3.setOnButton2Click {
            binding.statusText.text = "Status: 3-Bar -> Button 2 (Favorites) clicked"
        }
        binding.horizontalBar3.setOnButton3Click {
            binding.statusText.text = "Status: 3-Bar -> Button 3 (Archived) clicked"
        }

        binding.pillNextBar.setOnPillClick {
            val isNowActive = !binding.pillNextBar.isPillActivated()
            binding.pillNextBar.setPillActivated(isNowActive)
            binding.statusText.text = "Status: Pill Next clicked (Activated: $isNowActive)"
        }
    }
}