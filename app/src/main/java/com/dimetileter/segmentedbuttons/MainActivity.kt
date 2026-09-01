package com.dimetileter.segmentedbuttons

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dimetileter.segmentedbuttons.databinding.ActivityMainBinding

/**
 * SegmentedButtonBar bileşenlerinin tüm 5 stilini gösteren örnek etkinlik.
 * Demo activity showcasing all 5 styles of SegmentedButtonBar components.
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
        // Horizontal 2-button bar
        binding.horizontalBar2.setOnButton1Click {
            binding.statusText.text = "Status: Horizontal (2) -> Button 1 (Camera)"
        }
        binding.horizontalBar2.setOnButton2Click {
            binding.statusText.text = "Status: Horizontal (2) -> Button 2 (Gallery)"
        }

        // Horizontal 3-button bar
        binding.horizontalBar3.setOnButton1Click {
            binding.statusText.text = "Status: Horizontal (3) -> Button 1 (All)"
        }
        binding.horizontalBar3.setOnButton2Click {
            binding.statusText.text = "Status: Horizontal (3) -> Button 2 (Favorites)"
        }
        binding.horizontalBar3.setOnButton3Click {
            binding.statusText.text = "Status: Horizontal (3) -> Button 3 (Archived)"
        }

        // Vertical 3-button bar
        binding.verticalBar3.setOnButton1Click {
            binding.statusText.text = "Status: Vertical (3) -> Button 1"
        }
        binding.verticalBar3.setOnButton2Click {
            binding.statusText.text = "Status: Vertical (3) -> Button 2"
        }
        binding.verticalBar3.setOnButton3Click {
            binding.statusText.text = "Status: Vertical (3) -> Button 3"
        }

        // Circular single-button bar
        binding.circularBar.setOnButton1Click {
            binding.statusText.text = "Status: Circular button clicked"
        }

        // Expandable animated bar
        binding.expandableBar.setOnExpandChangeListener { isExpanded ->
            binding.statusText.text = "Status: Expandable bar (Expanded: $isExpanded)"
        }
        binding.expandableBar.setOnButton2Click {
            binding.statusText.text = "Status: Expandable -> Button 2 clicked"
        }
        binding.expandableBar.setOnButton3Click {
            binding.statusText.text = "Status: Expandable -> Button 3 clicked"
        }

        // Pill button bar
        binding.pillNextBar.setOnPillClick {
            val isNowActive = !binding.pillNextBar.isPillActivated()
            binding.pillNextBar.setPillActivated(isNowActive)
            binding.statusText.text = "Status: Pill Next clicked (Activated: $isNowActive)"
        }
    }
}