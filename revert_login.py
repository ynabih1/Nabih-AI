with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

# Revert google logo
content = content.replace("R.drawable.ic_google_logo", "R.drawable.ic_google")
# Revert icon size
content = content.replace('contentDescription = "Google Icon",\n                                            tint = Color.Unspecified,\n                                            modifier = Modifier.size(20.dp)', 'contentDescription = "Google Icon",\n                                            tint = Color.Unspecified,\n                                            modifier = Modifier.size(24.dp)')

# Revert google button
bad_google = """                                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .shadow(1.dp, RoundedCornerShape(14.dp))
                                        .testTag("google_login_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {"""

good_google = """                                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .shadow(1.dp, RoundedCornerShape(12.dp))
                                        .testTag("google_login_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF1F1F1F)
                                    )
                                ) {"""

content = content.replace(bad_google, good_google)

# Revert patch9 and patch10
bad_header = """            Spacer(modifier = Modifier.height(32.dp))
            // Header: App Logo & Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(72.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = if (isArabic) "الجيل القادم من الذكاء الاصطناعي" else "The Next Gen Intelligent Workspace",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Main Interactive Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedContent(
                    targetState = currentStep,"""

good_header = """            Spacer(modifier = Modifier.height(48.dp))
            // Header: App Logo & Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(72.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = if (isArabic) "الجيل القادم من الذكاء الاصطناعي" else "The Next Gen Intelligent Workspace",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Main Interactive Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedContent(
                    targetState = currentStep,"""
content = content.replace(bad_header, good_header)

bad_onboarding = """                        0 -> {
                            // Onboarding
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val activeSlide = onboardingSlides[activeSlideIndex]

                                Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
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
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .shadow(2.dp, CircleShape)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = activeSlide.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text(
                                            text = activeSlide.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = activeSlide.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }

                                // Indicator dots
                                Row(
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
                                }

                                Button(
                                    onClick = {
                                        if (activeSlideIndex < onboardingSlides.size - 1) {
                                            activeSlideIndex++
                                        } else {
                                            settingsViewModel.updateOnboardingCompleted(true)
                                            currentStep = 1
                                        }
                                    },
                                    modifier = Modifier
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

good_onboarding = """                        0 -> {
                            // Onboarding
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val activeSlide = onboardingSlides[activeSlideIndex]

                                Row(
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
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = activeSlide.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = activeSlide.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = activeSlide.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            lineHeight = 20.sp
                                        )
                                    }
                                }

                                // Indicator dots
                                Row(
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

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (activeSlideIndex < onboardingSlides.size - 1) {
                                            activeSlideIndex++
                                        } else {
                                            settingsViewModel.updateOnboardingCompleted(true)
                                            currentStep = 1
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("onboarding_continue_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {"""
content = content.replace(bad_onboarding, good_onboarding)

bad_footer = """            Spacer(modifier = Modifier.height(32.dp))

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

good_footer = """            Spacer(modifier = Modifier.height(48.dp))

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
content = content.replace(bad_footer, good_footer)

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
