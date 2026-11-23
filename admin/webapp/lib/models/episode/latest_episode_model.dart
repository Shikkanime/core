import 'package:json_annotation/json_annotation.dart';
import 'package:shikkanime_admin/core/network/proto/annotations.dart';
import 'package:shikkanime_admin/models/enums/episode_type.dart';

part 'latest_episode_model.g.dart';

/// One recently released episode, with the anime it belongs to.
///
/// The row is the release list view of the API: [animeTitle] is the normalized title of the anime,
/// [title] the main title of the episode. The episode identity - season, type and number - is what
/// identifies it when it has no localized text yet.
@ProtoMessage()
@JsonSerializable(createToJson: false)
class LatestEpisodeModel {
  final String id;
  final String animeId;
  final String animeTitle;
  final int season;

  @JsonKey(unknownEnumValue: EpisodeType.unknown)
  final EpisodeType episodeType;
  final int number;
  final DateTime releaseDateTime;
  final String? title;
  final int releaseCount;
  final int localizationCount;

  const LatestEpisodeModel(
    this.id,
    this.animeId,
    this.animeTitle,
    this.season,
    this.episodeType,
    this.number,
    this.releaseDateTime,
    this.title,
    this.releaseCount,
    this.localizationCount,
  );

  factory LatestEpisodeModel.fromJson(Map<String, dynamic> json) =>
      _$LatestEpisodeModelFromJson(json);
}
