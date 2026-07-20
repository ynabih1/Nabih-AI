with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'r') as f:
    text = f.read()

import re

# Remove the Spacer that pushes everything down
text = re.sub(r'Spacer\(modifier = Modifier.weight\(1f\)\) // Pushes the rest to the bottom', 'Spacer(modifier = Modifier.height(48.dp))', text)

# Remove verticalScroll from the inner Box and put it on the root Column
# We will use a regex to modify the Column
old_column = """        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp), // Removed verticalScroll to allow weight to push content down
            horizontalAlignment = Alignment.CenterHorizontally
        ) {"""

new_column = """        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {"""

text = text.replace(old_column, new_column)

# Also remove verticalScroll from the Box
old_box = """            // Main Interactive Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp)
                    .verticalScroll(rememberScrollState()), // Moved scroll state here
                contentAlignment = Alignment.TopCenter
            ) {"""

new_box = """            // Main Interactive Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {"""

text = text.replace(old_box, new_box)

with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'w') as f:
    f.write(text)
