package fr.shikkanime.entities.enums

enum class ConfigPropertyKey(val key: String) {
    SIMULCAST_RANGE("simulcast_range"),
    DISCORD_TOKEN("discord_token"),
    TWITTER_CONSUMER_KEY("twitter_consumer_key"),
    TWITTER_CONSUMER_SECRET("twitter_consumer_secret"),
    TWITTER_ACCESS_TOKEN("twitter_access_token"),
    TWITTER_ACCESS_TOKEN_SECRET("twitter_access_token_secret"),
    SEO_DESCRIPTION("seo_description"),
    SOCIAL_NETWORK_EPISODES_SIZE_LIMIT("social_network_episodes_size_limit"),
    GOOGLE_SITE_VERIFICATION_ID("google_site_verification_id"),
    CHECK_CRUNCHYROLL_SIMULCASTS("check_crunchyroll_simulcasts"),
    BSKY_IDENTIFIER("bsky_identifier"),
    BSKY_PASSWORD("bsky_password"),
    TWITTER_FIRST_MESSAGE("twitter_first_message"),
    TWITTER_SECOND_MESSAGE("twitter_second_message"),
    THREADS_USERNAME("threads_username"),
    THREADS_PASSWORD("threads_password"),
    THREADS_MESSAGE("threads_message"),
    BSKY_MESSAGE("bsky_message"),
    BSKY_SESSION_TIMEOUT("bsky_session_timeout"),
    THREADS_SESSION_TIMEOUT("threads_session_timeout"),
    SIMULCAST_RANGE_DELAY("simulcast_range_delay"),
    CRUNCHYROLL_FETCH_API_SIZE("crunchyroll_fetch_api_size"),
    ANIMATION_DITIGAL_NETWORK_SIMULCAST_DETECTION_REGEX("animation_digital_network_simulcast_detection_regex"),
    ANIME_EPISODES_SIZE_LIMIT("anime_episodes_size_limit"),
    DISNEY_PLUS_AUTHORIZATION("disney_plus_authorization"),
    DISNEY_PLUS_REFRESH_TOKEN("disney_plus_refresh_token"),
    LAST_FETCH_OLD_EPISODES("last_fetch_old_episodes"),
    FETCH_OLD_EPISODES_RANGE("fetch_old_episodes_range"),
    FETCH_OLD_EPISODES_LIMIT("fetch_old_episodes_limit"),
    FETCH_OLD_EPISODES_EMAIL("fetch_old_episodes_email"),
    EMAIL_HOST("email_host"),
    EMAIL_PORT("email_port"),
    EMAIL_USERNAME("email_username"),
    EMAIL_PASSWORD("email_password"),
    USE_SECURITY_HEADERS("use_security_headers"),
    UPDATE_EPISODE_DELAY("update_episode_delay"),
    UPDATE_EPISODE_SIZE("update_episode_size"),
}