package com.example

import org.junit.Assert.*
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCreateKvdbBucket() {
    val cookieJar = object : CookieJar {
      private val cookieStore = java.util.concurrent.ConcurrentHashMap<String, List<Cookie>>()
      override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies
      }
      override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
      }
    }

    val client = OkHttpClient.Builder()
      .cookieJar(cookieJar)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()
    try {
      // Step 1: Get domains from mail.tm
      val domainsUrl = "https://api.mail.tm/domains"
      val domainsRequest = Request.Builder().url(domainsUrl).get().build()
      var domain = ""
      client.newCall(domainsRequest).execute().use { response ->
        val respBody = response.body?.string() ?: ""
        println("Mail.tm domains: $respBody")
        if (respBody.contains("\"domain\"")) {
          domain = respBody.substringAfter("\"domain\":\"").substringBefore("\"")
        }
      }
      if (domain.isEmpty()) {
        println("Failed to fetch mail.tm domain")
        return
      }
      println("Domain fetched: $domain")

      // Step 2: Register a new account on mail.tm
      val randomUsername = "kvdbtester_" + (100000..999999).random()
      val address = "$randomUsername@$domain"
      val password = "MySecurePassword123!"
      val registerUrl = "https://api.mail.tm/accounts"
      val registerPayload = "{\"address\":\"$address\",\"password\":\"$password\"}"
      val registerRequest = Request.Builder()
        .url(registerUrl)
        .post(registerPayload.toRequestBody("application/json".toMediaType()))
        .build()
      
      client.newCall(registerRequest).execute().use { response ->
        println("Mail.tm Register account code: ${response.code}")
        println("Mail.tm Register account body: ${response.body?.string()}")
      }

      // Step 3: Authenticate to get JWT token
      val loginUrl = "https://api.mail.tm/token"
      val loginPayload = "{\"address\":\"$address\",\"password\":\"$password\"}"
      val loginRequest = Request.Builder()
        .url(loginUrl)
        .post(loginPayload.toRequestBody("application/json".toMediaType()))
        .build()
      
      var jwtToken = ""
      client.newCall(loginRequest).execute().use { response ->
        val resp = response.body?.string() ?: ""
        println("Mail.tm Login body: $resp")
        if (resp.contains("\"token\"")) {
          jwtToken = resp.substringAfter("\"token\":\"").substringBefore("\"")
        }
      }

      if (jwtToken.isEmpty()) {
        println("Failed to authenticate with mail.tm")
        return
      }
      println("Mail.tm JWT token obtained: $jwtToken")

      // Step 4: Create a KVDB bucket with our new verified email
      val kvdbUrl = "https://kvdb.io"
      val formBody = okhttp3.FormBody.Builder()
        .add("email", address)
        .build()
      val kvdbRequest = Request.Builder()
        .url(kvdbUrl)
        .post(formBody)
        .build()
      
      var createdBucketId = ""
      client.newCall(kvdbRequest).execute().use { response ->
        val body = response.body?.string() ?: ""
        println("KVDB Create Code: ${response.code}")
        println("KVDB Create Body: $body")
        if (response.code == 201) {
          createdBucketId = body.trim()
        }
      }

      if (createdBucketId.isEmpty()) {
        println("Failed to create KVDB bucket")
        return
      }
      println("Created KVDB Bucket ID: $createdBucketId")

      // Step 5: Poll inbox for verification mail from KVDB
      println("Polling mail.tm inbox for verification mail...")
      var verificationUrl = ""
      for (retry in 1..30) {
        Thread.sleep(3000)
        val messagesUrl = "https://api.mail.tm/messages"
        val messagesRequest = Request.Builder()
          .url(messagesUrl)
          .header("Authorization", "Bearer $jwtToken")
          .get()
          .build()
        
        var messagesBody = ""
        client.newCall(messagesRequest).execute().use { response ->
          messagesBody = response.body?.string() ?: ""
        }
        println("Poll #$retry inbox body: $messagesBody")
        
        if (messagesBody.contains("\"id\"")) {
          val msgId = messagesBody.substringAfter("\"id\":\"").substringBefore("\"")
          println("Received email message ID: $msgId")

          // Read the detailed message content to access HTML of activation url
          val readUrl = "https://api.mail.tm/messages/$msgId"
          val readRequest = Request.Builder()
            .url(readUrl)
            .header("Authorization", "Bearer $jwtToken")
            .get()
            .build()
          
          client.newCall(readRequest).execute().use { response ->
            var msgBody = response.body?.string() ?: ""
            println("=== EMAIL CONTENT DEBUG ===")
            println(msgBody)
            println("=== END EMAIL CONTENT DEBUG ===")
            
            // Unescape JSON slashes to match standard URL
            msgBody = msgBody.replace("\\/", "/")
            
            if (msgBody.contains("https://kvdb.io/login?token=")) {
              val rawSuffix = msgBody.substringAfter("https://kvdb.io/login?token=")
              val cleanToken = rawSuffix.takeWhile { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }
              verificationUrl = "https://kvdb.io/login?token=$cleanToken"
            }
          }
          break
        }
      }

      if (verificationUrl.isEmpty()) {
        println("Verification email did not arrive in 30 seconds.")
        return
      }
      println("Extracted Verification URL: $verificationUrl")

      // Step 6: Trigger bucket activation (GET)
      val verifyRequest = Request.Builder().url(verificationUrl).get().build()
      client.newCall(verifyRequest).execute().use { response ->
        println("Verification Result Code: ${response.code}")
        val verifyBody = response.body?.string() ?: ""
        println("Verification Response: $verifyBody")
      }

      // Step 7: Perform write and read operation on the newly activated bucket!
      val testKeyUrl = "https://kvdb.io/$createdBucketId/test_key_handshake"
      val putRequest = Request.Builder()
        .url(testKeyUrl)
        .put("verified_kvdb_bucket_payload".toRequestBody("text/plain".toMediaType()))
        .build()
      
      client.newCall(putRequest).execute().use { response ->
        println("Verified Bucket PUT Code: ${response.code}")
        println("Verified Bucket PUT Body: ${response.body?.string()}")
      }

      val getRequest = Request.Builder()
        .url(testKeyUrl)
        .get()
        .build()

      client.newCall(getRequest).execute().use { response ->
        println("Verified Bucket GET Code: ${response.code}")
        println("Verified Bucket GET Body: ${response.body?.string()}")
      }

    } catch (e: Exception) {
      println("KVDB Auto Activation Exception: ${e.message}")
    }
  }

  @Test
  fun testKeyValueSystems() {
    val client = OkHttpClient()
    try {
      val url = "https://keyvalue.systems/cfc353b8f6ce4b6492ebf58404f911a4" // unique key
      val putRequest = Request.Builder()
        .url(url)
        .put("hello_keyvalue_systems".toRequestBody("text/plain".toMediaType()))
        .build()
      client.newCall(putRequest).execute().use { response ->
        println("KeyValueSystems PUT Code: ${response.code}")
        println("KeyValueSystems PUT Body: ${response.body?.string()}")
      }

      val getRequest = Request.Builder()
        .url(url)
        .get()
        .build()
      client.newCall(getRequest).execute().use { response ->
        println("KeyValueSystems GET Code: ${response.code}")
        println("KeyValueSystems GET Body: ${response.body?.string()}")
      }
    } catch (e: Exception) {
      println("KeyValueSystems Exception: ${e.message}")
    }
  }
}












