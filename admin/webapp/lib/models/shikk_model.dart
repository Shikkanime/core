/// Identity and audit columns shared by every aggregate the administration reads.
///
/// It is the client counterpart of the Kotlin `ShikkModel`: projections that only need an
/// identity - the summary rows of a list - do not implement it, because they never carry the
/// audit columns.
abstract interface class ShikkModel {
  /// Identity of the aggregate, a UUID.
  String get id;

  /// Creation date, absent for a row created before the column existed.
  DateTime? get createdAt;

  /// Date of the last update, absent for a row never updated since its creation.
  DateTime? get updatedAt;
}
