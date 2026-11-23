import 'package:json_annotation/json_annotation.dart';
import 'package:shikkanime_admin/core/network/proto/annotations.dart';
import 'package:shikkanime_admin/core/network/proto/enum_values.dart';

part 'metadata_link_model.g.dart';

/// Link of a stored anime or episode to its entry on an external metadata platform
/// (AniList, MyAnimeList, AniDB).
@ProtoMessage()
@JsonSerializable(createToJson: false)
class MetadataLinkModel {
  /// Metadata platform owning the entry, as the Kotlin enum name.
  @ProtoEnum(metadataPlatformValues)
  final String platform;

  /// Identifier of the entry on the platform.
  ///
  /// For MyAnimeList episode links this is the composite `"{animeId}/{episodeNumber}"`,
  /// because episode identifiers are only unique inside their anime. For AniDB episode
  /// links it is the episode `eid`.
  final String platformId;

  /// Identifier of the parent anime entry on the platform, for episode links.
  final String? platformAnimeId;

  const MetadataLinkModel(this.platform, this.platformId, this.platformAnimeId);

  factory MetadataLinkModel.fromJson(Map<String, dynamic> json) =>
      _$MetadataLinkModelFromJson(json);

  /// Whether this link targets an episode entry rather than the anime entry itself.
  bool get isEpisodeLink => platformAnimeId != null;

  /// Public URL of the linked entry on the platform's website.
  ///
  /// Episode links resolve to the episode page when the platform exposes one
  /// (MyAnimeList, AniDB); otherwise they fall back to the parent anime page, which is
  /// what a manual review needs anyway. AniList exposes no episode entries.
  String? get url {
    if (isEpisodeLink) {
      final episodeUrl = _episodeUrl;
      if (episodeUrl != null) return episodeUrl;
    }

    final id = int.tryParse(platformAnimeId ?? platformId);
    if (id == null) return null;

    return switch (platform) {
      'ANILIST' => 'https://anilist.co/anime/$id',
      'MYANIMELIST' => 'https://myanimelist.net/anime/$id',
      'ANIDB' => 'https://anidb.net/anime/$id',
      _ => null,
    };
  }

  /// Episode page URL of this link, or null when the platform or identifier shape
  /// does not allow one.
  String? get _episodeUrl {
    final animeId = platformAnimeId;
    if (animeId == null) return null;

    return switch (platform) {
      // platformId is "{animeId}/{episodeNumber}" on MyAnimeList.
      'MYANIMELIST' => _malEpisodeUrl(animeId),
      // platformId is the AniDB episode `eid`.
      'ANIDB' =>
        'https://anidb.net/perl-bin/animedb.pl'
            '?show=ep&aid=$animeId&eid=$platformId',
      _ => null,
    };
  }

  /// MyAnimeList episode URL built from the composite `"{animeId}/{episodeNumber}"`
  /// identifier, or null when the episode number cannot be parsed.
  String? _malEpisodeUrl(String animeId) {
    final separator = platformId.indexOf('/');
    if (separator < 0) return null;

    final episodeNumber = int.tryParse(platformId.substring(separator + 1));
    if (episodeNumber == null) return null;

    return 'https://myanimelist.net/anime/$animeId/$episodeNumber';
  }
}
