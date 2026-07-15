import re

with open('app/src/main/java/com/example/core/utils/DocumentParser.kt', 'r') as f:
    content = f.read()

# Replace all .toByte() on Chars with .code.toByte()
# 's'.toByte() -> 's'.code.toByte()
content = re.sub(r"'([^'])'\.toByte\(\)", r"'\1'.code.toByte()", content)
content = re.sub(r"'\\[rn]'\.toByte\(\)", lambda m: m.group(0).replace('.toByte()', '.code.toByte()'), content)

with open('app/src/main/java/com/example/core/utils/DocumentParser.kt', 'w') as f:
    f.write(content)
print("DocumentParser patched")
