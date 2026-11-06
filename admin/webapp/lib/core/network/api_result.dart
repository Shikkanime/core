sealed class ApiResult<T> {
  const ApiResult();
}

/// Result of a call that succeeded, carrying its payload.
class ApiSuccess<T> extends ApiResult<T> {
  const ApiSuccess(this.data);

  final T data;

  @override
  String toString() => 'ApiSuccess($data)';
}

/// Result of a call that failed, carrying the message and the HTTP status.
class ApiFailure<T> extends ApiResult<T> {
  const ApiFailure([this.error, this.statusCode]);

  final String? error;
  final int? statusCode;

  @override
  String toString() => 'ApiFailure($statusCode: $error)';
}
