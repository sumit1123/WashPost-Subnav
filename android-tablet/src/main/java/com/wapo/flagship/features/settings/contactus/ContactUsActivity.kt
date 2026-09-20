package com.wapo.flagship.features.settings.contactus

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.wapo.android.commons.util.applySystemBarPadding
import com.wapo.zendesk.model.ZendeskDestinations
import com.wapo.zendesk.viewmodel.ZendeskDestinationViewModel
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivityContactUsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * This activity can be directly opened for contact us native screen without having to go through settings screen.
 */
@AndroidEntryPoint
class ContactUsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactUsBinding

    private var navController: NavController? = null

    private var navHostFragment: NavHostFragment? = null

    private val zendeskDestinationViewModel: ZendeskDestinationViewModel by viewModels()

    /**
     * Any destinations that are not required to pop off the backstack.
     */
    private val lastDestinations =
        HashSet<Int>().apply {
            add(R.id.zendeskSuccessFragment)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityContactUsBinding.inflate(layoutInflater)
        binding.root.applySystemBarPadding()
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment?
        navController = navHostFragment?.navController
        observeZendeskDestinationEvents()
    }

    private fun observeZendeskDestinationEvents() {
        zendeskDestinationViewModel.destination.observe(this) {
            when (it) {
                ZendeskDestinations.Finish -> finish()
                ZendeskDestinations.Resubmit -> {
                    val action =
                        com.wapo.zendesk.fragments.ZendeskSuccessFragmentDirections
                            .settingsZendeskAnotherRequest()
                    navHostFragment?.navController?.navigate(action)
                }
                ZendeskDestinations.SubmitSuccess -> {
                    val action =
                        com.wapo.zendesk.fragments.ZendeskFormFragmentDirections
                            .settingsZendeskSuccess()
                    navHostFragment?.navController?.navigate(action)
                }
                else -> {
                    /*
                        Do nothing for other cases.
                     */
                }
            }
        }
    }

    override fun setTheme(resId: Int) {
        super.setTheme(R.style.WaPo_Settings)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isLastDestination()) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Any destinations that are not required to pop off the backstack.
     *   In such cases we just close out the workflow.
     */
    private fun isLastDestination(): Boolean =
        navController?.currentDestination?.id != null &&
            lastDestinations.contains(
                navController?.currentDestination?.id,
            )
}
