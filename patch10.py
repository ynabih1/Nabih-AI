with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

import re

# We want to replace the onboarding card and button section.
# Instead of doing massive string replace, let's use regex for specific parts.

# 1. Skip button spacing above card (Header Area)
# Let's find Spacer(modifier = Modifier.height(48.dp)) at the top and replace with 32.dp
# Wait, it's safer to just replace it directly.
content = content.replace("Spacer(modifier = Modifier.height(48.dp))\n            // Header: App Logo & Name", "Spacer(modifier = Modifier.height(32.dp))\n            // Header: App Logo & Name")

# 2. Main Interactive Content padding
content = content.replace(".padding(top = 24.dp, bottom = 24.dp)", ".padding(top = 16.dp, bottom = 16.dp)")

# 3. Card padding and styling
content = content.replace(".padding(24.dp),\n                                        horizontalAlignment = Alignment.CenterHorizontally", ".padding(32.dp),\n                                        horizontalAlignment = Alignment.CenterHorizontally")

content = content.replace("CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)", "CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)")

# Card icon size
content = content.replace(".size(56.dp)\n                                                .clip(CircleShape)", ".size(72.dp)\n                                                .shadow(2.dp, CircleShape)\n                                                .clip(CircleShape)")

content = content.replace("modifier = Modifier.size(28.dp)", "modifier = Modifier.size(32.dp)")

content = content.replace("Spacer(modifier = Modifier.height(16.dp))", "Spacer(modifier = Modifier.height(20.dp))")

# Description spacing
content = content.replace("Spacer(modifier = Modifier.height(8.dp))\n                                        Text(\n                                            text = activeSlide.description", "Spacer(modifier = Modifier.height(12.dp))\n                                        Text(\n                                            text = activeSlide.description")

# 4. Skip button positioning
# The skip button is currently in a Row.
skip_row = """                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (activeSlideIndex < onboardingSlides.size - 1) {
                                        Text(
                                            text = if (isArabic) "تخطي" else "Skip",
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .clickable {
                                                    settingsViewModel.updateOnboardingCompleted(true)
                                                    currentStep = 1
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("onboarding_skip_button")
                                        )
                                    }
                                }"""

new_skip = """                                Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                    if (activeSlideIndex < onboardingSlides.size - 1) {
                                        Text(
                                            text = if (isArabic) "تخطي" else "Skip",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    settingsViewModel.updateOnboardingCompleted(true)
                                                    currentStep = 1
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .testTag("onboarding_skip_button")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))"""
content = content.replace(skip_row, new_skip)

# 5. Onboarding dots spacing and size
dots_row = """                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    onboardingSlides.forEachIndexed { idx, _ ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (activeSlideIndex == idx) 16.dp else 8.dp, 8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (activeSlideIndex == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                                                )
                                                .clickable { activeSlideIndex = idx }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))"""

new_dots_row = """                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 20.dp)
                                ) {
                                    onboardingSlides.forEachIndexed { idx, _ ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (activeSlideIndex == idx) 24.dp else 8.dp, 8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (activeSlideIndex == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                )
                                                .clickable { activeSlideIndex = idx }
                                        )
                                    }
                                }"""
content = content.replace(dots_row, new_dots_row)

# 6. Next button
button_old = """                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("onboarding_continue_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {"""
button_new = """                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .shadow(2.dp, RoundedCornerShape(16.dp))
                                        .testTag("onboarding_continue_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {"""
content = content.replace(button_old, button_new)

# 7. Footer
footer_old = """            Spacer(modifier = Modifier.height(48.dp))

            // Footer / Bottom Brand Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Nabih Secure Authentication",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                )
                Text(
                    text = if (isArabic) "تشفير محلي آمن بالكامل 256-بت" else "Full 256-bit Local Secure Encryption",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                    fontSize = 10.sp
                )
            }"""

footer_new = """            Spacer(modifier = Modifier.height(32.dp))

            // Footer / Bottom Brand Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Nabih Secure Authentication",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
                Text(
                    text = if (isArabic) "تشفير محلي آمن بالكامل 256-بت" else "Full 256-bit Local Secure Encryption",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    fontSize = 9.sp
                )
            }"""
content = content.replace(footer_old, footer_new)

# Subtitles
content = content.replace('color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)', 'color = MaterialTheme.colorScheme.onSurfaceVariant')


with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
