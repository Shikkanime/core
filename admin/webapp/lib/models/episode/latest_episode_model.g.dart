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
      json['episodeType'] as String,
      (json['number'] as num).toInt(),
      DateTime.parse(json['releaseDateTime'] as String),
      json['title'] as String?,
      (json['releaseCount'] as num).toInt(),
      (json['localizationCount'] as num).toInt(),
    );
