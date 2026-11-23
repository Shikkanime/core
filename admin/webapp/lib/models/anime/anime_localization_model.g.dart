// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'anime_localization_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AnimeLocalizationModel _$AnimeLocalizationModelFromJson(
  Map<String, dynamic> json,
) => AnimeLocalizationModel(
  $enumDecode(
    _$ContentLocaleEnumMap,
    json['locale'],
    unknownValue: ContentLocale.unknown,
  ),
  json['title'] as String?,
  json['description'] as String?,
);

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
