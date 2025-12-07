package com.fghbuild.sidekick.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fghbuild.sidekick.R
import java.util.Calendar

@Composable
fun onboardingScreen(
    modifier: Modifier = Modifier,
    onBirthYearSubmit: (Int) -> Unit = {},
) {
    // Memoize current year calculation to avoid repeated Calendar.getInstance() calls
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var birthYearInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome),
            fontSize = 32.sp,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_description),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = birthYearInput,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    birthYearInput = newValue
                    isError = false
                }
            },
            label = { Text(stringResource(R.string.onboarding_birth_year)) },
            placeholder = { Text("e.g., ${currentYear - 30}") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isError,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("birthYearInput"),
            singleLine = true,
        )

        if (isError) {
            Text(
                text = stringResource(R.string.onboarding_invalid_year, currentYear),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp, start = 16.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val year = birthYearInput.toIntOrNull()
                if (year != null && year in 1900..currentYear) {
                    onBirthYearSubmit(year)
                } else {
                    isError = true
                }
            },
            modifier = Modifier.fillMaxWidth(0.6f),
        ) {
            Text(stringResource(R.string.onboarding_get_started))
        }
    }
}
