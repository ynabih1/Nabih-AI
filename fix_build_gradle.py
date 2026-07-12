import sys
content = open("app/build.gradle.kts").read()

content = content.replace('buildConfigField("String", "GEMINI_API_KEY", ""${geminiKey}"")', 'buildConfigField("String", "GEMINI_API_KEY", "\\"${geminiKey}\\"")')
content = content.replace('buildConfigField("String", "GOOGLE_CLIENT_ID", ""${googleClientId}"")', 'buildConfigField("String", "GOOGLE_CLIENT_ID", "\\"${googleClientId}\\"")')

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
