import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

fun main() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.anthropic.com/v1/messages")
        .header("x-api-key", "sk-ant-api03-invalidkey")
        .header("anthropic-version", "2023-06-01")
        .post(RequestBody.create(null, ByteArray(0)))
        .build()
    val response = client.newCall(request).execute()
    println(response.code)
}
