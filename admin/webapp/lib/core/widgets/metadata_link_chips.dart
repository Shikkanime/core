import 'package:material_ui/material_ui.dart';
import 'package:shikkanime_admin/core/widgets/detail_widgets.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/metadata/metadata_link_model.dart';
import 'package:url_launcher/url_launcher.dart';

/// Chips of the links between a stored row and the external metadata platforms.
///
/// Each chip opens the entry on the platform's website in a new tab, so an administrator can
/// compare the stored data with the editorial record. Unlike the streaming platform chips, these
/// links carry no release information: they only identify the metadata entry.
class MetadataLinkChips extends StatelessWidget {
  const MetadataLinkChips({super.key, required this.links});

  final List<MetadataLinkModel> links;

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;

    if (links.isEmpty) return const EmptyDetailValue();

    return Wrap(
      spacing: 6,
      runSpacing: 6,
      children: [
        for (final link in links)
          ActionChip(
            visualDensity: VisualDensity.compact,
            avatar: const Icon(Icons.open_in_new, size: 16),
            label: Text(
              '${localizations.metadataPlatformName(link.platform)} '
              '#${link.platformId}',
            ),
            onPressed: link.url == null
                ? null
                : () => launchUrl(
                    Uri.parse(link.url!),
                    webOnlyWindowName: '_blank',
                  ),
          ),
      ],
    );
  }
}
