#!/bin/bash
cd app/src/main/java/com/example

# Move files
mv feature/auth/* auth/ 2>/dev/null
mv feature/chat/* chat/ 2>/dev/null
mv feature/settings/* settings/ 2>/dev/null
mv core/theme/* ui/theme/ 2>/dev/null
mv core/database/* data/local/ 2>/dev/null
mv feature/memory/* data/local/ 2>/dev/null
mv core/network/* data/remote/ 2>/dev/null
mv core/di/* di/ 2>/dev/null
mv feature/diagnostic/* chat/ 2>/dev/null
mv feature/tools/* settings/ 2>/dev/null

mkdir -p data/repository model utils ui/components
mv chat/ChatRepository.kt data/repository/
mv settings/SettingsRepository.kt data/repository/
mv data/local/MemoryRepository.kt data/repository/
mv core/model/* model/ 2>/dev/null
mv core/utils/* utils/ 2>/dev/null
mv core/ui/* ui/components/ 2>/dev/null

# Remove empty dirs
rm -rf feature core

# Update package names and imports
find . -name "*.kt" -type f | while read file; do
    # Replace package names
    sed -i 's/package com.example.feature.auth/package com.example.auth/g' "$file"
    sed -i 's/package com.example.feature.chat/package com.example.chat/g' "$file"
    sed -i 's/package com.example.feature.settings/package com.example.settings/g' "$file"
    sed -i 's/package com.example.core.theme/package com.example.ui.theme/g' "$file"
    sed -i 's/package com.example.core.database/package com.example.data.local/g' "$file"
    sed -i 's/package com.example.feature.memory/package com.example.data.repository/g' "$file"
    sed -i 's/package com.example.core.network/package com.example.data.remote/g' "$file"
    sed -i 's/package com.example.core.di/package com.example.di/g' "$file"
    sed -i 's/package com.example.feature.diagnostic/package com.example.chat/g' "$file"
    sed -i 's/package com.example.feature.tools/package com.example.settings/g' "$file"
    sed -i 's/package com.example.core.model/package com.example.model/g' "$file"
    sed -i 's/package com.example.core.utils/package com.example.utils/g' "$file"
    sed -i 's/package com.example.core.ui/package com.example.ui.components/g' "$file"

    # Some files were moved to data/repository
    if [[ "$file" == *"data/repository/"* ]]; then
        sed -i 's/package com.example.chat/package com.example.data.repository/g' "$file"
        sed -i 's/package com.example.settings/package com.example.data.repository/g' "$file"
    fi

    # Replace imports
    sed -i 's/com.example.feature.auth/com.example.auth/g' "$file"
    sed -i 's/com.example.feature.chat.ChatRepository/com.example.data.repository.ChatRepository/g' "$file"
    sed -i 's/com.example.feature.settings.SettingsRepository/com.example.data.repository.SettingsRepository/g' "$file"
    sed -i 's/com.example.feature.memory.MemoryRepository/com.example.data.repository.MemoryRepository/g' "$file"
    sed -i 's/com.example.feature.chat/com.example.chat/g' "$file"
    sed -i 's/com.example.feature.settings/com.example.settings/g' "$file"
    sed -i 's/com.example.core.theme/com.example.ui.theme/g' "$file"
    sed -i 's/com.example.core.database/com.example.data.local/g' "$file"
    sed -i 's/com.example.core.network/com.example.data.remote/g' "$file"
    sed -i 's/com.example.core.di/com.example.di/g' "$file"
    sed -i 's/com.example.feature.diagnostic/com.example.chat/g' "$file"
    sed -i 's/com.example.feature.tools/com.example.settings/g' "$file"
    sed -i 's/com.example.core.model/com.example.model/g' "$file"
    sed -i 's/com.example.core.utils/com.example.utils/g' "$file"
    sed -i 's/com.example.core.ui/com.example.ui.components/g' "$file"
done

