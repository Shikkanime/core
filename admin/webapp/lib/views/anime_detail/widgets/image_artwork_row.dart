import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/image/image_model.dart';

/// One artwork row of the anime detail: its type and its URL as an always-editable field.
///
/// The URL is a field in place: Enter or a focus loss submits it to [onSubmit], Escape restores
/// the stored value. An unchanged or emptied value is never sent. A manual image also carries
/// a badge and a release action forwarded to [onReset]. Both callbacks return `null` on success
/// or the error message to display.
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
  late final FocusNode _focusNode;
  bool _saving = false;

  ImageModel get image => widget.image;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.image.url);
    _focusNode = FocusNode()..addListener(_onFocusChange);
  }

  @override
  void didUpdateWidget(ImageArtworkRow oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (widget.image.url != oldWidget.image.url && !_focusNode.hasFocus) {
      _controller.text = widget.image.url;
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  /// Submits the field when it loses focus, like Enter does.
  void _onFocusChange() {
    if (!_focusNode.hasFocus) _save();
  }

  /// Restores the stored URL and leaves the field.
  void _revert() {
    _controller.text = widget.image.url;
    _focusNode.unfocus();
  }

  Future<void> _save() async {
    final url = _controller.text.trim();

    if (_saving || url.isEmpty) {
      if (url.isEmpty) _revert();
      return;
    }

    if (url == widget.image.url) return;

    setState(() => _saving = true);
    final error = await widget.onSubmit(url);

    if (!mounted) return;

    setState(() => _saving = false);

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
            Tooltip(
              message: localizations.manualArtwork,
              child: Icon(
                Icons.edit,
                size: 14,
                color: theme.colorScheme.primary,
              ),
            ),
            const SizedBox(width: 6),
          ],
          Text(
            '${localizations.imageTypeName(image.type.name)} — ',
            style: theme.textTheme.bodySmall,
          ),
          Expanded(
            child: CallbackShortcuts(
              bindings: {
                const SingleActivator(LogicalKeyboardKey.escape): _revert,
              },
              child: TextField(
                controller: _controller,
                focusNode: _focusNode,
                enabled: widget.enabled && !_saving,
                keyboardType: TextInputType.url,
                style: theme.textTheme.bodySmall,
                decoration: InputDecoration(
                  isDense: true,
                  border: const OutlineInputBorder(),
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 6,
                  ),
                ),
                onSubmitted: (_) {
                  _save();
                  _focusNode.unfocus();
                },
              ),
            ),
          ),
          if (image.manual)
            IconButton(
              onPressed: widget.enabled ? _release : null,
              tooltip: localizations.releaseArtworkOverride,
              icon: const Icon(Icons.undo),
              visualDensity: VisualDensity.compact,
            ),
        ],
      ),
    );
  }
}
