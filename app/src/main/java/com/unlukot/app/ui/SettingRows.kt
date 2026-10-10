package com.unlukot.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
private fun RowLabel(title: String, subtitle: String?, modifier: Modifier, titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = titleColor)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SwitchSetting(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        RowLabel(title, subtitle, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun <T> ChoiceSetting(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            RowLabel(title, subtitle, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        PillSelector(options, selected, label, onSelect)
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        RowLabel(title, null, Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ActionRow(icon: ImageVector, title: String, enabled: Boolean = true, destructive: Boolean = false, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val color = when {
        !enabled -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
        destructive -> scheme.error
        else -> scheme.onSurface
    }
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(
            icon, size = 40.dp,
            container = if (destructive && enabled) scheme.errorContainer else scheme.primaryContainer,
            tint = if (destructive && enabled) scheme.error else color
        )
        Spacer(Modifier.width(14.dp))
        RowLabel(title, null, Modifier.weight(1f), titleColor = color)
    }
}
