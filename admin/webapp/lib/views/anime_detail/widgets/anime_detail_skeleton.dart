import 'package:flutter/material.dart';
import 'package:shikkanime_admin/core/widgets/app_skeleton.dart';

/// Placeholder of the anime card while the detail loads.
class AnimeDetailSkeleton extends StatelessWidget {
  const AnimeDetailSkeleton({super.key});

  @override
  Widget build(BuildContext context) => const Card(
    child: Padding(
      padding: EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AppSkeleton(height: 20, width: 240),
          SizedBox(height: 16),
          AppSkeleton(height: 12),
          SizedBox(height: 8),
          AppSkeleton(height: 12),
          SizedBox(height: 8),
          AppSkeleton(height: 12, width: 320),
          SizedBox(height: 16),
          AppSkeleton(height: 12, width: 200),
        ],
      ),
    ),
  );
}
