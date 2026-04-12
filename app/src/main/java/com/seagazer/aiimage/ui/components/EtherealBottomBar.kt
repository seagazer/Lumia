package com.seagazer.aiimage.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.seagazer.aiimage.R

enum class EtherealTab { Create, Gallery, Settings }

enum class ResultTabHighlight { Create, Gallery }

@Composable
fun EtherealBottomBar(
    selected: EtherealTab,
    resultHighlight: ResultTabHighlight?,
    onSelect: (EtherealTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val effective = when (resultHighlight) {
        ResultTabHighlight.Gallery -> EtherealTab.Gallery
        ResultTabHighlight.Create -> EtherealTab.Create
        null -> selected
    }

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        NavigationBarItem(
            selected = effective == EtherealTab.Create,
            onClick = { onSelect(EtherealTab.Create) },
            icon = {
                Icon(
                    if (effective == EtherealTab.Create) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.nav_create)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = effective == EtherealTab.Gallery,
            onClick = { onSelect(EtherealTab.Gallery) },
            icon = {
                Icon(
                    if (effective == EtherealTab.Gallery) Icons.Filled.GridView else Icons.Outlined.GridView,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.nav_gallery)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = effective == EtherealTab.Settings,
            onClick = { onSelect(EtherealTab.Settings) },
            icon = {
                Icon(
                    if (effective == EtherealTab.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.nav_settings)) },
            colors = itemColors,
        )
    }
}
