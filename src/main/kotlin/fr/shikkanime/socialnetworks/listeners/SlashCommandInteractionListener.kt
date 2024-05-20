package fr.shikkanime.socialnetworks.listeners

import fr.shikkanime.socialnetworks.DiscordSocialNetwork.Channel
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.io.File

class SlashCommandInteractionListener : ListenerAdapter() {
    private fun getFile() = File(Constant.configFolder, "discord_channels.json")

    private fun getChannels(file: File): MutableList<Channel> {
        return if (file.exists()) {
            ObjectParser.fromJson(file.readText(), Array<Channel>::class.java).toMutableList()
        } else {
            mutableListOf()
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!event.name.equals("add-release-channel", true)) return

        val channel = event.getOption("channel")?.asChannel ?: run {
            event.reply("Channel is required").setEphemeral(true).queue()
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

        val file = getFile()
        val channels = getChannels(file)

        // If channel id already exists, update the release type and animes
        val existingChannel = channels.firstOrNull { it.id == channel.idLong }

        if (existingChannel != null) {
            val oldReleaseType = existingChannel.releaseType
            existingChannel.releaseType = releaseType

            if (oldReleaseType != releaseType && oldReleaseType == "CUSTOM") {
                existingChannel.animes.clear()
            }

            if (releaseType == "CUSTOM" && !animeName.isNullOrBlank() && !existingChannel.animes.contains(
                    animeName
                )
            ) {
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
}