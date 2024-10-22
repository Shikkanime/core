package fr.shikkanime.entities.enums

enum class ConfigPropertyKey(val key: String) {
    SIMULCAST_RANGE("simulcast_range"),
    DISCORD_TOKEN("discord_token"),
    SEO_DESCRIPTION("seo_description"),
    SOCIAL_NETWORK_EPISODES_SIZE_LIMIT("social_network_episodes_size_limit"),
    GOOGLE_SITE_VERIFICATION_ID("google_site_verification_id"),
    CHECK_CRUNCHYROLL_SIMULCASTS("check_crunchyroll_simulcasts"),

    // Twitter API
    TWITTER_CONSUMER_KEY("twitter_consumer_key"),
    TWITTER_CONSUMER_SECRET("twitter_consumer_secret"),
    TWITTER_ACCESS_TOKEN("twitter_access_token"),
    TWITTER_ACCESS_TOKEN_SECRET("twitter_access_token_secret"),
    TWITTER_FIRST_MESSAGE("twitter_first_message"),
    TWITTER_SECOND_MESSAGE("twitter_second_message"),

    // Threads API
    THREADS_APP_ID("threads_app_id"),
    THREADS_APP_SECRET("threads_app_secret"),
    THREADS_ACCESS_TOKEN("threads_access_token"),
    THREADS_FIRST_MESSAGE("threads_first_message"),
    THREADS_SECOND_MESSAGE("threads_second_message"),

    // Bsky API
    BSKY_IDENTIFIER("bsky_identifier"),
    BSKY_PASSWORD("bsky_password"),
    BSKY_SESSION_TIMEOUT("bsky_session_timeout"),
    BSKY_FIRST_MESSAGE("bsky_first_message"),
    BSKY_SECOND_MESSAGE("bsky_second_message"),

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
    UPDATE_ANIME_DELAY("update_anime_delay"),
    UPDATE_ANIME_SIZE("update_anime_size"),
}