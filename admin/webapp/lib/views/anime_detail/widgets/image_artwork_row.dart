import 'package:material_ui/material_ui.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/image/image_model.dart';

/// One artwork row of the anime detail: its type, its URL and its edit actions.
///
/// The URL is edited inline: the edit action swaps the text for a field, Enter or the check
/// action submits it to [onSubmit] and the close action abandons the change. A manual image
/// also carries a badge and a release action forwarded to [onReset]. Both callbacks return
/// `null` on success or the error message to display.
class ImageArtworkRow extends StatefulWidget {
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

  @override
  State<ImageArtworkRow> createState() => _ImageArtworkRowState();
}

class _ImageArtworkRowState extends State<ImageArtworkRow> {
  late final TextEditingController _controller;
  bool _editing = false;
  bool _saving = false;

  ImageModel get image => widget.image;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.image.url);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _startEdit() {
    _controller.text = widget.image.url;
    setState(() => _editing = true);
  }

  void _cancelEdit() {
    setState(() => _editing = false);
  }

  Future<void> _save() async {
    final url = _controller.text.trim();

    if (url.isEmpty || _saving) return;

    if (url == widget.image.url) {
      _cancelEdit();
      return;
    }

    setState(() => _saving = true);
    final error = await widget.onSubmit(url);

    if (!mounted) return;

    setState(() {
      _saving = false;
      if (error == null) _editing = false;
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(error ?? AppLocalizations.of(context)!.artworkUpdated),
      ),
    );
  }

  Future<void> _release() async {
    final localizations = AppLocalizations.of(context)!;
    final error = await widget.onReset();

    if (!mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(error ?? localizations.artworkReleased)),
    );
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
          Text(
            '${localizations.imageTypeName(image.type.name)} — ',
            style: theme.textTheme.bodySmall,
          ),
          Expanded(child: _buildValue(context, theme)),
          if (_editing) ...[
            IconButton(
              onPressed: _saving ? null : _save,
              tooltip: localizations.save,
              icon: const Icon(Icons.check),
              visualDensity: VisualDensity.compact,
            ),
            IconButton(
              onPressed: _saving ? null : _cancelEdit,
              tooltip: localizations.cancel,
              icon: const Icon(Icons.close),
              visualDensity: VisualDensity.compact,
            ),
          ] else ...[
            IconButton(
              onPressed: widget.enabled ? _startEdit : null,
              tooltip: localizations.editArtwork,
              icon: const Icon(Icons.edit_outlined),
              visualDensity: VisualDensity.compact,
            ),
            if (image.manual)
              IconButton(
                onPressed: widget.enabled ? _release : null,
                tooltip: localizations.releaseArtworkOverride,
                icon: const Icon(Icons.undo),
                visualDensity: VisualDensity.compact,
              ),
          ],
        ],
      ),
    );
  }

  Widget _buildValue(BuildContext context, ThemeData theme) {
    if (!_editing) {
      return SelectableText(image.url, style: theme.textTheme.bodySmall);
    }

    return TextField(
      controller: _controller,
      autofocus: true,
      enabled: !_saving,
      keyboardType: TextInputType.url,
      style: theme.textTheme.bodySmall,
      decoration: const InputDecoration(
        isDense: true,
        border: InputBorder.none,
        contentPadding: EdgeInsets.zero,
      ),
      onSubmitted: (_) => _save(),
    );
  }
}
