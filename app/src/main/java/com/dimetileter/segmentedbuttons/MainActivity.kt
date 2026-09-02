package com.dimetileter.segmentedbuttons

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dimetileter.segmentedbuttons.databinding.ActivityMainBinding

/**
 * SegmentedButtonBar bileşenlerinin hibrit, metin, ikon renklendirme ve durum yönetimini gösteren örnek etkinlik.
 * Demo activity showcasing hybrid styles, icon tinting, and state management.
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
        // TabBar with Sliding Indicator
        binding.tabBarDemo.setOnTabSelectedListener { position ->
            val tabName = when (position) {
                0 -> "Home"
                1 -> "Explore"
                2 -> "Profile"
                else -> "Tab ${position + 1}"
            }
            binding.statusText.text = "Status: TabBar selected -> $tabName"
        }

        // 5-Button Bar with Circular action and icon tint
        binding.fiveButtonBar.setOnButton1Click {
            binding.statusText.text = "Status: 5-Button -> Button 1 clicked"
        }
        binding.fiveButtonBar.setOnButton2Click {
            binding.statusText.text = "Status: 5-Button -> Button 2 clicked"
        }
        binding.fiveButtonBar.setOnButton3Click {
            binding.statusText.text = "Status: 5-Button -> Button 3 clicked"
        }
        binding.fiveButtonBar.setOnButton4Click {
            binding.statusText.text = "Status: 5-Button -> Button 4 clicked"
        }
        binding.fiveButtonBar.setOnButton5Click {
            binding.statusText.text = "Status: 5-Button -> Button 5 (Circular) clicked!"
        }

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

        // Hybrid bar (2 Horizontal + 1 Circular 32x32dp)
        binding.hybridBar.setOnButton1Click {
            binding.statusText.text = "Status: Hybrid -> Tab 1 (Active)"
        }
        binding.hybridBar.setOnButton2Click {
            binding.statusText.text = "Status: Hybrid -> Tab 2 (Completed)"
        }
        binding.hybridBar.setOnButton3Click {
            binding.statusText.text = "Status: Hybrid -> Button 3 (Circular 32x32 Action clicked!)"
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