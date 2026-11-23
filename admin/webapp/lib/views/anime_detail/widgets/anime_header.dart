import 'package:material_ui/material_ui.dart';
import 'package:shikkanime_admin/core/format/formatters.dart';
import 'package:shikkanime_admin/core/widgets/detail_widgets.dart';
import 'package:shikkanime_admin/core/widgets/metadata_link_chips.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/anime/anime_detail_model.dart';
import 'package:shikkanime_admin/models/image/image_model.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/image_artwork_row.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/localization_block.dart';

/// Card of one anime: its identity, audit columns, platforms, translations and artwork.
class AnimeHeader extends StatelessWidget {
  const AnimeHeader({
    super.key,
    required this.anime,
    required this.isSavingImage,
    required this.onUpdateImage,
    required this.onResetImage,
  });

  final AnimeDetailModel anime;

  /// Whether an artwork mutation is in flight; the edit actions stay disabled meanwhile.
  final bool isSavingImage;

  /// Overrides the artwork of one type; returns `null` on success or the error to display.
  final Future<String?> Function(ImageModel image, String url) onUpdateImage;

  /// Releases the manual management of one artwork; same result contract as [onUpdateImage].
  final Future<String?> Function(ImageModel image) onResetImage;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final localizations = AppLocalizations.of(context)!;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DetailInfoRow(label: localizations.identifier, value: anime.id),
            DetailInfoRow(
              label: localizations.createdAt,
              value: formatOptionalDateTime(anime.createdAt),
            ),
            DetailInfoRow(
              label: localizations.updatedAt,
              value: formatOptionalDateTime(anime.updatedAt),
            ),
            const Divider(height: 24),
            DetailSectionTitle(title: localizations.platforms),
            const SizedBox(height: 4),
            if (anime.platforms.isEmpty)
              const EmptyDetailValue()
            else
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: [
                  for (final platform in anime.platforms)
                    Chip(
                      visualDensity: VisualDensity.compact,
                      label: Text(
                        '${localizations.platformName(platform.platform)} '
                        '#${platform.platformId}',
                      ),
                    ),
                ],
              ),
            const Divider(height: 24),
            DetailSectionTitle(title: localizations.metadataLinks),
            const SizedBox(height: 4),
            MetadataLinkChips(links: anime.metadataLinks),
            const Divider(height: 24),
            DetailSectionTitle(title: localizations.localizations),
            const SizedBox(height: 4),
            if (anime.localizations.isEmpty)
              const EmptyDetailValue()
            else
              for (final localization in anime.localizations)
                AnimeLocalizationBlock(localization: localization),
            const Divider(height: 24),
            DetailSectionTitle(title: localizations.artwork),
            const SizedBox(height: 4),
            Text(
              localizations.artworkNotDisplayed,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 8),
            if (anime.images.isEmpty)
              const EmptyDetailValue()
            else
              for (final image in anime.images)
                ImageArtworkRow(
                  image: image,
                  enabled: !isSavingImage,
                  onSubmit: (url) => onUpdateImage(image, url),
                  onReset: () => onResetImage(image),
                ),
          ],
        ),
      ),
    );
  }
}
