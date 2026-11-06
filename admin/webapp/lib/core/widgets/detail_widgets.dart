import 'package:flutter/material.dart';

/// Title of a section inside a detail card.
class DetailSectionTitle extends StatelessWidget {
  const DetailSectionTitle({super.key, required this.title});

  final String title;

  @override
  Widget build(BuildContext context) =>
      Text(title, style: Theme.of(context).textTheme.titleSmall);
}

/// Label and value of one field of a detail card.
class DetailInfoRow extends StatelessWidget {
  const DetailInfoRow({super.key, required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(
              label,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          Expanded(
            child: SelectableText(value, style: theme.textTheme.bodySmall),
          ),
        ],
      ),
    );
  }
}

/// Compact badge carrying a locale code.
class LocaleBadge extends StatelessWidget {
  const LocaleBadge({super.key, required this.locale});

  final String locale;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(locale, style: theme.textTheme.labelSmall),
    );
  }
}

/// Placeholder of a detail field the database leaves empty.
class EmptyDetailValue extends StatelessWidget {
  const EmptyDetailValue({super.key});

  @override
  Widget build(BuildContext context) => Text(
    '—',
    style: Theme.of(context).textTheme.bodySmall
        ?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
  );
}
