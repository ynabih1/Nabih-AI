with open('app/src/main/java/com/example/chat/ChatViewModel.kt', 'r') as f:
    text = f.read()

old_cond = 'lastMsg.content.startsWith("An error occurred") || lastMsg.content.startsWith("API_ERROR:") || lastMsg.content.startsWith("حدث خطأ:") || lastMsg.content.startsWith("Error:")'
new_cond = 'lastMsg.content.startsWith("An error occurred") || lastMsg.content.startsWith("API_ERROR:") || lastMsg.content.startsWith("حدث خطأ:") || lastMsg.content.startsWith("Error:") || lastMsg.content.startsWith("Sorry,") || lastMsg.content.startsWith("عذراً")'

text = text.replace(old_cond, new_cond)

with open('app/src/main/java/com/example/chat/ChatViewModel.kt', 'w') as f:
    f.write(text)
