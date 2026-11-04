import 'package:json_annotation/json_annotation.dart';

part 'anime_localization_model.g.dart';

@JsonSerializable(createToJson: false)
/// One localized text of an anime.
class AnimeLocalizationModel {
  final String locale;
  final String? title;
  final String? description;

  const AnimeLocalizationModel(this.locale, this.title, this.description);

  factory AnimeLocalizationModel.fromJson(Map<String, dynamic> json) =>
      _$AnimeLocalizationModelFromJson(json);
}
