package com.gayathrini.chatapp.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Reusable, elderly-friendly building blocks (TDD §8.4/§8.6): large touch targets (≥48 dp,
// primary actions ≥56 dp), comfortable text, explicit Loading / Empty / Error states.

private val MinTarget = 48.dp
private val PrimaryHeight = 56.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PrimaryHeight),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            isError = error != null,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTarget),
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onAction, modifier = Modifier.heightIn(min = MinTarget)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Spacer(Modifier.heightIn(min = 8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.heightIn(min = 16.dp))
            PrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun MessageBubble(
    text: String,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
) {
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor =
        if (isOutgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Surface(
            color = bubbleColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(alignment).sizeIn(maxWidth = 300.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(PaddingValues(horizontal = 16.dp, vertical = 10.dp)),
            )
        }
    }
}
