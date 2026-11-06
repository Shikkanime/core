import 'package:flutter/material.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';

/// Retryable failure of a screen that could not read the API.
class ErrorStateView extends StatelessWidget {
  const ErrorStateView({
    super.key,
    required this.message,
    required this.onRetry,
  });

  final String? message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.cloud_off,
              size: 48,
              color: Theme.of(context).colorScheme.error,
            ),
            const SizedBox(height: 16),
            Text(
              localizations.loadError,
              style: Theme.of(context).textTheme.titleMedium,
              textAlign: TextAlign.center,
            ),
            if (message != null) ...[
              const SizedBox(height: 8),
              Text(
                message!,
                style: Theme.of(context).textTheme.bodySmall,
                textAlign: TextAlign.center,
              ),
            ],
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: Text(localizations.retry),
            ),
          ],
        ),
      ),
    );
  }
}

/// Placeholder of an empty collection.
class EmptyStateView extends StatelessWidget {
  const EmptyStateView({super.key, required this.message});

  final String message;

  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Text(
        message,
        style: Theme.of(context).textTheme.bodyLarge,
        textAlign: TextAlign.center,
      ),
    ),
  );
}
