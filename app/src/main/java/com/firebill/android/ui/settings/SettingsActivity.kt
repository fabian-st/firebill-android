package com.firebill.android.ui.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firebill.android.R
import com.firebill.android.data.api.ApiClient
import com.firebill.android.data.repository.BillRepository
import com.firebill.android.databinding.ActivitySettingsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val repository by lazy { BillRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        // Load current server URL
        binding.editServerUrl.setText(ApiClient.getServerUrl(this))

        binding.buttonSaveSettings.setOnClickListener { saveSettings() }
        binding.buttonTestConnection.setOnClickListener { testConnection() }

        loadRemoteSettings()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadRemoteSettings() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            repository.getSettings()
                .onSuccess { settings ->
                    settings.fireflyUrl?.let { binding.editFireflyUrl.setText(it) }
                    settings.fireflyToken?.let { binding.editFireflyToken.setText(it) }
                }
                .onFailure {
                    // Not critical if remote settings can't be loaded
                }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun saveSettings() {
        val serverUrl = binding.editServerUrl.text?.toString()?.trim()
        if (serverUrl.isNullOrBlank()) {
            binding.layoutServerUrl.error = getString(R.string.error_url_required)
            return
        }
        binding.layoutServerUrl.error = null

        // Save server URL locally
        ApiClient.saveServerUrl(this, serverUrl)

        // Save Firefly settings remotely
        val fireflyUrl = binding.editFireflyUrl.text?.toString()?.trim()
        val fireflyToken = binding.editFireflyToken.text?.toString()?.trim()

        if (!fireflyUrl.isNullOrBlank() || !fireflyToken.isNullOrBlank()) {
            lifecycleScope.launch {
                binding.progressBar.visibility = View.VISIBLE
                repository.updateSettings(
                    com.firebill.android.data.api.models.UpdateSettingsRequest(
                        fireflyUrl = fireflyUrl,
                        fireflyToken = fireflyToken
                    )
                )
                    .onSuccess {
                        Snackbar.make(binding.root, R.string.settings_saved, Snackbar.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Snackbar.make(binding.root, it.message ?: getString(R.string.save_failed), Snackbar.LENGTH_LONG).show()
                    }
                binding.progressBar.visibility = View.GONE
            }
        } else {
            Snackbar.make(binding.root, R.string.server_url_saved, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun testConnection() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonTestConnection.isEnabled = false
        binding.textConnectionStatus.visibility = View.GONE

        lifecycleScope.launch {
            repository.testConnection()
                .onSuccess { response ->
                    binding.textConnectionStatus.visibility = View.VISIBLE
                    binding.textConnectionStatus.text = if (response.ok) {
                        getString(R.string.connection_success)
                    } else {
                        response.message ?: getString(R.string.connection_failed)
                    }
                    binding.textConnectionStatus.setTextColor(
                        if (response.ok) getColor(R.color.success_green)
                        else getColor(R.color.error_red)
                    )
                }
                .onFailure { error ->
                    binding.textConnectionStatus.visibility = View.VISIBLE
                    binding.textConnectionStatus.text = getString(R.string.connection_error, error.message)
                    binding.textConnectionStatus.setTextColor(getColor(R.color.error_red))
                }
            binding.progressBar.visibility = View.GONE
            binding.buttonTestConnection.isEnabled = true
        }
    }
}
