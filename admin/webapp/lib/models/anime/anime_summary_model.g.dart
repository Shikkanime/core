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
      (json['platforms'] as List<dynamic>)
          .map(
            (e) => $enumDecode(
              _$PlatformEnumMap,
              e,
              unknownValue: Platform.unknown,
            ),
          )
          .toList(),
      (json['localizationCount'] as num).toInt(),
    );

const _$PlatformEnumMap = {
  Platform.ANIMATION_DIGITAL_NETWORK: 'ANIMATION_DIGITAL_NETWORK',
  Platform.CRUNCHYROLL: 'CRUNCHYROLL',
  Platform.DISNEY_PLUS: 'DISNEY_PLUS',
  Platform.NETFLIX: 'NETFLIX',
  Platform.PRIME_VIDEO: 'PRIME_VIDEO',
  Platform.unknown: 'unknown',
};
