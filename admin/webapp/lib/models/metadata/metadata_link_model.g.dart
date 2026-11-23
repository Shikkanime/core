// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'metadata_link_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

MetadataLinkModel _$MetadataLinkModelFromJson(Map<String, dynamic> json) =>
    MetadataLinkModel(
      $enumDecode(
        _$MetadataPlatformEnumMap,
        json['platform'],
        unknownValue: MetadataPlatform.unknown,
      ),
      json['platformId'] as String,
      json['platformAnimeId'] as String?,
    );

const _$MetadataPlatformEnumMap = {
  MetadataPlatform.ANILIST: 'ANILIST',
  MetadataPlatform.MYANIMELIST: 'MYANIMELIST',
  MetadataPlatform.ANIDB: 'ANIDB',
  MetadataPlatform.unknown: 'unknown',
};
