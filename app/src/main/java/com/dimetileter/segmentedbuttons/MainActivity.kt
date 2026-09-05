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
                0 -> getString(R.string.tab_home)
                1 -> getString(R.string.tab_explore)
                2 -> getString(R.string.tab_profile)
                else -> getString(R.string.tab_fallback, position + 1)
            }
            binding.statusText.text = getString(R.string.status_tab_selected, tabName)
        }

        // 5-Button Bar with Circular action and icon tint
        binding.fiveButtonBar.setOnButton1Click {
            binding.statusText.text = getString(R.string.status_five_button_1)
        }
        binding.fiveButtonBar.setOnButton2Click {
            binding.statusText.text = getString(R.string.status_five_button_2)
        }
        binding.fiveButtonBar.setOnButton3Click {
            binding.statusText.text = getString(R.string.status_five_button_3)
        }
        binding.fiveButtonBar.setOnButton4Click {
            binding.statusText.text = getString(R.string.status_five_button_4)
        }
        binding.fiveButtonBar.setOnButton5Click {
            binding.statusText.text = getString(R.string.status_five_button_5)
        }

        // Horizontal text-only bar (Day, Week, Month)
        binding.horizontalBarTextOnly.setOnButton1Click {
            binding.statusText.text = getString(R.string.status_text_only_day)
        }
        binding.horizontalBarTextOnly.setOnButton2Click {
            binding.statusText.text = getString(R.string.status_text_only_week)
        }
        binding.horizontalBarTextOnly.setOnButton3Click {
            binding.statusText.text = getString(R.string.status_text_only_month)
        }

        // Hybrid bar (2 Horizontal + 1 Circular 32x32dp)
        binding.hybridBar.setOnButton1Click {
            binding.statusText.text = getString(R.string.status_hybrid_active)
        }
        binding.hybridBar.setOnButton2Click {
            binding.statusText.text = getString(R.string.status_hybrid_completed)
        }
        binding.hybridBar.setOnButton3Click {
            binding.statusText.text = getString(R.string.status_hybrid_circular)
        }

        // Horizontal icon + text bar
        binding.horizontalBarIconText.setOnButton1Click {
            binding.statusText.text = getString(R.string.status_icon_text_previous)
        }
        binding.horizontalBarIconText.setOnButton2Click {
            binding.statusText.text = getString(R.string.status_icon_text_next)
        }

        // Vertical 3-button bar
        binding.verticalBar3.setOnButton1Click {
            binding.statusText.text = getString(R.string.status_vertical_1)
        }
        binding.verticalBar3.setOnButton2Click {
            binding.statusText.text = getString(R.string.status_vertical_2)
        }
        binding.verticalBar3.setOnButton3Click {
            binding.statusText.text = getString(R.string.status_vertical_3)
        }

        // Expandable animated bar
        binding.expandableBar.setOnExpandChangeListener { isExpanded ->
            binding.statusText.text = getString(R.string.status_expandable_changed, isExpanded)
        }
        binding.expandableBar.setOnButton2Click {
            binding.statusText.text = getString(R.string.status_expandable_2)
        }
        binding.expandableBar.setOnButton3Click {
            binding.statusText.text = getString(R.string.status_expandable_3)
        }

        // Pill button bar
        binding.pillNextBar.setOnPillClick {
            val isNowActive = !binding.pillNextBar.isPillActivated()
            binding.pillNextBar.setPillActivated(isNowActive)
            binding.statusText.text = getString(R.string.status_pill_next_clicked, isNowActive)
        }
    }
}
