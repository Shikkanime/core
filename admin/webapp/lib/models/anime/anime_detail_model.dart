import 'package:json_annotation/json_annotation.dart';
import 'package:shikkanime_admin/models/anime/anime_localization_model.dart';
import 'package:shikkanime_admin/models/anime/anime_platform_model.dart';
import 'package:shikkanime_admin/models/image/image_model.dart';
import 'package:shikkanime_admin/models/shikk_model.dart';

part 'anime_detail_model.g.dart';

/// Full administration view of one anime.
@JsonSerializable(createToJson: false)
class AnimeDetailModel implements ShikkModel {
  @override
  final String id;
  final String normalizedTitle;
  @override
  final DateTime? createdAt;
  @override
  final DateTime? updatedAt;
  final List<AnimeLocalizationModel> localizations;
  final List<AnimePlatformModel> platforms;
  final List<ImageModel> images;

  const AnimeDetailModel(
    this.id,
    this.normalizedTitle,
    this.createdAt,
    this.updatedAt,
    this.localizations,
    this.platforms,
    this.images,
  );

  factory AnimeDetailModel.fromJson(Map<String, dynamic> json) =>
      _$AnimeDetailModelFromJson(json);
}
