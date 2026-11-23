import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/enums/image_type.dart';
import 'package:shikkanime_admin/models/image/image_model.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/image_artwork_row.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(
    localizationsDelegates: AppLocalizations.localizationsDelegates,
    supportedLocales: AppLocalizations.supportedLocales,
    home: Scaffold(body: child),
  );

  ImageArtworkRow row({
    ImageModel image = const ImageModel(ImageType.THUMBNAIL, 'https://a'),
    bool enabled = true,
    Future<String?> Function(String url)? onSubmit,
    Future<String?> Function()? onReset,
  }) => ImageArtworkRow(
    image: image,
    enabled: enabled,
    onSubmit: onSubmit ?? (_) async => null,
    onReset: onReset ?? () async => null,
  );

  testWidgets('shows the url as an always-editable field', (tester) async {
    await tester.pumpWidget(wrap(row()));

    final field = tester.widget<TextField>(find.byType(TextField));

    expect(field.controller!.text, 'https://a');
    expect(field.enabled, isTrue);
    expect(find.byIcon(Icons.edit_outlined), findsNothing);
  });

  testWidgets(
    'shows the manual badge and the release action on a manual image',
    (tester) async {
      await tester.pumpWidget(
        wrap(
          row(
            image: const ImageModel(
              ImageType.THUMBNAIL,
              'https://a',
              manual: true,
            ),
          ),
        ),
      );

      expect(find.text('Manual'), findsOneWidget);
      expect(find.byIcon(Icons.undo), findsOneWidget);
    },
  );

  testWidgets('forwards the edited url to onSubmit on enter', (tester) async {
    String? submitted;

    await tester.pumpWidget(
      wrap(
        row(
          onSubmit: (url) async {
            submitted = url;
            return null;
          },
        ),
      ),
    );

    await tester.enterText(find.byType(TextField), 'https://new');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();

    expect(submitted, 'https://new');
    expect(find.text('Artwork updated'), findsOneWidget);
  });

  testWidgets('does not submit an unchanged url', (tester) async {
    var submitted = false;

    await tester.pumpWidget(
      wrap(
        row(
          onSubmit: (_) async {
            submitted = true;
            return null;
          },
        ),
      ),
    );

    await tester.enterText(find.byType(TextField), 'https://a');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();

    expect(submitted, isFalse);
  });

  testWidgets('restores the stored url on escape', (tester) async {
    var submitted = false;

    await tester.pumpWidget(
      wrap(
        row(
          onSubmit: (_) async {
            submitted = true;
            return null;
          },
        ),
      ),
    );

    await tester.enterText(find.byType(TextField), 'https://new');
    await tester.sendKeyEvent(LogicalKeyboardKey.escape);
    await tester.pumpAndSettle();

    expect(submitted, isFalse);
    expect(
      tester.widget<TextField>(find.byType(TextField)).controller!.text,
      'https://a',
    );
  });

  testWidgets('forwards the release to onReset', (tester) async {
    var reset = false;

    await tester.pumpWidget(
      wrap(
        row(
          image: const ImageModel(
            ImageType.THUMBNAIL,
            'https://a',
            manual: true,
          ),
          onReset: () async {
            reset = true;
            return null;
          },
        ),
      ),
    );

    await tester.tap(find.byIcon(Icons.undo));
    await tester.pumpAndSettle();

    expect(reset, isTrue);
    expect(find.text('Artwork released to platform harvests'), findsOneWidget);
  });

  testWidgets('disables the field while a mutation is in flight', (
    tester,
  ) async {
    await tester.pumpWidget(wrap(row(enabled: false)));

    final field = tester.widget<TextField>(find.byType(TextField));

    expect(field.enabled, isFalse);
  });
}
