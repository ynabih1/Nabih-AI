with open('app/src/main/java/com/example/chat/ChatViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace('private val networkMonitor: NetworkMonitor', 'private val networkMonitor: NetworkMonitor,\n    private val notificationHelper: com.example.util.NotificationHelper')

with open('app/src/main/java/com/example/chat/ChatViewModel.kt', 'w') as f:
    f.write(text)
