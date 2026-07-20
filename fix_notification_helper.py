with open('app/src/main/java/com/example/util/NotificationHelper.kt', 'r') as f:
    text = f.read()

text = text.replace('NotificationManager.IMPORTANCE_DEFAULT', 'NotificationManager.IMPORTANCE_HIGH')
text = text.replace('.setSmallIcon(R.mipmap.ic_launcher)', '.setSmallIcon(android.R.drawable.ic_dialog_info)')
text = text.replace('.setPriority(NotificationCompat.PRIORITY_DEFAULT)', '.setPriority(NotificationCompat.PRIORITY_HIGH)')

with open('app/src/main/java/com/example/util/NotificationHelper.kt', 'w') as f:
    f.write(text)
