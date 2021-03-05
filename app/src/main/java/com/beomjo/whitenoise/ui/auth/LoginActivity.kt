package com.beomjo.whitenoise.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.beomjo.whitenoise.R
import com.beomjo.whitenoise.base.BaseActivity
import com.beomjo.whitenoise.databinding.ActivityLoginBinding
import com.beomjo.whitenoise.factory.ViewModelFactory
import com.beomjo.whitenoise.ui.main.MainActivity
import com.beomjo.whitenoise.utilities.ext.getComponent
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@FlowPreview
class LoginActivity : BaseActivity<ActivityLoginBinding, LoginViewModel>(R.layout.activity_login) {

    private val requestGoogleLogin: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            activityResult.data?.let(viewModel::processGoogleLogin)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingViewModel()
    }

    override fun inject() {
        application.getComponent().authComponent().create().inject(this@LoginActivity)
    }

    private fun bindingViewModel() {
        binding {
            vm = viewModel.apply {
                googleLoginIntent.observe(this@LoginActivity, requestGoogleLogin::launch)
                loginSuccess.observe(this@LoginActivity) { moveToMainActivity() }
            }
        }
    }

    private fun moveToMainActivity() {
        finish()
        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
    }
}