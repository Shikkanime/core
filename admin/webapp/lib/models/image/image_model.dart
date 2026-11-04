import 'package:json_annotation/json_annotation.dart';

part 'image_model.g.dart';

@JsonSerializable(createToJson: false)
/// One artwork row of an aggregate, as the administration reads it.
class ImageModel {
  final String type;
  final String url;

  const ImageModel(this.type, this.url);

  factory ImageModel.fromJson(Map<String, dynamic> json) =>
      _$ImageModelFromJson(json);
}
