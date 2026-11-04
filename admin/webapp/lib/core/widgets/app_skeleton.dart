import 'package:flutter/material.dart';
import 'package:shimmer_animation/shimmer_animation.dart';

/// Placeholder of a block whose content is still loading.
///
/// The shimmer runs over the page colour and under the canvas colour, exactly like the
/// `AppSkeleton` of the application v2: a screen keeps its final shape while the data arrives,
/// instead of collapsing to a spinner and jumping once it is loaded.
class AppSkeleton extends StatelessWidget {
  const AppSkeleton({super.key, this.borderRadius, this.width, this.height});

  /// Radius of the block; square when absent.
  final BorderRadius? borderRadius;

  /// Width of the block, filling its parent when absent.
  final double? width;

  /// Height of the block, filling its parent when absent.
  final double? height;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: width,
      height: height,
      child: ClipRRect(
        borderRadius: borderRadius ?? BorderRadius.zero,
        child: Shimmer(
          colorOpacity: 1,
          color: Theme.of(context).canvasColor,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: Theme.of(context).scaffoldBackgroundColor,
            ),
          ),
        ),
      ),
    );
  }
}
