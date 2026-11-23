import 'package:json_annotation/json_annotation.dart';
import 'package:shikkanime_admin/core/network/proto/annotations.dart';
import 'package:shikkanime_admin/core/network/proto/enum_values.dart';

part 'episode_localization_model.g.dart';

@ProtoMessage()
@JsonSerializable(createToJson: false)
/// One localized text of an episode.
class EpisodeLocalizationModel {
  @ProtoEnum(localeValues)
  final String locale;
  final String? title;
  final String? description;

  const EpisodeLocalizationModel(this.locale, this.title, this.description);

  factory EpisodeLocalizationModel.fromJson(Map<String, dynamic> json) =>
      _$EpisodeLocalizationModelFromJson(json);
}
