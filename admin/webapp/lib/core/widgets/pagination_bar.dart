import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';

/// Compact paginator of a paged collection.
///
/// It shows the current page, lets the administrator jump to another one, and keeps the first,
/// previous, next and last actions one click away. The total number of elements is displayed next
/// to the bounds, so the size of the result is visible without loading it.
class PaginationBar extends StatelessWidget {
  const PaginationBar({
    super.key,
    required this.page,
    required this.totalPages,
    required this.itemLabel,
    required this.onFirst,
    required this.onPrevious,
    required this.onNext,
    required this.onLast,
    required this.onGoToPage,
  });

  final int page;
  final int totalPages;
  final String itemLabel;
  final VoidCallback onFirst;
  final VoidCallback onPrevious;
  final VoidCallback onNext;
  final VoidCallback onLast;
  final ValueChanged<int> onGoToPage;

  @override
  Widget build(BuildContext context) {
    if (totalPages <= 0) {
      return const SizedBox.shrink();
    }

    final localizations = AppLocalizations.of(context)!;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Wrap(
        alignment: WrapAlignment.center,
        crossAxisAlignment: WrapCrossAlignment.center,
        spacing: 4,
        runSpacing: 4,
        children: [
          IconButton(
            onPressed: page > 1 ? onFirst : null,
            tooltip: localizations.firstPage,
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.first_page),
          ),
          IconButton(
            onPressed: page > 1 ? onPrevious : null,
            tooltip: localizations.previousPage,
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.chevron_left),
          ),
          _PageField(
            page: page,
            totalPages: totalPages,
            onSubmitted: onGoToPage,
          ),
          Text(
            localizations.ofTotal(totalPages),
            style: Theme.of(context).textTheme.bodySmall,
          ),
          IconButton(
            onPressed: page < totalPages ? onNext : null,
            tooltip: localizations.nextPage,
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.chevron_right),
          ),
          IconButton(
            onPressed: page < totalPages ? onLast : null,
            tooltip: localizations.lastPage,
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.last_page),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 8),
            child: Text(
              itemLabel,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _PageField extends StatefulWidget {
  const _PageField({
    required this.page,
    required this.totalPages,
    required this.onSubmitted,
  });

  final int page;
  final int totalPages;
  final ValueChanged<int> onSubmitted;

  @override
  State<_PageField> createState() => _PageFieldState();
}

class _PageFieldState extends State<_PageField> {
  late final TextEditingController _controller = TextEditingController(
    text: '${widget.page}',
  );

  @override
  void didUpdateWidget(covariant _PageField oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (widget.page != oldWidget.page) {
      _controller.text = '${widget.page}';
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _submit(String value) {
    final page = int.tryParse(value);

    if (page == null) {
      _controller.text = '${widget.page}';
      return;
    }

    widget.onSubmitted(page.clamp(1, widget.totalPages));
  }

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;

    return SizedBox(
      width: 56,
      height: 34,
      child: TextField(
        controller: _controller,
        textAlign: TextAlign.center,
        keyboardType: TextInputType.number,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        decoration: InputDecoration(
          labelText: localizations.page,
          labelStyle: Theme.of(context).textTheme.labelSmall,
          isDense: true,
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 6,
            vertical: 6,
          ),
          border: const OutlineInputBorder(),
        ),
        onSubmitted: _submit,
      ),
    );
  }
}
