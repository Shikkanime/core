import 'package:flutter/material.dart';
import 'package:shikkanime_admin/core/widgets/app_skeleton.dart';

/// Placeholder of the catalog rows while the first page loads.
class AnimeListSkeleton extends StatelessWidget {
  const AnimeListSkeleton({super.key, this.rows = 10});

  /// Number of placeholder rows displayed.
  final int rows;

  @override
  Widget build(BuildContext context) => ListView.separated(
    itemCount: rows,
    separatorBuilder: (context, index) => const Divider(height: 1),
    itemBuilder: (context, index) => const Padding(
      padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: AppSkeleton(height: 40),
    ),
  );
}
