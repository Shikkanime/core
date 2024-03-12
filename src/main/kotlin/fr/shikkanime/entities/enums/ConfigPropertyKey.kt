package fr.shikkanime.entities.enums

enum class ConfigPropertyKey(val key: String) {
    SIMULCAST_RANGE("simulcast_range"),
    DISCORD_TOKEN("discord_token"),
    TWITTER_CONSUMER_KEY("twitter_consumer_key"),
    TWITTER_CONSUMER_SECRET("twitter_consumer_secret"),
    TWITTER_ACCESS_TOKEN("twitter_access_token"),
    TWITTER_ACCESS_TOKEN_SECRET("twitter_access_token_secret"),
    USE_CRUNCHYROLL_API("use_crunchyroll_api"),
    SEO_DESCRIPTION("seo_description"),
    SOCIAL_NETWORK_EPISODES_SIZE_LIMIT("social_network_episodes_size_limit"),
    FETCH_OLD_EPISODE_DESCRIPTION_SIZE("fetch_old_episode_description_size"),
    GOOGLE_SITE_VERIFICATION_ID("google_site_verification_id"),
    FETCH_DEPRECATED_EPISODE_DATE("fetch_deprecated_episode_date"),
    CHECK_CRUNCHYROLL_SIMULCASTS("check_crunchyroll_simulcasts"),
    BSKY_IDENTIFIER("bsky_identifier"),
    BSKY_PASSWORD("bsky_password"),
    TWITTER_MESSAGE("twitter_message"),
    THREADS_USERNAME("threads_username"),
    THREADS_PASSWORD("threads_password"),
    THREADS_MESSAGE("threads_message"),
    BSKY_MESSAGE("bsky_message"),
    BSKY_SESSION_TIMEOUT("bsky_session_timeout"),
    THREADS_SESSION_TIMEOUT("threads_session_timeout"),
    SIMULCAST_RANGE_DELAY("simulcast_range_delay"),
    ANALYTICS_DOMAIN("analytics_domain"),
    ANALYTICS_API("analytics_api"),
    ANALYTICS_SCRIPT("analytics_script"),
}