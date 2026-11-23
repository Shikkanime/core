// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'episode_release_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

EpisodeReleaseModel _$EpisodeReleaseModelFromJson(Map<String, dynamic> json) =>
    EpisodeReleaseModel(
      $enumDecode(
        _$PlatformEnumMap,
        json['platform'],
        unknownValue: Platform.unknown,
      ),
      json['platformId'] as String,
      json['url'] as String,
      $enumDecode(
        _$ContentLocaleEnumMap,
        json['audioLocale'],
        unknownValue: ContentLocale.unknown,
      ),
      json['uncensored'] as bool,
      DateTime.parse(json['releaseDateTime'] as String),
    );

const _$PlatformEnumMap = {
  Platform.ANIMATION_DIGITAL_NETWORK: 'ANIMATION_DIGITAL_NETWORK',
  Platform.CRUNCHYROLL: 'CRUNCHYROLL',
  Platform.DISNEY_PLUS: 'DISNEY_PLUS',
  Platform.NETFLIX: 'NETFLIX',
  Platform.PRIME_VIDEO: 'PRIME_VIDEO',
  Platform.unknown: 'unknown',
};

const _$ContentLocaleEnumMap = {
  ContentLocale.JA_JP: 'JA_JP',
  ContentLocale.EN_US: 'EN_US',
  ContentLocale.FR_FR: 'FR_FR',
  ContentLocale.FR_BE: 'FR_BE',
  ContentLocale.FR_LU: 'FR_LU',
  ContentLocale.FR_CH: 'FR_CH',
  ContentLocale.CA_AD: 'CA_AD',
  ContentLocale.FR_MC: 'FR_MC',
  ContentLocale.DE_DE: 'DE_DE',
  ContentLocale.PL_PL: 'PL_PL',
  ContentLocale.ZH_CN: 'ZH_CN',
  ContentLocale.KO_KR: 'KO_KR',
  ContentLocale.unknown: 'unknown',
};
