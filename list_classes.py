import os
import re

dir_path = 'app/src/main/java/com/example'
classes = {}

for root, _, files in os.walk(dir_path):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
                
            pkg_match = re.search(r'^package\s+([a-zA-Z0-9_.]+)', content, re.MULTILINE)
            pkg = pkg_match.group(1) if pkg_match else ''
            
            # Find class, interface, object, enum class, fun (if top level like @Composable)
            decls = re.findall(r'^(?:suspend\s+)?(?:data\s+)?(?:sealed\s+)?(?:class|interface|object|enum class)\s+([A-Z][a-zA-Z0-9_]+)', content, re.MULTILINE)
            funcs = re.findall(r'^@Composable\s*\n(?:@.*\n)*fun\s+([A-Z][a-zA-Z0-9_]+)', content, re.MULTILINE)
            
            classes[path] = {
                'package': pkg,
                'declarations': list(set(decls + funcs))
            }

for p, info in classes.items():
    print(f"{p}: {info['package']} -> {info['declarations']}")
