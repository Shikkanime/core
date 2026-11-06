// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'episode_summary_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

EpisodeSummaryModel _$EpisodeSummaryModelFromJson(Map<String, dynamic> json) =>
    EpisodeSummaryModel(
      json['id'] as String,
      (json['season'] as num).toInt(),
      json['episodeType'] as String,
      (json['number'] as num).toInt(),
      DateTime.parse(json['releaseDateTime'] as String),
      (json['duration'] as num).toInt(),
      json['title'] as String?,
      (json['releaseCount'] as num).toInt(),
      (json['localizationCount'] as num).toInt(),
    );
