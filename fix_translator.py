with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'r') as f:
    text = f.read()

english_old = """                else -> throwable.localizedMessage ?: throwable.message ?: "An unknown error occurred."
            }
        }"""
english_new = """                else -> {
                    val msg = throwable.localizedMessage ?: throwable.message ?: ""
                    if (msg.contains("VALIDATION_FAILED")) {
                        "Sorry, I couldn't understand your request. Please try rephrasing your question."
                    } else {
                        msg.ifBlank { "An unknown error occurred." }
                    }
                }
            }
        }"""

text = text.replace(english_old, english_new)

arabic_old = """                } else {
                    msg.ifBlank { "حدث خطأ غير معروف أثناء الاتصال بالمزود." }
                }"""
arabic_new = """                } else if (msg.contains("VALIDATION_FAILED")) {
                    "عذراً، لم أتمكن من فهم طلبك. يرجى إعادة صياغة السؤال."
                } else {
                    msg.ifBlank { "حدث خطأ غير معروف أثناء الاتصال بالمزود." }
                }"""

text = text.replace(arabic_old, arabic_new)

with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'w') as f:
    f.write(text)
