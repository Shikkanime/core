package fr.shikkanime.socialnetworks.listeners

import fr.shikkanime.socialnetworks.DiscordSocialNetwork
import fr.shikkanime.socialnetworks.DiscordSocialNetwork.Channel
import fr.shikkanime.utils.ObjectParser
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

private const val CHANNEL_IS_REQUIRED = "Channel is required"

class SlashCommandInteractionListener(val discordSocialNetwork: DiscordSocialNetwork) : ListenerAdapter() {
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name.lowercase()) {
            "add-release-channel" -> handleAddReleaseChannel(event)
            "remove-release-channel" -> handleRemoveReleaseChannel(event)
            "view-release-channel" -> handleViewReleaseChannel(event)
        }
    }

    private fun handleAddReleaseChannel(event: SlashCommandInteractionEvent) {
        val channel = event.getOption("channel")?.asChannel ?: run {
            event.reply(CHANNEL_IS_REQUIRED).setEphemeral(true).queue()
            return
        }

        // If we can send messages to the channel
        if (!channel.type.isMessage) {
            event.reply("I can't send messages to this channel").setEphemeral(true).queue()
            return
        }

        val animeName = event.getOption("anime")?.asString
        val releaseType = if (animeName.isNullOrBlank()) "ALL" else "CUSTOM"

        // Save the channel to the database
        event.reply("Channel added to receive release notifications").queue()

        val file = discordSocialNetwork.getFile()
        val channels = discordSocialNetwork.getChannels(file).toMutableList()

        // If channel id already exists, update the release type and animes
        val existingChannel = channels.firstOrNull { it.id == channel.idLong }

        if (existingChannel != null) {
            val oldReleaseType = existingChannel.releaseType
            existingChannel.releaseType = releaseType

            if (oldReleaseType != releaseType && oldReleaseType == "CUSTOM") {
                existingChannel.animes.clear()
            }

            if (releaseType == "CUSTOM" && !animeName.isNullOrBlank() && animeName !in existingChannel.animes) {
                existingChannel.animes.add(animeName)
            }
        } else {
            channels.add(
                Channel(
                    channel.idLong,
                    releaseType,
                    animeName?.let { mutableListOf(it) } ?: mutableListOf()))
        }

        file.writeText(ObjectParser.toJson(channels))
    }

    private fun handleRemoveReleaseChannel(event: SlashCommandInteractionEvent) {
        val channel = event.getOption("channel")?.asChannel ?: run {
            event.reply(CHANNEL_IS_REQUIRED).setEphemeral(true).queue()
            return
        }

        val animeName = event.getOption("anime")?.asString
        val file = discordSocialNetwork.getFile()
        val channels = discordSocialNetwork.getChannels(file).toMutableList()

        // Find the channel in the list
        val existingChannel = channels.firstOrNull { it.id == channel.idLong }

        if (existingChannel == null) {
            event.reply("This channel is not registered for notifications").setEphemeral(true).queue()
            return
        }

        if (animeName.isNullOrBlank() || existingChannel.releaseType == "ALL") {
            // Remove the entire channel if no anime specified or if the channel is set to ALL
            channels.removeIf { it.id == channel.idLong }

            val message = if (animeName.isNullOrBlank()) {
                "Channel removed from all release notifications"
            } else {
                "Channel removed from all release notifications (was set to receive ALL)"
            }

            event.reply(message).queue()
        } else {
            // Remove only the specified anime
            existingChannel.animes.remove(animeName)

            // If no animes left, remove the channel entirely
            if (existingChannel.animes.isEmpty()) {
                channels.removeIf { it.id == channel.idLong }
            }

            event.reply("Anime '$animeName' removed from channel notifications").queue()
        }

        file.writeText(ObjectParser.toJson(channels))
    }

    private fun handleViewReleaseChannel(event: SlashCommandInteractionEvent) {
        val channel = event.getOption("channel")?.asChannel ?: run {
            event.reply(CHANNEL_IS_REQUIRED).setEphemeral(true).queue()
            return
        }

        val file = discordSocialNetwork.getFile()
        val channels = discordSocialNetwork.getChannels(file)

        // Find the channel in the list
        val existingChannel = channels.firstOrNull { it.id == channel.idLong }

        if (existingChannel == null) {
            event.reply("This channel is not registered for notifications").setEphemeral(true).queue()
            return
        }

        val message = StringBuilder()
        message.append("Channel: <#${channel.id}>\n")
        message.append("Notification type: ${existingChannel.releaseType}\n")

        if (existingChannel.releaseType == "CUSTOM" && existingChannel.animes.isNotEmpty()) {
            message.append("Anime list:\n")
            existingChannel.animes.sorted().forEach { anime ->
                message.append("- $anime\n")
            }
        } else if (existingChannel.releaseType == "ALL") {
            message.append("This channel receives notifications for all animes")
        } else {
            message.append("No animes in the list")
        }

        event.reply(message.toString()).queue()
    }
}
