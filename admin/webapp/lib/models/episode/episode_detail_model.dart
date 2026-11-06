import 'package:json_annotation/json_annotation.dart';
import 'package:shikkanime_admin/models/episode/episode_localization_model.dart';
import 'package:shikkanime_admin/models/episode/episode_release_model.dart';
import 'package:shikkanime_admin/models/image/image_model.dart';
import 'package:shikkanime_admin/models/shikk_model.dart';

part 'episode_detail_model.g.dart';

/// Full administration view of one episode.
@JsonSerializable(createToJson: false)
class EpisodeDetailModel implements ShikkModel {
  @override
  final String id;
  final String? animeId;
  final int season;
  final String episodeType;
  final int number;
  final DateTime releaseDateTime;
  final int duration;
  @override
  final DateTime? createdAt;
  @override
  final DateTime? updatedAt;
  final List<EpisodeLocalizationModel> localizations;
  final List<EpisodeReleaseModel> releases;
  final List<ImageModel> images;

  const EpisodeDetailModel(
    this.id,
    this.animeId,
    this.season,
    this.episodeType,
    this.number,
    this.releaseDateTime,
    this.duration,
    this.createdAt,
    this.updatedAt,
    this.localizations,
    this.releases,
    this.images,
  );

  factory EpisodeDetailModel.fromJson(Map<String, dynamic> json) =>
      _$EpisodeDetailModelFromJson(json);

  /// Title displayed for the episode: the French one when it exists, the first one otherwise.
  String? get displayTitle {
    for (final localization in localizations) {
      if (localization.locale.startsWith('FR') && localization.title != null) {
        return localization.title;
      }
    }

    for (final localization in localizations) {
      if (localization.title != null) {
        return localization.title;
      }
    }

    return null;
  }
}
