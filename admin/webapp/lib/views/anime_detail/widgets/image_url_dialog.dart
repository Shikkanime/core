import 'package:material_ui/material_ui.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';

/// Dialog editing the URL of one artwork.
///
/// Pops with the trimmed URL on save, or `null` when cancelled.
class ImageUrlDialog extends StatefulWidget {
  const ImageUrlDialog({
    super.key,
    required this.title,
    required this.initialUrl,
  });

  final String title;
  final String initialUrl;

  @override
  State<ImageUrlDialog> createState() => _ImageUrlDialogState();
}

class _ImageUrlDialogState extends State<ImageUrlDialog> {
  late final TextEditingController _controller = TextEditingController(
    text: widget.initialUrl,
  );
  bool _canSave = true;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _save() {
    final url = _controller.text.trim();

    if (url.isEmpty) return;

    Navigator.of(context).pop(url);
  }

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;

    return AlertDialog(
      title: Text(widget.title),
      content: TextField(
        controller: _controller,
        autofocus: true,
        keyboardType: TextInputType.url,
        decoration: InputDecoration(labelText: localizations.imageUrl),
        onChanged: (value) {
          final canSave = value.trim().isNotEmpty;

          if (canSave != _canSave) {
            setState(() => _canSave = canSave);
          }
        },
        onSubmitted: (_) => _save(),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(localizations.cancel),
        ),
        FilledButton(
          onPressed: _canSave ? _save : null,
          child: Text(localizations.save),
        ),
      ],
    );
  }
}
