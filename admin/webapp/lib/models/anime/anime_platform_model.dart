import 'package:json_annotation/json_annotation.dart';

part 'anime_platform_model.g.dart';

@JsonSerializable(createToJson: false)
/// Source link of an anime on a streaming platform.
class AnimePlatformModel {
  final String platform;
  final String platformId;

  const AnimePlatformModel(this.platform, this.platformId);

  factory AnimePlatformModel.fromJson(Map<String, dynamic> json) =>
      _$AnimePlatformModelFromJson(json);
}
