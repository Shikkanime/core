import 'package:material_ui/material_ui.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/image/image_model.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/image_url_dialog.dart';

/// One artwork row of the anime detail: its type, its URL and its edit actions.
///
/// The edit button opens [ImageUrlDialog] and forwards the new URL to [onSubmit]; a manual
/// image also carries a badge and a release action forwarded to [onReset]. Both callbacks
/// return `null` on success or the error message to display.
class ImageArtworkRow extends StatelessWidget {
  const ImageArtworkRow({
    super.key,
    required this.image,
    required this.enabled,
    required this.onSubmit,
    required this.onReset,
  });

  final ImageModel image;
  final bool enabled;
  final Future<String?> Function(String url) onSubmit;
  final Future<String?> Function() onReset;

  Future<void> _edit(BuildContext context) async {
    final localizations = AppLocalizations.of(context)!;
    final url = await showDialog<String>(
      context: context,
      builder: (context) => ImageUrlDialog(
        title: localizations.editImageTitle(
          localizations.imageTypeName(image.type.name),
        ),
        initialUrl: image.url,
      ),
    );

    if (url == null || !context.mounted) return;

    await _report(context, onSubmit(url), localizations.artworkUpdated);
  }

  Future<void> _release(BuildContext context) async {
    final localizations = AppLocalizations.of(context)!;

    await _report(context, onReset(), localizations.artworkReleased);
  }

  /// Reports the outcome of a mutation: the success wording, or the error it returned.
  Future<void> _report(
    BuildContext context,
    Future<String?> result,
    String successMessage,
  ) async {
    final error = await result;

    if (!context.mounted) return;

    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(error ?? successMessage)));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final localizations = AppLocalizations.of(context)!;

    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        children: [
          if (image.manual) ...[
            Chip(
              visualDensity: VisualDensity.compact,
              label: Text(localizations.manualArtwork),
            ),
            const SizedBox(width: 8),
          ],
          Expanded(
            child: SelectableText(
              '${localizations.imageTypeName(image.type.name)} — ${image.url}',
              style: theme.textTheme.bodySmall,
            ),
          ),
          IconButton(
            onPressed: enabled ? () => _edit(context) : null,
            tooltip: localizations.editArtwork,
            icon: const Icon(Icons.edit_outlined),
            visualDensity: VisualDensity.compact,
          ),
          if (image.manual)
            IconButton(
              onPressed: enabled ? () => _release(context) : null,
              tooltip: localizations.releaseArtworkOverride,
              icon: const Icon(Icons.undo),
              visualDensity: VisualDensity.compact,
            ),
        ],
      ),
    );
  }
}
