import 'package:flutter/material.dart';
import 'package:shikkanime_admin/core/widgets/app_skeleton.dart';

/// Placeholder of the episode cards while the detail loads.
class EpisodeDetailSkeleton extends StatelessWidget {
  const EpisodeDetailSkeleton({super.key});

  @override
  Widget build(BuildContext context) => ListView(
    padding: const EdgeInsets.all(16),
    children: const [
      Card(
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
            ],
          ),
        ),
      ),
      SizedBox(height: 16),
      Card(
        child: Padding(
          padding: EdgeInsets.all(16),
          child: AppSkeleton(height: 80),
        ),
      ),
    ],
  );
}
