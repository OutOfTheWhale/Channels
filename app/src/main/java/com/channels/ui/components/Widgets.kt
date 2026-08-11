package com.channels.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.channels.ui.theme.Ash
import com.channels.ui.theme.Hairline
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Slate

/** A single-line search field styled monochrome, submitting on the IME action. */
@Composable
fun LightSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Ash) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink,
            unfocusedBorderColor = Hairline,
            cursorColor = Ink,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A tappable star glyph: filled when starred. */
@Composable
fun StarToggle(
    starred: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (starred) "★" else "☆", // ★ / ☆
        style = MaterialTheme.typography.headlineMedium,
        color = Ink,
        modifier = modifier
            .clickable(onClick = onToggle)
            .padding(8.dp),
    )
}

/** Small uppercase section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Slate,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * A generic two-line list row. The second line shows [subtitle] on the left (with a
 * downloaded ⤓ mark right after it when [downloaded]) and [endText] (e.g. duration)
 * right-aligned, then an optional [trailing] slot at the far right.
 */
@Composable
fun ListRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    downloaded: Boolean = false,
    endText: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null || endText != null || downloaded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        if (downloaded) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "⤓",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Ink,
                            )
                        }
                    }
                    if (endText != null) {
                        Text(
                            text = endText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(color = Hairline, thickness = 1.dp, modifier = Modifier.padding(start = 16.dp))
}

/** A back chevron + title header used by the Library sub-screens, with an optional trailing action. */
@Composable
fun BackHeader(title: String, onBack: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "‹",
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
            modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 8.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
    RowDivider()
}

/** Centered status text (loading / empty / error). */
@Composable
fun CenteredNote(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = LocalTextStyle.current.merge(TextStyle(color = Slate)),
            color = Slate,
        )
    }
}
