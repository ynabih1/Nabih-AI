with open('app/src/main/java/com/example/di/AppContainer.kt', 'r') as f:
    text = f.read()

text = text.replace('val chatRepository: ChatRepository', 'val chatRepository: ChatRepository\n    val notificationHelper: com.example.util.NotificationHelper')
text = text.replace('override val chatRepository: ChatRepository by lazy {', 'override val notificationHelper: com.example.util.NotificationHelper by lazy {\n        com.example.util.NotificationHelper(context)\n    }\n\n    override val chatRepository: ChatRepository by lazy {')

with open('app/src/main/java/com/example/di/AppContainer.kt', 'w') as f:
    f.write(text)
