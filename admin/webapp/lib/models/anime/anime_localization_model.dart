import 'package:json_annotation/json_annotation.dart';
import 'package:shikkanime_admin/core/network/proto/annotations.dart';
import 'package:shikkanime_admin/core/network/proto/enum_values.dart';

part 'anime_localization_model.g.dart';

@ProtoMessage()
@JsonSerializable(createToJson: false)
/// One localized text of an anime.
class AnimeLocalizationModel {
  @ProtoEnum(localeValues)
  final String locale;
  final String? title;
  final String? description;

  const AnimeLocalizationModel(this.locale, this.title, this.description);

  factory AnimeLocalizationModel.fromJson(Map<String, dynamic> json) =>
      _$AnimeLocalizationModelFromJson(json);
}
