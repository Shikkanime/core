// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'episode_summary_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

EpisodeSummaryModel _$EpisodeSummaryModelFromJson(Map<String, dynamic> json) =>
    EpisodeSummaryModel(
      json['id'] as String,
      (json['season'] as num).toInt(),
      $enumDecode(
        _$EpisodeTypeEnumMap,
        json['episodeType'],
        unknownValue: EpisodeType.unknown,
      ),
      (json['number'] as num).toInt(),
      DateTime.parse(json['releaseDateTime'] as String),
      (json['duration'] as num).toInt(),
      json['title'] as String?,
      (json['releaseCount'] as num).toInt(),
      (json['localizationCount'] as num).toInt(),
    );

const _$EpisodeTypeEnumMap = {
  EpisodeType.EPISODE: 'EPISODE',
  EpisodeType.FILM: 'FILM',
  EpisodeType.SPECIAL: 'SPECIAL',
  EpisodeType.SUMMARY: 'SUMMARY',
  EpisodeType.SPIN_OFF: 'SPIN_OFF',
  EpisodeType.unknown: 'unknown',
};
