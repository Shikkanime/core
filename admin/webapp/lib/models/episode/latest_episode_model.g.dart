// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'latest_episode_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LatestEpisodeModel _$LatestEpisodeModelFromJson(Map<String, dynamic> json) =>
    LatestEpisodeModel(
      json['id'] as String,
      json['animeId'] as String,
      json['animeTitle'] as String,
      (json['season'] as num).toInt(),
      $enumDecode(
        _$EpisodeTypeEnumMap,
        json['episodeType'],
        unknownValue: EpisodeType.unknown,
      ),
      (json['number'] as num).toInt(),
      DateTime.parse(json['releaseDateTime'] as String),
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
