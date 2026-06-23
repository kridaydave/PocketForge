package dev.pocketforge.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class GitHubTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences(GITHUB_TOKEN_PREFS, Context.MODE_PRIVATE)

    fun hasToken(): Boolean {
        return prefs.contains(PREF_GITHUB_TOKEN_CIPHER_TEXT) && prefs.contains(PREF_GITHUB_TOKEN_IV)
    }

    fun saveToken(token: String) {
        val cleanToken = token.trim()
        require(cleanToken.isNotBlank()) { "Enter a GitHub token before saving." }

        val encryptedToken = encrypt(cleanToken.encodeToByteArray())
        prefs.edit()
            .putString(PREF_GITHUB_TOKEN_CIPHER_TEXT, Base64.encodeToString(encryptedToken.cipherText, Base64.NO_WRAP))
            .putString(PREF_GITHUB_TOKEN_IV, Base64.encodeToString(encryptedToken.iv, Base64.NO_WRAP))
            .apply()
    }

    fun loadToken(): String? {
        val cipherText = prefs.getString(PREF_GITHUB_TOKEN_CIPHER_TEXT, null) ?: return null
        val iv = prefs.getString(PREF_GITHUB_TOKEN_IV, null) ?: return null

        return try {
            decrypt(
                cipherText = Base64.decode(cipherText, Base64.NO_WRAP),
                iv = Base64.decode(iv, Base64.NO_WRAP),
            ).decodeToString()
        } catch (ignored: Exception) {
            null
        }
    }

    fun clearToken() {
        prefs.edit()
            .remove(PREF_GITHUB_TOKEN_CIPHER_TEXT)
            .remove(PREF_GITHUB_TOKEN_IV)
            .apply()
    }

    private fun encrypt(plainText: ByteArray): EncryptedValue {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return EncryptedValue(
            cipherText = cipher.doFinal(plainText),
            iv = cipher.iv,
        )
    }

    private fun decrypt(cipherText: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(cipherText)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getEntry(GITHUB_TOKEN_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) return existingKey.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            GITHUB_TOKEN_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE_BITS)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}

internal class GitHubReadOnlyClient {
    fun listRepos(token: String): List<GitHubRepo> {
        val reposJson = getJsonArray(
            token = token,
            endpoint = "$GITHUB_API_BASE/user/repos?type=all&sort=updated&per_page=100",
        )

        return buildList {
            for (index in 0 until reposJson.length()) {
                val repo = reposJson.optJSONObject(index) ?: continue
                val owner = repo.optJSONObject("owner")?.optString("login").orEmpty()
                val name = repo.optString("name")
                if (owner.isBlank() || name.isBlank()) continue

                add(
                    GitHubRepo(
                        id = repo.optLong("id"),
                        owner = owner,
                        name = name,
                        fullName = repo.optString("full_name", "$owner/$name"),
                        description = repo.optString("description").orEmpty(),
                        defaultBranch = repo.optString("default_branch", "main"),
                        privateRepo = repo.optBoolean("private", false),
                        updatedAt = repo.optString("updated_at").orEmpty(),
                    ),
                )
            }
        }
    }

    fun listContents(token: String, repo: GitHubRepo, path: String): List<GitHubContentItem> {
        val cleanPath = path.trim('/')
        val encodedPath = cleanPath
            .takeIf { it.isNotBlank() }
            ?.split("/")
            ?.joinToString("/") { segment -> encodePathSegment(segment) }
            .orEmpty()
        val suffix = if (encodedPath.isBlank()) "" else "/$encodedPath"
        val contentsJson = getJson(
            token = token,
            endpoint = "$GITHUB_API_BASE/repos/${repo.owner}/${repo.name}/contents$suffix?ref=${encodePathSegment(repo.defaultBranch)}",
        )

        val contents = when (contentsJson) {
            is JSONArray -> contentsJson
            is JSONObject -> JSONArray().put(contentsJson)
            else -> JSONArray()
        }

        return buildList {
            for (index in 0 until contents.length()) {
                val item = contents.optJSONObject(index) ?: continue
                val type = item.optString("type")
                if (type != GITHUB_CONTENT_FILE && type != GITHUB_CONTENT_DIR) continue

                add(
                    GitHubContentItem(
                        name = item.optString("name"),
                        path = item.optString("path"),
                        type = type,
                        size = item.optLong("size", 0L),
                        downloadUrl = item.optString("download_url").ifBlank { null },
                    ),
                )
            }
        }.sortedWith(compareBy<GitHubContentItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

    fun loadFile(token: String, repo: GitHubRepo, item: GitHubContentItem): GitHubFilePreview {
        if (!item.isFile) {
            return GitHubFilePreview(
                name = item.name,
                path = item.path,
                sizeBytes = item.size,
                content = "",
                mode = "Folder",
                tooLarge = false,
                binary = false,
            )
        }

        if (item.size > MAX_FILE_PREVIEW_BYTES) {
            return GitHubFilePreview(
                name = item.name,
                path = item.path,
                sizeBytes = item.size,
                content = "",
                mode = "Too large",
                tooLarge = true,
                binary = false,
            )
        }

        val encodedPath = item.path
            .split("/")
            .joinToString("/") { segment -> encodePathSegment(segment) }
        val fileJson = getJsonObject(
            token = token,
            endpoint = "$GITHUB_API_BASE/repos/${repo.owner}/${repo.name}/contents/$encodedPath?ref=${encodePathSegment(repo.defaultBranch)}",
        )
        val encoding = fileJson.optString("encoding")
        if (encoding != "base64") {
            return GitHubFilePreview(
                name = item.name,
                path = item.path,
                sizeBytes = item.size,
                content = "",
                mode = "Not previewable",
                tooLarge = false,
                binary = true,
            )
        }

        val bytes = Base64.decode(fileJson.optString("content"), Base64.DEFAULT)
        val decodedText = decodeUtf8(bytes)
        return if (decodedText == null || bytes.any { it == 0.toByte() }) {
            GitHubFilePreview(
                name = item.name,
                path = item.path,
                sizeBytes = item.size,
                content = "",
                mode = "Binary",
                tooLarge = false,
                binary = true,
            )
        } else {
            GitHubFilePreview(
                name = item.name,
                path = item.path,
                sizeBytes = item.size,
                content = decodedText,
                mode = inferPreviewMode(item.name),
                tooLarge = false,
                binary = false,
            )
        }
    }

    private fun getJsonArray(token: String, endpoint: String): JSONArray {
        return getJson(token = token, endpoint = endpoint) as? JSONArray
            ?: throw GitHubApiException("GitHub returned an unexpected response.")
    }

    private fun getJsonObject(token: String, endpoint: String): JSONObject {
        return getJson(token = token, endpoint = endpoint) as? JSONObject
            ?: throw GitHubApiException("GitHub returned an unexpected response.")
    }

    private fun getJson(token: String, endpoint: String): Any {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = HTTP_TIMEOUT_MS
            readTimeout = HTTP_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("User-Agent", "PocketForge-Android-ReadOnly")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }

        return try {
            val statusCode = connection.responseCode
            val body = readBody(
                if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: ByteArrayInputStream(ByteArray(0))
                },
            )

            if (statusCode !in 200..299) {
                throw GitHubApiException(githubErrorMessage(statusCode, body))
            }

            val trimmedBody = body.trim()
            when {
                trimmedBody.startsWith("[") -> JSONArray(trimmedBody)
                trimmedBody.startsWith("{") -> JSONObject(trimmedBody)
                else -> throw GitHubApiException("GitHub returned an empty or unreadable response.")
            }
        } finally {
            connection.disconnect()
        }
    }
}

internal data class GitHubReadOnlyActions(
    val saveToken: (String, (GitHubUiResult<Unit>) -> Unit) -> Unit,
    val clearToken: ((GitHubUiResult<Unit>) -> Unit) -> Unit,
    val loadRepos: ((GitHubUiResult<List<GitHubRepo>>) -> Unit) -> Unit,
    val loadContents: (GitHubRepo, String, (GitHubUiResult<List<GitHubContentItem>>) -> Unit) -> Unit,
    val loadFile: (GitHubRepo, GitHubContentItem, (GitHubUiResult<GitHubFilePreview>) -> Unit) -> Unit,
)

internal sealed class GitHubUiResult<out T> {
    data class Success<T>(val value: T) : GitHubUiResult<T>()
    data class Error(val message: String) : GitHubUiResult<Nothing>()
}

internal data class GitHubRepo(
    val id: Long,
    val owner: String,
    val name: String,
    val fullName: String,
    val description: String,
    val defaultBranch: String,
    val privateRepo: Boolean,
    val updatedAt: String,
)

internal data class GitHubContentItem(
    val name: String,
    val path: String,
    val type: String,
    val size: Long,
    val downloadUrl: String?,
) {
    val isDirectory: Boolean = type == GITHUB_CONTENT_DIR
    val isFile: Boolean = type == GITHUB_CONTENT_FILE
}

internal data class GitHubFilePreview(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val content: String,
    val mode: String,
    val tooLarge: Boolean,
    val binary: Boolean,
)

internal val NoopGitHubReadOnlyActions = GitHubReadOnlyActions(
    saveToken = { _, callback -> callback(GitHubUiResult.Error("GitHub is not available in preview.")) },
    clearToken = { callback -> callback(GitHubUiResult.Error("GitHub is not available in preview.")) },
    loadRepos = { callback -> callback(GitHubUiResult.Error("GitHub is not available in preview.")) },
    loadContents = { _, _, callback -> callback(GitHubUiResult.Error("GitHub is not available in preview.")) },
    loadFile = { _, _, callback -> callback(GitHubUiResult.Error("GitHub is not available in preview.")) },
)

private data class EncryptedValue(
    val cipherText: ByteArray,
    val iv: ByteArray,
)

private class GitHubApiException(message: String) : Exception(message)

private fun readBody(inputStream: java.io.InputStream): String {
    inputStream.use { input ->
        val output = ByteArrayOutputStream()
        input.copyTo(output)
        return output.toString(Charsets.UTF_8.name())
    }
}

private fun githubErrorMessage(statusCode: Int, body: String): String {
    val apiMessage = runCatching {
        JSONObject(body).optString("message")
    }.getOrNull().orEmpty()

    return when (statusCode) {
        HttpURLConnection.HTTP_UNAUTHORIZED -> "GitHub rejected the token. Check that it is valid and not expired."
        HttpURLConnection.HTTP_FORBIDDEN -> "GitHub denied the read request. Check repo permissions or API rate limits."
        HttpURLConnection.HTTP_NOT_FOUND -> "GitHub could not find that repo or path with this token."
        else -> "GitHub request failed ($statusCode)${if (apiMessage.isBlank()) "." else ": $apiMessage"}"
    }
}

private fun decodeUtf8(bytes: ByteArray): String? {
    return try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (ignored: CharacterCodingException) {
        null
    }
}

private fun inferPreviewMode(fileName: String): String {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return when (extension) {
        "md", "markdown" -> "Markdown"
        "kt", "kts", "java", "xml", "gradle", "yml", "yaml", "json", "toml", "txt", "sh" -> "Code"
        else -> "Text"
    }
}

private fun encodePathSegment(segment: String): String {
    return URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
}

private const val GITHUB_TOKEN_PREFS = "github_token_store"
private const val PREF_GITHUB_TOKEN_CIPHER_TEXT = "github_token_cipher_text"
private const val PREF_GITHUB_TOKEN_IV = "github_token_iv"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val GITHUB_TOKEN_KEY_ALIAS = "pocketforge_github_read_only_token"
private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
private const val AES_KEY_SIZE_BITS = 256
private const val GCM_TAG_LENGTH_BITS = 128
private const val GITHUB_API_BASE = "https://api.github.com"
private const val GITHUB_CONTENT_FILE = "file"
private const val GITHUB_CONTENT_DIR = "dir"
private const val HTTP_TIMEOUT_MS = 15_000
private const val MAX_FILE_PREVIEW_BYTES = 240_000L
