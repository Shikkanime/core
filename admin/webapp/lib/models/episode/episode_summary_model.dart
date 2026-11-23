import 'package:json_annotation/json_annotation.dart';
import 'package:shikkanime_admin/core/network/proto/annotations.dart';
import 'package:shikkanime_admin/models/enums/episode_type.dart';

part 'episode_summary_model.g.dart';

/// Compact view of one episode in the episode list of an anime.
@ProtoMessage()
@JsonSerializable(createToJson: false)
class EpisodeSummaryModel {
  final String id;
  final int season;

  @JsonKey(unknownEnumValue: EpisodeType.unknown)
  final EpisodeType episodeType;
  final int number;
  final DateTime releaseDateTime;
  final int duration;
  final String? title;
  final int releaseCount;
  final int localizationCount;

  const EpisodeSummaryModel(
    this.id,
    this.season,
    this.episodeType,
    this.number,
    this.releaseDateTime,
    this.duration,
    this.title,
    this.releaseCount,
    this.localizationCount,
  );

  factory EpisodeSummaryModel.fromJson(Map<String, dynamic> json) =>
      _$EpisodeSummaryModelFromJson(json);
}
