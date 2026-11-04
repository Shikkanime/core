// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'episode_release_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

EpisodeReleaseModel _$EpisodeReleaseModelFromJson(Map<String, dynamic> json) =>
    EpisodeReleaseModel(
      json['platform'] as String,
      json['platformId'] as String,
      json['url'] as String,
      json['audioLocale'] as String,
      json['uncensored'] as bool,
      DateTime.parse(json['releaseDateTime'] as String),
    );
