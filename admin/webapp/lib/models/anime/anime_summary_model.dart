import 'package:json_annotation/json_annotation.dart';

part 'anime_summary_model.g.dart';

/// Compact view of one anime in the catalog list.
@JsonSerializable(createToJson: false)
class AnimeSummaryModel {
  final String id;
  final String title;
  final String normalizedTitle;
  final List<String> platforms;
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
