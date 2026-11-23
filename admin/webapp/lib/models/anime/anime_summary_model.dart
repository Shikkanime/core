import 'package:json_annotation/json_annotation.dart';
import 'package:shikkanime_admin/core/network/proto/annotations.dart';
import 'package:shikkanime_admin/models/enums/platform.dart';

part 'anime_summary_model.g.dart';

/// Compact view of one anime in the catalog list.
@ProtoMessage()
@JsonSerializable(createToJson: false)
class AnimeSummaryModel {
  final String id;
  final String title;
  final String normalizedTitle;

  @JsonKey(unknownEnumValue: Platform.unknown)
  final List<Platform> platforms;
  final int localizationCount;

  const AnimeSummaryModel(
    this.id,
    this.title,
    this.normalizedTitle,
    this.platforms,
    this.localizationCount,
  );

  factory AnimeSummaryModel.fromJson(Map<String, dynamic> json) =>
      _$AnimeSummaryModelFromJson(json);
}
