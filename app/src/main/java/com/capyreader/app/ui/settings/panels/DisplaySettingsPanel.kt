package com.capyreader.app.ui.settings.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.capyreader.app.R
import com.capyreader.app.common.ImagePreview
import com.capyreader.app.common.RowItem
import com.capyreader.app.common.ThemeOption
import com.capyreader.app.ui.articles.ArticleListFontScale
import com.capyreader.app.ui.components.FormSection
import com.capyreader.app.ui.components.TextSwitch
import com.capyreader.app.ui.settings.PreferenceSelect
import com.capyreader.app.ui.theme.CapyTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun DisplaySettingsPanel(
    viewModel: DisplaySettingsViewModel = koinViewModel(),
) {
    DisplaySettingsPanelView(
        onUpdateTheme = viewModel::updateTheme,
        theme = viewModel.theme,
        enableHighContrastDarkTheme = viewModel.enableHighContrastDarkTheme,
        updateHighContrastDarkTheme = viewModel::updateHighContrastDarkTheme,
        readerOptions = ReaderOptions(
            updateStickyFullContent = viewModel::updateStickyFullContent,
            updatePinTopBar = viewModel::updatePinTopBar,
            pinTopBar = viewModel.pinArticleTopBar,
            enableStickyFullContent = viewModel.enableStickyFullContent,
        ),
        articleListOptions = ArticleListOptions(
            imagePreview = viewModel.imagePreview,
            showSummary = viewModel.showSummary,
            fontScale = viewModel.fontScale,
            showFeedIcons = viewModel.showFeedIcons,
            showFeedName = viewModel.showFeedName,
            confirmMarkAllRead = viewModel.confirmMarkAllRead,
            updateImagePreview = viewModel::updateImagePreview,
            updateSummary = viewModel::updateSummary,
            updateFeedName = viewModel::updateFeedName,
            updateFeedIcons = viewModel::updateFeedIcons,
            updateFontScale = viewModel::updateFontScale,
            updateConfirmMarkAllRead = viewModel::updateConfirmMarkAllRead,
        )
    )
}

@Composable
fun DisplaySettingsPanelView(
    onUpdateTheme: (theme: ThemeOption) -> Unit,
    enableHighContrastDarkTheme: Boolean,
    updateHighContrastDarkTheme: (enabled: Boolean) -> Unit,
    theme: ThemeOption,
    readerOptions: ReaderOptions,
    articleListOptions: ArticleListOptions,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Column {
            PreferenceSelect(
                selected = theme,
                update = onUpdateTheme,
                options = ThemeOption.sorted,
                label = R.string.theme_menu_label,
                optionText = {
                    stringResource(it.translationKey)
                }
            )

            RowItem {
                TextSwitch(
                    onCheckedChange = updateHighContrastDarkTheme,
                    checked = enableHighContrastDarkTheme,
                    title = {
                        Text(stringResource(R.string.settings_enable_high_contrast_dark_theme))
                    }
                )
            }
        }

        FormSection(title = stringResource(R.string.settings_reader_title)) {
            RowItem {
                ReaderSettings(options = readerOptions)
            }
        }

        FormSection(
            title = stringResource(R.string.settings_article_list_title)
        ) {
            ArticleListSettings(
                options = articleListOptions
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun DisplaySettingsPanelViewPreview() {
    CapyTheme {
        DisplaySettingsPanelView(
            onUpdateTheme = {},
            theme = ThemeOption.SYSTEM_DEFAULT,
            enableHighContrastDarkTheme = true,
            updateHighContrastDarkTheme = {},
            readerOptions = ReaderOptions(
                updateStickyFullContent = {},
                enableStickyFullContent = true,
                updatePinTopBar = {},
                pinTopBar = true,
            ),
            articleListOptions = ArticleListOptions(
                imagePreview = ImagePreview.default,
                showSummary = true,
                fontScale = ArticleListFontScale.MEDIUM,
                showFeedIcons = true,
                showFeedName = false,
                confirmMarkAllRead = true,
                updateImagePreview = {},
                updateSummary = {},
                updateFeedName = {},
                updateFeedIcons = {},
                updateFontScale = {},
                updateConfirmMarkAllRead = {},
            )
        )
    }
}
