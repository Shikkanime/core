import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:shikkanime_admin/core/settings/response_format.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/viewmodels/settings_view_model.dart';

/// Screen selecting the wire format used by the administration API client.
class SettingsView extends StatelessWidget {
  const SettingsView({super.key});

  String _label(AppLocalizations localizations, ResponseFormat format) =>
      switch (format) {
        ResponseFormat.json => localizations.formatJson,
        ResponseFormat.protobuf => localizations.formatProtobuf,
        ResponseFormat.cbor => localizations.formatCbor,
      };

  String _description(AppLocalizations localizations, ResponseFormat format) =>
      switch (format) {
        ResponseFormat.json => localizations.formatJsonDescription,
        ResponseFormat.protobuf => localizations.formatProtobufDescription,
        ResponseFormat.cbor => localizations.formatCborDescription,
      };

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final viewModel = context.watch<SettingsViewModel>();

    return Scaffold(
      appBar: AppBar(title: Text(localizations.settings)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(localizations.requestFormat, style: theme.textTheme.titleLarge),
          const SizedBox(height: 8),
          Text(
            localizations.requestFormatDescription,
            style: theme.textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          SegmentedButton<ResponseFormat>(
            segments: [
              for (final format in ResponseFormat.values)
                ButtonSegment<ResponseFormat>(
                  value: format,
                  label: Text(_label(localizations, format)),
                ),
            ],
            selected: {viewModel.format},
            showSelectedIcon: false,
            onSelectionChanged: (selection) =>
                viewModel.select(selection.single),
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                _description(localizations, viewModel.format),
                style: theme.textTheme.bodyMedium,
              ),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            localizations.formatAppliedOnNextRequest,
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}
