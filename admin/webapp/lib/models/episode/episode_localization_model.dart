import 'package:json_annotation/json_annotation.dart';

part 'episode_localization_model.g.dart';

@JsonSerializable(createToJson: false)
/// One localized text of an episode.
class EpisodeLocalizationModel {
  final String locale;
  final String? title;
  final String? description;

  const EpisodeLocalizationModel(this.locale, this.title, this.description);

  factory EpisodeLocalizationModel.fromJson(Map<String, dynamic> json) =>
      _$EpisodeLocalizationModelFromJson(json);
}
