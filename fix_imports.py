import os

for root, _, files in os.walk('app/src/main/java/com/example'):
    for file in files:
        if file.endswith('.kt'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                lines = f.readlines()
            
            new_lines = []
            seen_imports = set()
            for line in lines:
                if line.startswith('import '):
                    if line not in seen_imports:
                        seen_imports.add(line)
                        new_lines.append(line)
                else:
                    new_lines.append(line)
            
            with open(filepath, 'w') as f:
                f.writelines(new_lines)
