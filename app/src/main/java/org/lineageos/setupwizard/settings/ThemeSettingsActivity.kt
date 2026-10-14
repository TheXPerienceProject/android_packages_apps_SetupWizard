/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.settings

import android.app.UiModeManager
import android.app.WallpaperManager
import android.content.res.Configuration
import android.graphics.Outline
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import org.json.JSONArray
import org.json.JSONObject
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.updateCheckedIcons
import org.lineageos.setupwizard.widget.ColorSwatchView

class ThemeSettingsActivity : BaseSetupWizardActivity() {

    private val uiModeManager by lazy { getSystemService(UiModeManager::class.java) }
    private lateinit var blackThemeCard: View

    private data class ColorSwatch(
        val id: String,
        val topColor: Int,
        val bottomColor: Int,
        val seedColor: Int?,
        val isWallpaper: Boolean = false,
        val title: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val modeGroup = findViewById<MaterialButtonToggleGroup>(R.id.theme_mode_group)
        val blackTheme = findViewById<MaterialSwitch>(R.id.black_theme)
        blackThemeCard = findViewById(R.id.black_theme_card)
        roundPreviewWallpaper(findViewById(R.id.theme_preview_wallpaper))
        val isNight =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        blackTheme.isChecked =
            Settings.Secure.getInt(
                contentResolver,
                "berry_black_theme",
                0,
            ) != 0
        blackTheme.setOnCheckedChangeListener { _, checked ->
            Settings.Secure.putInt(
                contentResolver,
                "berry_black_theme",
                if (checked) 1 else 0,
            )
        }

        modeGroup.check(if (isNight) R.id.mode_dark else R.id.mode_light)
        modeGroup.updateCheckedIcons(R.drawable.ic_check)

        modeGroup.addOnButtonCheckedListener { _, checkedId, checked ->
            if (!checked) {
                return@addOnButtonCheckedListener
            }
            val night = checkedId == R.id.mode_dark
            modeGroup.updateCheckedIcons(R.drawable.ic_check)
            setBlackThemeRevealed(night)
            uiModeManager.setNightModeActivated(night)
        }

        setBlackThemeRevealed(isNight)
        setupColorPalette()
    }

    private fun setupColorPalette() {
        val container = findViewById<LinearLayout>(R.id.color_swatches_container) ?: return
        container.removeAllViews()

        var wpTop = 0xFFF5BA9D.toInt()
        var wpBottom = 0xFFC4965A.toInt()
        try {
            val wpManager = getSystemService(WallpaperManager::class.java)
            val wpColors = wpManager?.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            if (wpColors != null) {
                val primary = wpColors.primaryColor.toArgb()
                val secondary = wpColors.secondaryColor?.toArgb() ?: primary
                wpBottom = primary
                wpTop = secondary
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get wallpaper colors", e)
        }

        val wpSwatch = ColorSwatch(
            id = "wallpaper",
            topColor = wpTop,
            bottomColor = wpBottom,
            seedColor = null,
            isWallpaper = true,
            title = getString(R.string.theme_color_wallpaper)
        )

        val presetSwatches = listOf(
            ColorSwatch(
                id = "slate",
                topColor = 0xFFFFFFFF.toInt(),
                bottomColor = 0xFF757575.toInt(),
                seedColor = 0xFF607D8B.toInt(),
                title = getString(R.string.theme_color_slate)
            ),
            ColorSwatch(
                id = "lavender",
                topColor = 0xFFF3E5F5.toInt(),
                bottomColor = 0xFF8E24AA.toInt(),
                seedColor = 0xFF8E24AA.toInt(),
                title = getString(R.string.theme_color_lavender)
            ),
            ColorSwatch(
                id = "crimson",
                topColor = 0xFFFF8A80.toInt(),
                bottomColor = 0xFF9E2B45.toInt(),
                seedColor = 0xFFD32F2F.toInt(),
                title = getString(R.string.theme_color_crimson)
            ),
            ColorSwatch(
                id = "ocean",
                topColor = 0xFFEF9A9A.toInt(),
                bottomColor = 0xFF0288D1.toInt(),
                seedColor = 0xFF0288D1.toInt(),
                title = getString(R.string.theme_color_ocean)
            ),
            ColorSwatch(
                id = "mint",
                topColor = 0xFF80DEEA.toInt(),
                bottomColor = 0xFF00695C.toInt(),
                seedColor = 0xFF00897B.toInt(),
                title = getString(R.string.theme_color_mint)
            ),
            ColorSwatch(
                id = "forest",
                topColor = 0xFFDCEDC8.toInt(),
                bottomColor = 0xFF33691E.toInt(),
                seedColor = 0xFF388E3C.toInt(),
                title = getString(R.string.theme_color_forest)
            ),
            ColorSwatch(
                id = "amber",
                topColor = 0xFFFFE0B2.toInt(),
                bottomColor = 0xFFE65100.toInt(),
                seedColor = 0xFFFB8C00.toInt(),
                title = getString(R.string.theme_color_amber)
            ),
            ColorSwatch(
                id = "indigo",
                topColor = 0xFFC5CAE9.toInt(),
                bottomColor = 0xFF283593.toInt(),
                seedColor = 0xFF3F51B5.toInt(),
                title = getString(R.string.theme_color_indigo)
            )
        )

        val allSwatches = listOf(wpSwatch) + presetSwatches

        var selectedSwatchId = "wallpaper"
        val currentJsonStr = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES
        )
        if (!currentJsonStr.isNullOrEmpty()) {
            try {
                val json = JSONObject(currentJsonStr)
                val source = json.optString("android.theme.customization.color_source", "home_wallpaper")
                if (source == "preset") {
                    val palette = json.optString("android.theme.customization.system_palette", "")
                    val cleanPalette = palette.removePrefix("#")
                    val matched = presetSwatches.find { swatch ->
                        swatch.seedColor != null &&
                            String.format("%06X", 0xFFFFFF and swatch.seedColor)
                                .equals(cleanPalette, ignoreCase = true)
                    }
                    if (matched != null) {
                        selectedSwatchId = matched.id
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse theme customization settings", e)
            }
        }

        val swatchViews = mutableListOf<ColorSwatchView>()
        val margin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            5f,
            resources.displayMetrics
        ).toInt()
        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            56f,
            resources.displayMetrics
        ).toInt()

        for (swatch in allSwatches) {
            val swatchView = ColorSwatchView(this).apply {
                topColor = swatch.topColor
                bottomColor = swatch.bottomColor
                ringColor = swatch.topColor
                contentDescription = swatch.title
                isSwatchSelected = (swatch.id == selectedSwatchId)

                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, 0, margin, 0)
                }

                setOnClickListener {
                    if (isSwatchSelected) return@setOnClickListener
                    for (v in swatchViews) {
                        v.isSwatchSelected = false
                    }
                    isSwatchSelected = true
                    applyThemeAsync(
                        colorSource = if (swatch.isWallpaper) "home_wallpaper" else "preset",
                        seedColor = swatch.seedColor
                    )
                }
            }
            swatchViews.add(swatchView)
            container.addView(swatchView)
        }
    }

    private fun applyThemeAsync(colorSource: String, seedColor: Int?) {
        val currentJsonStr = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES
        ) ?: "{}"

        Thread {
            try {
                val json = if (currentJsonStr.isNotEmpty()) JSONObject(currentJsonStr) else JSONObject()
                json.put("_applied_timestamp", System.currentTimeMillis())
                json.put("android.theme.customization.color_source", colorSource)
                json.put("android.theme.customization.theme_style", "TONAL_SPOT")

                if (colorSource == "preset" && seedColor != null) {
                    val hex = String.format("%06X", 0xFFFFFF and seedColor)
                    json.put("android.theme.customization.system_palette", hex)
                    json.put("android.theme.customization.accent_color", hex)
                    val seedList = JSONArray()
                    seedList.put(hex)
                    json.put("android.theme.customization.seed_color_list", seedList)
                    json.remove("android.theme.customization.color_both")
                } else {
                    json.remove("android.theme.customization.system_palette")
                    json.remove("android.theme.customization.accent_color")
                    json.remove("android.theme.customization.seed_color_list")
                    json.put("android.theme.customization.color_both", "1")
                }

                Settings.Secure.putString(
                    contentResolver,
                    Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                    json.toString()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply theme customization", e)
            }
        }.start()
    }

    private fun roundPreviewWallpaper(wallpaper: ImageView) {
        wallpaper.outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val artwork = wallpaper.drawable ?: return
                    val scale =
                        minOf(
                            view.width.toFloat() / artwork.intrinsicWidth,
                            view.height.toFloat() / artwork.intrinsicHeight,
                        )
                    val width = (artwork.intrinsicWidth * scale).toInt()
                    val height = (artwork.intrinsicHeight * scale).toInt()
                    val left = (view.width - width) / 2
                    val top = (view.height - height) / 2
                    val radius = width * PREVIEW_CORNER_RADIUS / PREVIEW_WIDTH
                    outline.setRoundRect(
                        left,
                        top,
                        left + width,
                        top + height + radius.toInt(),
                        radius,
                    )
                }
            }
        wallpaper.clipToOutline = true
    }

    private fun setBlackThemeRevealed(revealed: Boolean) {
        blackThemeCard.visibility = if (revealed) View.VISIBLE else View.GONE
    }

    override val layoutResId = R.layout.setup_theme

    override val titleResId = R.string.setup_theme

    override val iconResId = R.drawable.ic_palette

    companion object {
        private const val TAG = "ThemeSettingsActivity"
        // Both taken from the preview artwork's viewport, see theme_preview_qs.xml.
        private const val PREVIEW_WIDTH = 412f
        private const val PREVIEW_CORNER_RADIUS = 28f
    }
}
