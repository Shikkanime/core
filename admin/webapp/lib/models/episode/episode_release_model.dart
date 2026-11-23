import 'package:json_annotation/json_annotation.dart';
import 'package:shikkanime_admin/core/network/proto/annotations.dart';
import 'package:shikkanime_admin/core/network/proto/enum_values.dart';

part 'episode_release_model.g.dart';

@ProtoMessage()
@JsonSerializable(createToJson: false)
/// One platform release of an episode.
class EpisodeReleaseModel {
  @ProtoEnum(platformValues)
  final String platform;
  final String platformId;
  final String url;

  @ProtoEnum(localeValues)
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
