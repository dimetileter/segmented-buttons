package com.dimetileter.segmentedbuttons

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dimetileter.segmentedbuttons.databinding.ActivityMainBinding

/**
 * SegmentedButtonBar bileşenlerinin metin ve ikon odaklı kullanımını gösteren örnek etkinlik.
 * Demo activity showcasing text-only, icon-only, and icon+text configurations.
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
        // Horizontal text-only bar (Day, Week, Month)
        binding.horizontalBarTextOnly.setOnButton1Click {
            binding.statusText.text = "Status: Text-Only -> Button 1 (Day)"
        }
        binding.horizontalBarTextOnly.setOnButton2Click {
            binding.statusText.text = "Status: Text-Only -> Button 2 (Week)"
        }
        binding.horizontalBarTextOnly.setOnButton3Click {
            binding.statusText.text = "Status: Text-Only -> Button 3 (Month)"
        }

        // Horizontal icon + text bar
        binding.horizontalBarIconText.setOnButton1Click {
            binding.statusText.text = "Status: Icon+Text -> Button 1 (Previous)"
        }
        binding.horizontalBarIconText.setOnButton2Click {
            binding.statusText.text = "Status: Icon+Text -> Button 2 (Next)"
        }

        // Vertical 3-button bar
        binding.verticalBar3.setOnButton1Click {
            binding.statusText.text = "Status: Vertical -> Button 1"
        }
        binding.verticalBar3.setOnButton2Click {
            binding.statusText.text = "Status: Vertical -> Button 2"
        }
        binding.verticalBar3.setOnButton3Click {
            binding.statusText.text = "Status: Vertical -> Button 3"
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