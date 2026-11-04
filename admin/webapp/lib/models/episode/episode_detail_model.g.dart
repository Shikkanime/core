// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'episode_detail_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

EpisodeDetailModel _$EpisodeDetailModelFromJson(Map<String, dynamic> json) =>
    EpisodeDetailModel(
      json['id'] as String,
      json['animeId'] as String?,
      (json['season'] as num).toInt(),
      json['episodeType'] as String,
      (json['number'] as num).toInt(),
      DateTime.parse(json['releaseDateTime'] as String),
      (json['duration'] as num).toInt(),
      json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
      json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
      (json['localizations'] as List<dynamic>)
          .map(
            (e) => EpisodeLocalizationModel.fromJson(e as Map<String, dynamic>),
          )
          .toList(),
      (json['releases'] as List<dynamic>)
          .map((e) => EpisodeReleaseModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      (json['images'] as List<dynamic>)
          .map((e) => ImageModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
