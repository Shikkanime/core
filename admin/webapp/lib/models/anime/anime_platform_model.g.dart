// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'anime_platform_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AnimePlatformModel _$AnimePlatformModelFromJson(Map<String, dynamic> json) =>
    AnimePlatformModel(
      $enumDecode(
        _$PlatformEnumMap,
        json['platform'],
        unknownValue: Platform.unknown,
      ),
      json['platformId'] as String,
    );

const _$PlatformEnumMap = {
  Platform.ANIMATION_DIGITAL_NETWORK: 'ANIMATION_DIGITAL_NETWORK',
  Platform.CRUNCHYROLL: 'CRUNCHYROLL',
  Platform.DISNEY_PLUS: 'DISNEY_PLUS',
  Platform.NETFLIX: 'NETFLIX',
  Platform.PRIME_VIDEO: 'PRIME_VIDEO',
  Platform.unknown: 'unknown',
};
