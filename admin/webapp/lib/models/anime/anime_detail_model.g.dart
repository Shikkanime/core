// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'anime_detail_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AnimeDetailModel _$AnimeDetailModelFromJson(Map<String, dynamic> json) =>
    AnimeDetailModel(
      json['id'] as String,
      json['normalizedTitle'] as String,
      json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
      json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
      (json['localizations'] as List<dynamic>)
          .map(
            (e) => AnimeLocalizationModel.fromJson(e as Map<String, dynamic>),
          )
          .toList(),
      (json['platforms'] as List<dynamic>)
          .map((e) => AnimePlatformModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      (json['images'] as List<dynamic>)
          .map((e) => ImageModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
