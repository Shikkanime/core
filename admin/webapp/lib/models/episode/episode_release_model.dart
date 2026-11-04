import 'package:json_annotation/json_annotation.dart';

part 'episode_release_model.g.dart';

@JsonSerializable(createToJson: false)
/// One platform release of an episode.
class EpisodeReleaseModel {
  final String platform;
  final String platformId;
  final String url;
  final String audioLocale;
  final bool uncensored;
  final DateTime releaseDateTime;

  const EpisodeReleaseModel(
    this.platform,
    this.platformId,
    this.url,
    this.audioLocale,
    this.uncensored,
    this.releaseDateTime,
  );

  factory EpisodeReleaseModel.fromJson(Map<String, dynamic> json) =>
      _$EpisodeReleaseModelFromJson(json);
}
