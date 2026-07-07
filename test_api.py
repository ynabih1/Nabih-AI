import os
import requests

api_key = os.environ.get('GEMINI_API_KEY')
print("API KEY:", api_key)
url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key={api_key}"

headers = {"Content-Type": "application/json"}
data = {
    "contents": [{"parts": [{"text": "Hello, how are you?"}]}]
}

response = requests.post(url, headers=headers, json=data)
print(response.status_code)
print(response.text)
