package fr.shikkanime.utils

enum class ConfigPropertyKey(val key: String) {
    SIMULCAST_RANGE("simulcast_range"),
    DISCORD_TOKEN("discord_token"),
    TWITTER_CONSUMER_KEY("twitter_consumer_key"),
    TWITTER_CONSUMER_SECRET("twitter_consumer_secret"),
    TWITTER_ACCESS_TOKEN("twitter_access_token"),
    TWITTER_ACCESS_TOKEN_SECRET("twitter_access_token_secret"),
    USE_CRUNCHYROLL_API("use_crunchyroll_api"),
}