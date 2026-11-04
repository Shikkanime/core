// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'anime_summary_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AnimeSummaryModel _$AnimeSummaryModelFromJson(Map<String, dynamic> json) =>
    AnimeSummaryModel(
      json['id'] as String,
      json['title'] as String,
      json['normalizedTitle'] as String,
      (json['platforms'] as List<dynamic>).map((e) => e as String).toList(),
      (json['localizationCount'] as num).toInt(),
    );
