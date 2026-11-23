// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'image_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ImageModel _$ImageModelFromJson(Map<String, dynamic> json) => ImageModel(
  $enumDecode(
    _$ImageTypeEnumMap,
    json['type'],
    unknownValue: ImageType.unknown,
  ),
  json['url'] as String,
  manual: json['manual'] as bool? ?? false,
);

const _$ImageTypeEnumMap = {
  ImageType.THUMBNAIL: 'THUMBNAIL',
  ImageType.BANNER: 'BANNER',
  ImageType.TITLE: 'TITLE',
  ImageType.unknown: 'unknown',
};
