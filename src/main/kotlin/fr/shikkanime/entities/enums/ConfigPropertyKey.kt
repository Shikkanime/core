package fr.shikkanime.entities.enums

enum class ConfigPropertyKey(val key: String) {
    SIMULCAST_RANGE("simulcast_range"),
    SIMULCAST_RANGE_DELAY("simulcast_range_delay"),
    ADMIN_EMAIL("admin_email"),
    ADDITIONAL_HEAD_TAGS("additional_head_tags"),
    ANIMATION_DITIGAL_NETWORK_SIMULCAST_DETECTION_REGEX("animation_digital_network_simulcast_detection_regex"),
    ANIME_EPISODES_SIZE_LIMIT("anime_episodes_size_limit"),
    AUTHORIZED_DOMAINS("authorized_domains"),
    BOT_ADDITIONAL_REGEX("bot_additional_regex"),
    BSKY_FIRST_MESSAGE("bsky_first_message"),
    BSKY_IDENTIFIER("bsky_identifier"),
    BSKY_PASSWORD("bsky_password"),
    BSKY_SECOND_MESSAGE("bsky_second_message"),
    BSKY_SESSION_TIMEOUT("bsky_session_timeout"),
    CHECK_PREVIOUS_AND_NEXT_EPISODES("check_previous_and_next_episodes"),
    CHECK_SIMULCAST("check_simulcast"),
    CRUNCHYROLL_FETCH_API_SIZE("crunchyroll_fetch_api_size"),
    DISABLE_BOT_DETECTION("disable_bot_detection"),
    DISCORD_TOKEN("discord_token"),
    DISNEY_PLUS_AUTHORIZATION("disney_plus_authorization"),
    DISNEY_PLUS_REFRESH_TOKEN("disney_plus_refresh_token"),
    EMAIL_HOST("email_host"),
    EMAIL_PASSWORD("email_password"),
    EMAIL_PORT("email_port"),
    EMAIL_USERNAME("email_username"),
    FETCH_OLD_EPISODES_LIMIT("fetch_old_episodes_limit"),
    FETCH_OLD_EPISODES_RANGE("fetch_old_episodes_range"),
    GOOGLE_SITE_VERIFICATION_ID("google_site_verification_id"),
    LAST_FETCH_OLD_EPISODES("last_fetch_old_episodes"),
    PREVIOUS_NEXT_EPISODES_DEPTH("previous_next_episodes_depth"),
    SEO_DESCRIPTION("seo_description"),
    SOCIAL_NETWORK_EPISODES_SIZE_LIMIT("social_network_episodes_size_limit"),
    THREADS_ACCESS_TOKEN("threads_access_token"),
    THREADS_APP_ID("threads_app_id"),
    THREADS_APP_SECRET("threads_app_secret"),
    THREADS_FIRST_MESSAGE("threads_first_message"),
    THREADS_SECOND_MESSAGE("threads_second_message"),
    TWITTER_ACCESS_TOKEN("twitter_access_token"),
    TWITTER_ACCESS_TOKEN_SECRET("twitter_access_token_secret"),
    TWITTER_CONSUMER_KEY("twitter_consumer_key"),
    TWITTER_CONSUMER_SECRET("twitter_consumer_secret"),
    TWITTER_FIRST_MESSAGE("twitter_first_message"),
    TWITTER_SECOND_MESSAGE("twitter_second_message"),
    UPDATE_ANIME_DELAY("update_anime_delay"),
    UPDATE_ANIME_SIZE("update_anime_size"),
    UPDATE_EPISODE_DELAY("update_episode_delay"),
    UPDATE_EPISODE_SIZE("update_episode_size"),
    USE_SECURITY_HEADERS("use_security_headers"),
    UPDATE_IMAGE_DELAY("update_image_delay"),
    UPDATE_IMAGE_SIZE("update_image_size"),
}