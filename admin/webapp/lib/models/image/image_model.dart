import 'package:json_annotation/json_annotation.dart';

part 'image_model.g.dart';

@JsonSerializable(createToJson: false)
/// One artwork row of an aggregate, as the administration reads it.
///
/// A [manual] image was set by an administrator and is never replaced by a platform harvest.
class ImageModel {
  final String type;
  final String url;

  @JsonKey(defaultValue: false)
  final bool manual;

  const ImageModel(this.type, this.url, {this.manual = false});

  factory ImageModel.fromJson(Map<String, dynamic> json) =>
      _$ImageModelFromJson(json);
}
