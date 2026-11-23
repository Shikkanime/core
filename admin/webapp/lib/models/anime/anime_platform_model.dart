import 'package:json_annotation/json_annotation.dart';
import 'package:shikkanime_admin/core/network/proto/annotations.dart';
import 'package:shikkanime_admin/core/network/proto/enum_values.dart';

part 'anime_platform_model.g.dart';

@ProtoMessage()
@JsonSerializable(createToJson: false)
/// Source link of an anime on a streaming platform.
class AnimePlatformModel {
  @ProtoEnum(platformValues)
  final String platform;
  final String platformId;

  const AnimePlatformModel(this.platform, this.platformId);

  factory AnimePlatformModel.fromJson(Map<String, dynamic> json) =>
      _$AnimePlatformModelFromJson(json);
}
