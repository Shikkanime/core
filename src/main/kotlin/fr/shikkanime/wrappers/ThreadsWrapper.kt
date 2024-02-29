package fr.shikkanime.wrappers

import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.time.ZonedDateTime
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

object ThreadsWrapper {
    private const val BASE_URL = "https://i.instagram.com"
    private const val LATEST_APP_VERSION = "291.0.0.31.111"
    private const val EXPERIMENTS =
        "ig_android_fci_onboarding_friend_search,ig_android_device_detection_info_upload,ig_android_account_linking_upsell_universe,ig_android_direct_main_tab_universe_v2,ig_android_allow_account_switch_once_media_upload_finish_universe,ig_android_sign_in_help_only_one_account_family_universe,ig_android_sms_retriever_backtest_universe,ig_android_direct_add_direct_to_android_native_photo_share_sheet,ig_android_spatial_account_switch_universe,ig_growth_android_profile_pic_prefill_with_fb_pic_2,ig_account_identity_logged_out_signals_global_holdout_universe,ig_android_prefill_main_account_username_on_login_screen_universe,ig_android_login_identifier_fuzzy_match,ig_android_mas_remove_close_friends_entrypoint,ig_android_shared_email_reg_universe,ig_android_video_render_codec_low_memory_gc,ig_android_custom_transitions_universe,ig_android_push_fcm,multiple_account_recovery_universe,ig_android_show_login_info_reminder_universe,ig_android_email_fuzzy_matching_universe,ig_android_one_tap_aymh_redesign_universe,ig_android_direct_send_like_from_notification,ig_android_suma_landing_page,ig_android_prefetch_debug_dialog,ig_android_smartlock_hints_universe,ig_android_black_out,ig_activation_global_discretionary_sms_holdout,ig_android_video_ffmpegutil_pts_fix,ig_android_multi_tap_login_new,ig_save_smartlock_universe,ig_android_caption_typeahead_fix_on_o_universe,ig_android_enable_keyboardlistener_redesign,ig_android_sign_in_password_visibility_universe,ig_android_nux_add_email_device,ig_android_direct_remove_view_mode_stickiness_universe,ig_android_hide_contacts_list_in_nux,ig_android_new_users_one_tap_holdout_universe,ig_android_ingestion_video_support_hevc_decoding,ig_android_mas_notification_badging_universe,ig_android_secondary_account_in_main_reg_flow_universe,ig_android_secondary_account_creation_universe,ig_android_account_recovery_auto_login,ig_android_pwd_encrytpion,ig_android_bottom_sheet_keyboard_leaks,ig_android_sim_info_upload,ig_android_mobile_http_flow_device_universe,ig_android_hide_fb_button_when_not_installed_universe,ig_android_account_linking_on_concurrent_user_session_infra_universe,ig_android_targeted_one_tap_upsell_universe,ig_android_gmail_oauth_in_reg,ig_android_account_linking_flow_shorten_universe,ig_android_vc_interop_use_test_igid_universe,ig_android_notification_unpack_universe,ig_android_registration_confirmation_code_universe,ig_android_device_based_country_verification,ig_android_log_suggested_users_cache_on_error,ig_android_reg_modularization_universe,ig_android_device_verification_separate_endpoint,ig_android_universe_noticiation_channels,ig_android_account_linking_universe,ig_android_hsite_prefill_new_carrier,ig_android_one_login_toast_universe,ig_android_retry_create_account_universe,ig_android_family_apps_user_values_provider_universe,ig_android_reg_nux_headers_cleanup_universe,ig_android_mas_ui_polish_universe,ig_android_device_info_foreground_reporting,ig_android_shortcuts_2019,ig_android_device_verification_fb_signup,ig_android_onetaplogin_optimization,ig_android_passwordless_account_password_creation_universe,ig_android_black_out_toggle_universe,ig_video_debug_overlay,ig_android_ask_for_permissions_on_reg,ig_assisted_login_universe,ig_android_security_intent_switchoff,ig_android_device_info_job_based_reporting,ig_android_add_account_button_in_profile_mas_universe,ig_android_add_dialog_when_delinking_from_child_account_universe,ig_android_passwordless_auth,ig_radio_button_universe_2,ig_android_direct_main_tab_account_switch,ig_android_recovery_one_tap_holdout_universe,ig_android_modularized_dynamic_nux_universe,ig_android_fb_account_linking_sampling_freq_universe,ig_android_fix_sms_read_lollipop,ig_android_access_flow_prefil"
    private const val BLOKS_VERSION = "5f56efad68e1edec7801f630b5c122704ec5378adbee6609a448f105f34a9c73"
    private const val CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8"
    private const val USER_AGENT = "Barcelona $LATEST_APP_VERSION Android"
    private val httpRequest = HttpRequest()
    private val secureRandom = SecureRandom()

    suspend fun qeSync(): HttpResponse {
        val uuid = UUID.randomUUID().toString()

        return httpRequest.post(
            "$BASE_URL/api/v1/qe/sync/",
            headers = mapOf(
                HttpHeaders.UserAgent to USER_AGENT,
                HttpHeaders.ContentType to CONTENT_TYPE,
                "Sec-Fetch-Site" to "same-origin",
                "X-DEVICE-ID" to uuid,
            ),
            body = FormDataContent(Parameters.build {
                append("id", uuid)
                append("experiments", EXPERIMENTS)
            })
        )
    }

    suspend fun encryptPassword(password: String): Map<String, String> {
        // https://github.com/instagram4j/instagram4j/blob/39635974c391e21a322ab3294275df99d7f75f84/src/main/java/com/github/instagram4j/instagram4j/utils/IGUtils.java#L176
        val randKey = ByteArray(32).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val headers = qeSync().headers
        val time = (System.currentTimeMillis() / 1000).toString()

        val passwordEncryptionKeyID = headers["ig-set-password-encryption-key-id"]!!.toInt()
        val passwordEncryptionPubKey = headers["ig-set-password-encryption-pub-key"]!!

        // Encrypt random key
        val decodedPubKey = String(
            Base64.getDecoder().decode(passwordEncryptionPubKey),
            StandardCharsets.UTF_8
        ).replace("-(.*)-|\n".toRegex(), "")
        val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING")
        rsaCipher.init(
            Cipher.ENCRYPT_MODE,
            KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(decodedPubKey)))
        )
        val randKeyEncrypted = rsaCipher.doFinal(randKey)

        // Encrypt password
        val aesGcmCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesGcmCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(randKey, "AES"), GCMParameterSpec(128, iv))
        aesGcmCipher.updateAAD(time.toByteArray())
        val passwordEncrypted = aesGcmCipher.doFinal(password.toByteArray())

        // Write to final byte array
        val out = ByteArrayOutputStream()
        out.write(1)
        out.write(Integer.valueOf(passwordEncryptionKeyID))

        withContext(Dispatchers.IO) {
            out.write(iv)
            out.write(
                ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putChar(randKeyEncrypted.size.toChar()).array()
            )
            out.write(randKeyEncrypted)
            out.write(Arrays.copyOfRange(passwordEncrypted, passwordEncrypted.size - 16, passwordEncrypted.size))
            out.write(Arrays.copyOfRange(passwordEncrypted, 0, passwordEncrypted.size - 16))
        }

        return mapOf(
            "time" to time,
            "password" to Base64.getEncoder().encodeToString(out.toByteArray())
        )
    }

    fun generateDeviceId(username: String, password: String): String {
        val seed: String = EncryptionManager.toMD5(username + password)
        val volatileSeed = "12345"
        return "android-" + EncryptionManager.toMD5(seed + volatileSeed).substring(0, 16)
    }

    suspend fun login(deviceId: String, username: String, password: String): Pair<String, String> {
        val encryptedPassword = encryptPassword(password)

        val params = URLEncoder.encode(
            ObjectParser.toJson(
                mapOf(
                    "client_input_params" to mapOf(
                        "password" to "#PWD_INSTAGRAM:4:${encryptedPassword["time"]}:${encryptedPassword["password"]}",
                        "contact_point" to username,
                        "device_id" to deviceId,
                    ),
                    "server_params" to mapOf(
                        "credential_type" to "password",
                        "device_id" to deviceId,
                    )
                )
            ), StandardCharsets.UTF_8
        )

        val bkClientContext = URLEncoder.encode(
            ObjectParser.toJson(
                mapOf(
                    "bloks_version" to BLOKS_VERSION,
                    "styles_id" to "instagram",
                )
            ), StandardCharsets.UTF_8
        )

        val response = httpRequest.post(
            "$BASE_URL/api/v1/bloks/apps/com.bloks.www.bloks.caa.login.async.send_login_request/",
            headers = mapOf(
                HttpHeaders.UserAgent to USER_AGENT,
                HttpHeaders.ContentType to CONTENT_TYPE,
                "Response-Type" to "json",
            ),
            body = "params=$params&bk_client_context=$bkClientContext&bloks_versioning_id=$BLOKS_VERSION"
        )

        require(response.status.value == 200) { "Failed to login: ${response.status}" }

        val responseJson = ObjectParser.fromJson(response.bodyAsText())
        val rawBloks = responseJson.getAsJsonObject("layout").getAsJsonObject("bloks_payload").getAsJsonObject("tree").getAsJsonObject("㐟").getAsString("#")
        val substring = rawBloks!!.substring(rawBloks.indexOfFirst { it == '{' }, rawBloks.indexOfLast { it == '}' } + 1)

        val sToken = substring.split("Bearer IGT:2:")[1]
        val token = sToken.substring(0, sToken.indexOf("\\\\\\\""))

        val sUserID = substring.split("pk_id")[1].replace("\\\\\\\":\\\\\\\"", "\":\"")
        val userID = sUserID.substring(3, sUserID.indexOf("\\\\\\\""))

        return token to userID
    }

    private suspend fun uploadImage(
        username: String,
        token: String,
        mimeType: ContentType,
        uploadId: String,
        content: ByteArray,
    ): HttpResponse {
        val name = "${uploadId}_0_${abs(secureRandom.nextInt())}"

        val map = mapOf(
            "upload_id" to uploadId,
            "media_type" to "1",
            "sticker_burnin_params" to "[]",
            "image_compression" to ObjectParser.toJson(
                mapOf(
                    "lib_name" to "moz",
                    "lib_version" to "3.1.m",
                    "quality" to "80"
                )
            ),
            "xsharing_user_ids" to "[]",
            "retry_context" to ObjectParser.toJson(
                mapOf(
                    "num_step_auto_retry" to 0,
                    "num_reupload" to 0,
                    "num_step_manual_retry" to 0
                )
            ),
            "IG-FB-Xpost-entry-point-v2" to "feed",
        )

        val imageHeaders = mapOf(
            HttpHeaders.UserAgent to USER_AGENT,
            HttpHeaders.ContentType to "application/octet-stream",
            HttpHeaders.Authorization to "Bearer IGT:2:$token",
            "Authority" to "www.threads.net",
            HttpHeaders.Accept to "*/*",
            HttpHeaders.AcceptLanguage to "en-US",
            HttpHeaders.CacheControl to "no-cache",
            HttpHeaders.Origin to "https://www.threads.net",
            HttpHeaders.Pragma to "no-cache",
            "Sec-Fetch-Site" to "same-origin",
            "x-asbd-id" to "129477",
            "x-fb-lsd" to "NjppQDEgONsU_1LCzrmp6q",
            "x-ig-app-id" to "238260118697367",
            HttpHeaders.Referrer to "https://www.threads.net/@$username",
            "X_FB_PHOTO_WATERFALL_ID" to UUID.randomUUID().toString(),
            "X-Entity-Type" to mimeType.toString(),
            "Offset" to "0",
            "X-Instagram-Rupload-Params" to ObjectParser.toJson(map),
            "X-Entity-Name" to name,
            "X-Entity-Length" to content.size.toString(),
            HttpHeaders.ContentLength to content.size.toString(),
            HttpHeaders.AcceptEncoding to "gzip"
        )

        return httpRequest.post(
            "https://www.instagram.com/rupload_igphoto/$name",
            headers = imageHeaders,
            body = content,
        )
    }

    suspend fun publish(
        username: String,
        deviceId: String,
        userId: String,
        token: String,
        text: String,
        image: ByteArray? = null,
    ): HttpResponse {
        val uploadId = System.currentTimeMillis().toString()

        if (image != null) {
            val uploadImage = uploadImage(username, token, ContentType.Image.PNG, uploadId, image)
            require(uploadImage.status.value == 200) { "Failed to upload image: ${uploadImage.status}" }
        }

        val map = mutableMapOf<String, Any?>(
            "upload_id" to uploadId,
            "source_type" to "4",
            "timezone_offset" to ZonedDateTime.now().offset.totalSeconds.toString(),
            "device" to mapOf(
                "manufacturer" to "OnePlus",
                "model" to "ONEPLUS+A3010",
                "os_version" to 25,
                "os_release" to "7.1.1",
            ),
            "text_post_app_info" to mapOf(
                "reply_control" to 0
            ),
            "_uid" to userId,
            "device_id" to deviceId,
            "caption" to text,
        )

        if (image != null) {
            map["scene_type"] = null
            map["scene_capture_type"] = ""
        } else map["publish_mode"] = "text_post"

        return httpRequest.post(
            if (image != null) "$BASE_URL/api/v1/media/configure_text_post_app_feed/" else "$BASE_URL/api/v1/media/configure_text_only_post/",
            headers = mapOf(
                HttpHeaders.UserAgent to USER_AGENT,
                HttpHeaders.ContentType to CONTENT_TYPE,
                HttpHeaders.Authorization to "Bearer IGT:2:$token",
            ),
            body = "signed_body=SIGNATURE.${URLEncoder.encode(ObjectParser.toJson(map), StandardCharsets.UTF_8)}"
        )
    }
}