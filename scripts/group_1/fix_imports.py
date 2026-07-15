import sys
content = open("app/build.gradle.kts").read()

content = content.replace("val props = java.util.Properties()", "val props = java.util.Properties()")
# Wait, let's just add imports at the top.
# And remove "java.util." and "java.io."

content = content.replace("java.util.Properties", "Properties")
content = content.replace("java.io.FileInputStream", "FileInputStream")

imports = """import java.util.Properties
import java.io.FileInputStream
"""

if "import java.util.Properties" not in content:
    content = imports + content

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
