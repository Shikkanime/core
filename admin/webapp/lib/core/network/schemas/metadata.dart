import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Metadata platform values in the order `kotlinx.serialization` numbers them for Protobuf.
const metadataPlatformValues = ['ANILIST', 'MYANIMELIST', 'ANIDB'];

/// Link of an anime to an external metadata platform entry.
const animeMetadataLinkSchema = ProtoSchema({
  1: ProtoField(
    'platform',
    ProtoKind.enumeration,
    enumValues: metadataPlatformValues,
  ),
  2: ProtoField('platformId', ProtoKind.string),
});

/// Link of an episode to an external metadata platform entry.
const episodeMetadataLinkSchema = ProtoSchema({
  1: ProtoField(
    'platform',
    ProtoKind.enumeration,
    enumValues: metadataPlatformValues,
  ),
  2: ProtoField('platformId', ProtoKind.string),
  3: ProtoField('platformAnimeId', ProtoKind.string, nullable: true),
});
