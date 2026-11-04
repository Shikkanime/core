import 'package:intl/intl.dart';

final _dateFormat = DateFormat('dd/MM/yyyy HH:mm', 'fr_FR');

/// Formats a release date for the administration.
String formatDateTime(DateTime value) => _dateFormat.format(value);

/// Formats an optional audit date, with a dash when the API did not publish one.
String formatOptionalDateTime(DateTime? value) =>
    value == null ? '—' : formatDateTime(value);

/// Formats a duration in seconds as whole minutes.
int durationInMinutes(int seconds) => (seconds / 60).round();
