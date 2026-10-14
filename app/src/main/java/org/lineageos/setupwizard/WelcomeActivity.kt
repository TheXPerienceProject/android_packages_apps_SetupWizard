/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.setupwizard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.updateLayoutParams
import com.google.android.material.button.MaterialButton
import com.google.android.setupcompat.util.SystemBarHelper
import com.google.android.setupdesign.R as SudR
import com.google.android.setupdesign.template.FloatingActionButtonMixin
import com.google.android.setupdesign.template.FloatingBackButtonMixin
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import org.lineageos.setupwizard.base.GlifTemplate
import org.lineageos.setupwizard.base.SubBaseActivity
import org.lineageos.setupwizard.system.DeviceInfoActivity
import org.lineageos.setupwizard.util.SetupWizardUtils

class WelcomeActivity : SubBaseActivity() {

    private val isSkipAvailable: Boolean
        get() = Build.TYPE == "eng"

    override fun onStartSubactivity() {
        // no-op
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onSetupStart()
        SystemBarHelper.setBackButtonVisible(window, false)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )

        if (template is GlifTemplate) {
            setupEmergencyCallButton()
            setupDeviceInfoButton()
            setupSkipButton()
        }

        findViewById<View>(R.id.start).setOnClickListener { onNextPressed() }
        findViewById<View>(R.id.launch_accessibility).setOnClickListener {
            startSubactivity(Intent(ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<View>(R.id.launch_brightness)?.setOnClickListener {
            showBrightnessDialog()
        }

        val welcomeTitle: TextView = findViewById(R.id.welcome_title)
        welcomeTitle.text =
            if (SetupWizardUtils.isManagedProfile(this)) {
                getString(R.string.setup_managed_profile_welcome_message)
            } else {
                getString(R.string.setup_welcome_message, getString(R.string.os_name))
            }
    }

    private fun MaterialButton.applyChipStyle(label: CharSequence) {
        text = label
        contentDescription = null
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            resources.getDimension(R.dimen.welcome_chip_text_size),
        )
        iconTint?.let { setTextColor(it) }
        minWidth = 0
        val horizontalPadding =
            resources.getDimensionPixelSize(R.dimen.welcome_chip_padding_horizontal)
        setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)
    }

    private fun setupDeviceInfoButton() {
        val actionButtonContainer =
            glifLayout.findManagedViewById<View>(
                SudR.id.sud_layout_floating_action_button_container
            )
        val row = actionButtonContainer?.parent as? ViewGroup ?: return
        val infoButton = layoutInflater.inflate(R.layout.welcome_info_button, row, false)
        row.addView(infoButton, 0)
        infoButton.setOnClickListener {
            startActivity(Intent(this, DeviceInfoActivity::class.java))
        }

        val edgeMargin = backButtonMarginStart()
        actionButtonContainer.updateLayoutParams<MarginLayoutParams> { marginEnd = edgeMargin }
        infoButton.updateLayoutParams<MarginLayoutParams> {
            marginEnd =
                if (isSkipAvailable) {
                    resources.getDimensionPixelSize(R.dimen.welcome_top_button_spacing)
                } else {
                    edgeMargin
                }
        }
    }

    private fun backButtonMarginStart(): Int {
        val attributes =
            theme.obtainStyledAttributes(intArrayOf(SudR.attr.sudBackButtonMarginStart))
        val margin = attributes.getDimensionPixelSize(0, 0)
        attributes.recycle()
        return margin
    }

    private fun setupSkipButton() {
        val mixin = glifLayout.getMixin(FloatingActionButtonMixin::class.java)
        if (!isSkipAvailable) {
            mixin.visibility = View.GONE
            return
        }
        mixin.visibility = View.VISIBLE
        mixin.onClickListener =
            View.OnClickListener { SetupWizardUtils.finishSetupWizard(this@WelcomeActivity) }
        mixin.actionButton?.applyChipStyle(getString(R.string.skip))
    }

    private fun setupEmergencyCallButton() {
        val mixin = glifLayout.getMixin(FloatingBackButtonMixin::class.java)
        if (!SetupWizardUtils.hasTelephony(this)) {
            mixin.setVisibility(View.GONE)
            return
        }
        mixin.setVisibility(View.VISIBLE)
        mixin.setOnClickListener { startSubactivity(Intent(ACTION_EMERGENCY_DIAL)) }

        val button =
            glifLayout.findManagedViewById<MaterialButton>(SudR.id.sud_floating_back_button)
                ?: return
        button.apply {
            setIconResource(R.drawable.ic_emergency_dial)
            iconSize = resources.getDimensionPixelSize(R.dimen.welcome_emergency_icon_size)
            iconPadding = resources.getDimensionPixelSize(R.dimen.welcome_emergency_icon_padding)
            applyChipStyle(getString(R.string.emergency_call))
        }
    }

    private fun showBrightnessDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_brightness, null)
        val slider = dialogView.findViewById<SeekBar>(R.id.brightness_slider)
        val adaptiveSwitch = dialogView.findViewById<Switch>(R.id.adaptive_brightness_switch)

        val currentMode = Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        adaptiveSwitch.isChecked = currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

        val currentBrightness = Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            128,
        )
        slider.progress = maxOf(5, currentBrightness)

        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val value = maxOf(5, progress)
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
                    val lp = window.attributes
                    lp.screenBrightness = value / 255f
                    window.attributes = lp
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        adaptiveSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (isChecked) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.display_brightness)
            .setView(dialogView)
            .setPositiveButton(R.string.done, null)
            .show()
    }

    override val layoutResId = R.layout.welcome_activity

    override val titleResId = -1

    companion object {
        private const val ACTION_ACCESSIBILITY_SETTINGS =
            "android.settings.ACCESSIBILITY_SETTINGS_FOR_SUW"
    }
}
