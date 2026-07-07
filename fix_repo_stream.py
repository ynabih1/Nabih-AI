import re

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'r') as f:
    code = f.read()

# I will rewrite the streamChatResponse logic
