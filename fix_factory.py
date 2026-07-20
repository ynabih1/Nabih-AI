with open('app/src/main/java/com/example/di/ViewModelFactory.kt', 'r') as f:
    text = f.read()

text = text.replace('networkMonitor = container.networkMonitor', 'networkMonitor = container.networkMonitor,\n                    notificationHelper = container.notificationHelper')

with open('app/src/main/java/com/example/di/ViewModelFactory.kt', 'w') as f:
    f.write(text)
