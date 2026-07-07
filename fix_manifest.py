with open('app/src/main/AndroidManifest.xml', 'r') as f:
    code = f.read()

old_code = """            </intent-filter>
        </activity>
    </application>"""

new_code = """            </intent-filter>
        </activity>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>"""

code = code.replace(old_code, new_code)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(code)

