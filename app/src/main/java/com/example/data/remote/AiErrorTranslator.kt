package com.example.data.remote

import androidx.annotation.Keep
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@Keep
object AiErrorTranslator {
    fun mapApiErrorToUserMessage(code: Int, provider: String): String {
        val providerNameAr = when (provider.lowercase()) {
            "gemini", "google" -> "Gemini"
            "claude", "anthropic" -> "Claude"
            "chatgpt", "openai" -> "ChatGPT"
            "nabih", "nabih-ultra" -> "Nabih Ultra"
            else -> provider
        }
        return when (code) {
            400 -> "حدث خطأ في تنسيق الطلب، حاول مرة أخرى"
            401, 403 -> "مفتاح API غير صالح أو منتهي الصلاحية، تحقق من إعدادات المفاتيح"
            404 -> "الموديل المطلوب غير متاح حالياً، جرّب موديلاً آخر من القائمة"
            429 -> "تم تجاوز الحد المسموح من الطلبات لهذا المزود، حاول لاحقاً أو استخدم موديل آخر"
            500, 502, 503, 504 -> "خدمة [$providerNameAr] غير متاحة مؤقتاً، حاول مرة أخرى بعد قليل"
            -1 -> "استغرق الرد وقتاً أطول من المتوقع، تحقق من اتصالك وحاول مرة أخرى"
            else -> "حدث خطأ غير متوقع ($code) أثناء الاتصال بـ $providerNameAr، يرجى المحاولة لاحقاً."
        }
    }

    fun translate(throwable: Throwable, provider: String = "الذكاء الاصطناعي", isArabic: Boolean = false): String {
        if (!isArabic) {
            return when (throwable) {
                is SocketTimeoutException -> "Connection timed out. Please try again later."
                is UnknownHostException, is IOException -> "Network failure. Please check your internet connection and try again."
                is HttpException -> {
                    when (throwable.code()) {
                        400 -> "Bad request (400). Please check your input parameters or model configuration."
                        401, 403 -> "Unauthorized or invalid API Key (401/403). Please verify your key in Settings."
                        404 -> "Model not found or currently unavailable (404). Please select a different model from the list."
                        429 -> "Rate limit exceeded (429). Please wait a moment before trying again."
                        500, 502, 503, 504 -> "Service internal error (${throwable.code()}). Please try again later."
                        else -> "Unexpected provider error (${throwable.code()})."
                    }
                }
                else -> {
                    val msg = throwable.localizedMessage ?: throwable.message ?: ""
                    if (msg.contains("VALIDATION_FAILED")) {
                        "Sorry, I couldn't understand your request. Please try rephrasing your question."
                    } else {
                        msg.ifBlank { "An unknown error occurred." }
                    }
                }
            }
        }
        return when (throwable) {
            is SocketTimeoutException -> {
                mapApiErrorToUserMessage(-1, provider)
            }
            is UnknownHostException, is IOException -> {
                "فشل الاتصال بالإنترنت. يرجى التحقق من اتصالك والمحاولة مرة أخرى."
            }
            is HttpException -> {
                mapApiErrorToUserMessage(throwable.code(), provider)
            }
            else -> {
                val msg = throwable.localizedMessage ?: throwable.message ?: ""
                if (msg.contains("429")) {
                    mapApiErrorToUserMessage(429, provider)
                } else if (msg.contains("404")) {
                    mapApiErrorToUserMessage(404, provider)
                } else if (msg.contains("401") || msg.contains("403")) {
                    mapApiErrorToUserMessage(401, provider)
                } else if (msg.contains("400")) {
                    mapApiErrorToUserMessage(400, provider)
                } else if (msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504")) {
                    mapApiErrorToUserMessage(500, provider)
                } else if (msg.contains("timeout") || msg.contains("Timeout") || msg.contains("SocketTimeout")) {
                    mapApiErrorToUserMessage(-1, provider)
                } else if (msg.contains("VALIDATION_FAILED")) {
                    "عذراً، لم أتمكن من فهم طلبك. يرجى إعادة صياغة السؤال."
                } else {
                    msg.ifBlank { "حدث خطأ غير معروف أثناء الاتصال بالمزود." }
                }
            }
        }
    }
}
