import sys
content = open("app/build.gradle.kts").read()
default_config_target = """    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }"""
default_config_replacement = """    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    var geminiKey = System.getenv("GEMINI_API_KEY")
    var googleClientId = System.getenv("GOOGLE_CLIENT_ID")
    if (geminiKey.isNullOrEmpty() || googleClientId.isNullOrEmpty()) {
        val envFile = rootProject.file(".env")
        val envExampleFile = rootProject.file(".env.example")
        val props = java.util.Properties()
        if (envFile.exists()) {
            props.load(java.io.FileInputStream(envFile))
        } else if (envExampleFile.exists()) {
            props.load(java.io.FileInputStream(envExampleFile))
        }
        if (geminiKey.isNullOrEmpty()) geminiKey = props.getProperty("GEMINI_API_KEY") ?: ""
        if (googleClientId.isNullOrEmpty()) googleClientId = props.getProperty("GOOGLE_CLIENT_ID") ?: ""
    }
    buildConfigField("String", "GEMINI_API_KEY", "\"${geminiKey}\"")
    buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${googleClientId}\"")
  }"""

secrets_target = """secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}"""
secrets_replacement = """secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("GEMINI_API_KEY")
  ignoreList.add("GOOGLE_CLIENT_ID")
}"""

content = content.replace(default_config_target, default_config_replacement)
content = content.replace(secrets_target, secrets_replacement)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
