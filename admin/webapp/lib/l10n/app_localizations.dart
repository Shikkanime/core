import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[Locale('en')];

  /// No description provided for @appTitle.
  ///
  /// In en, this message translates to:
  /// **'Shikkanime — Administration'**
  String get appTitle;

  /// No description provided for @searchAnime.
  ///
  /// In en, this message translates to:
  /// **'Search a title or an identifier...'**
  String get searchAnime;

  /// No description provided for @refresh.
  ///
  /// In en, this message translates to:
  /// **'Refresh'**
  String get refresh;

  /// No description provided for @retry.
  ///
  /// In en, this message translates to:
  /// **'Retry'**
  String get retry;

  /// No description provided for @loading.
  ///
  /// In en, this message translates to:
  /// **'Loading...'**
  String get loading;

  /// No description provided for @loadError.
  ///
  /// In en, this message translates to:
  /// **'Unable to load the data'**
  String get loadError;

  /// No description provided for @noAnime.
  ///
  /// In en, this message translates to:
  /// **'No anime stored'**
  String get noAnime;

  /// No description provided for @noResult.
  ///
  /// In en, this message translates to:
  /// **'No anime matches this search'**
  String get noResult;

  /// No description provided for @noEpisode.
  ///
  /// In en, this message translates to:
  /// **'No episode stored for this anime'**
  String get noEpisode;

  /// No description provided for @episodes.
  ///
  /// In en, this message translates to:
  /// **'Episodes'**
  String get episodes;

  /// No description provided for @platforms.
  ///
  /// In en, this message translates to:
  /// **'Platforms'**
  String get platforms;

  /// No description provided for @localizations.
  ///
  /// In en, this message translates to:
  /// **'Translations'**
  String get localizations;

  /// No description provided for @details.
  ///
  /// In en, this message translates to:
  /// **'Details'**
  String get details;

  /// No description provided for @episodeDetails.
  ///
  /// In en, this message translates to:
  /// **'Episode details'**
  String get episodeDetails;

  /// No description provided for @identifier.
  ///
  /// In en, this message translates to:
  /// **'Identifier'**
  String get identifier;

  /// No description provided for @animeIdentifier.
  ///
  /// In en, this message translates to:
  /// **'Anime identifier'**
  String get animeIdentifier;

  /// No description provided for @normalizedTitle.
  ///
  /// In en, this message translates to:
  /// **'Normalized title'**
  String get normalizedTitle;

  /// No description provided for @createdAt.
  ///
  /// In en, this message translates to:
  /// **'Created'**
  String get createdAt;

  /// No description provided for @updatedAt.
  ///
  /// In en, this message translates to:
  /// **'Updated'**
  String get updatedAt;

  /// No description provided for @artwork.
  ///
  /// In en, this message translates to:
  /// **'Artwork'**
  String get artwork;

  /// No description provided for @noArtwork.
  ///
  /// In en, this message translates to:
  /// **'No artwork'**
  String get noArtwork;

  /// No description provided for @artworkNotDisplayed.
  ///
  /// In en, this message translates to:
  /// **'Artwork URLs are listed but not displayed: the image host does not allow requests from this origin (CORS).'**
  String get artworkNotDisplayed;

  /// No description provided for @releaseDate.
  ///
  /// In en, this message translates to:
  /// **'Release date'**
  String get releaseDate;

  /// No description provided for @duration.
  ///
  /// In en, this message translates to:
  /// **'Duration'**
  String get duration;

  /// No description provided for @season.
  ///
  /// In en, this message translates to:
  /// **'Season'**
  String get season;

  /// No description provided for @number.
  ///
  /// In en, this message translates to:
  /// **'Number'**
  String get number;

  /// No description provided for @episodeType.
  ///
  /// In en, this message translates to:
  /// **'Type'**
  String get episodeType;

  /// No description provided for @audioLocale.
  ///
  /// In en, this message translates to:
  /// **'Audio'**
  String get audioLocale;

  /// No description provided for @uncensored.
  ///
  /// In en, this message translates to:
  /// **'Uncensored'**
  String get uncensored;

  /// No description provided for @platformId.
  ///
  /// In en, this message translates to:
  /// **'Platform identifier'**
  String get platformId;

  /// No description provided for @url.
  ///
  /// In en, this message translates to:
  /// **'URL'**
  String get url;

  /// No description provided for @language.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get language;

  /// No description provided for @title.
  ///
  /// In en, this message translates to:
  /// **'Title'**
  String get title;

  /// No description provided for @description.
  ///
  /// In en, this message translates to:
  /// **'Description'**
  String get description;

  /// No description provided for @noDescription.
  ///
  /// In en, this message translates to:
  /// **'No description'**
  String get noDescription;

  /// No description provided for @page.
  ///
  /// In en, this message translates to:
  /// **'Page'**
  String get page;

  /// No description provided for @ofTotal.
  ///
  /// In en, this message translates to:
  /// **'of {total}'**
  String ofTotal(int total);

  /// No description provided for @goToPage.
  ///
  /// In en, this message translates to:
  /// **'Go to page'**
  String get goToPage;

  /// No description provided for @firstPage.
  ///
  /// In en, this message translates to:
  /// **'First page'**
  String get firstPage;

  /// No description provided for @previousPage.
  ///
  /// In en, this message translates to:
  /// **'Previous page'**
  String get previousPage;

  /// No description provided for @nextPage.
  ///
  /// In en, this message translates to:
  /// **'Next page'**
  String get nextPage;

  /// No description provided for @lastPage.
  ///
  /// In en, this message translates to:
  /// **'Last page'**
  String get lastPage;

  /// No description provided for @episodeIdentity.
  ///
  /// In en, this message translates to:
  /// **'Season {season} • {type} {number}'**
  String episodeIdentity(int season, String type, int number);

  /// No description provided for @durationMinutes.
  ///
  /// In en, this message translates to:
  /// **'{minutes} min'**
  String durationMinutes(int minutes);

  /// No description provided for @animeCount.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =0{No anime} =1{1 anime} other{{count} animes}}'**
  String animeCount(int count);

  /// No description provided for @episodeCount.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =0{No episode} =1{1 episode} other{{count} episodes}}'**
  String episodeCount(int count);

  /// No description provided for @releaseCount.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =0{No release} =1{1 release} other{{count} releases}}'**
  String releaseCount(int count);

  /// No description provided for @localizationCount.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =0{No translation} =1{1 translation} other{{count} translations}}'**
  String localizationCount(int count);

  /// No description provided for @settings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settings;

  /// No description provided for @requestFormat.
  ///
  /// In en, this message translates to:
  /// **'Request format'**
  String get requestFormat;

  /// No description provided for @requestFormatDescription.
  ///
  /// In en, this message translates to:
  /// **'The format used by every administration API request. JSON is the default; Protobuf and CBOR send less data over the network.'**
  String get requestFormatDescription;

  /// No description provided for @formatJson.
  ///
  /// In en, this message translates to:
  /// **'JSON'**
  String get formatJson;

  /// No description provided for @formatProtobuf.
  ///
  /// In en, this message translates to:
  /// **'Protobuf'**
  String get formatProtobuf;

  /// No description provided for @formatCbor.
  ///
  /// In en, this message translates to:
  /// **'CBOR'**
  String get formatCbor;

  /// No description provided for @formatJsonDescription.
  ///
  /// In en, this message translates to:
  /// **'Readable text. The default, and the easiest format to inspect.'**
  String get formatJsonDescription;

  /// No description provided for @formatProtobufDescription.
  ///
  /// In en, this message translates to:
  /// **'Compact binary format. The smallest payload of the three.'**
  String get formatProtobufDescription;

  /// No description provided for @formatCborDescription.
  ///
  /// In en, this message translates to:
  /// **'Compact binary format. Smaller than JSON and self-describing.'**
  String get formatCborDescription;

  /// No description provided for @formatAppliedOnNextRequest.
  ///
  /// In en, this message translates to:
  /// **'The format applies to the next request. Refresh the list to reload the current data with it.'**
  String get formatAppliedOnNextRequest;

  /// No description provided for @appShortTitle.
  ///
  /// In en, this message translates to:
  /// **'Shikkanime'**
  String get appShortTitle;

  /// No description provided for @navigationAnimes.
  ///
  /// In en, this message translates to:
  /// **'Animes'**
  String get navigationAnimes;

  /// No description provided for @navigationEpisodes.
  ///
  /// In en, this message translates to:
  /// **'Episodes'**
  String get navigationEpisodes;

  /// No description provided for @noEpisodeStored.
  ///
  /// In en, this message translates to:
  /// **'No episode stored'**
  String get noEpisodeStored;

  /// No description provided for @episodeTypeName.
  ///
  /// In en, this message translates to:
  /// **'{value, select, EPISODE{Episode} FILM{Film} SPECIAL{Special} SUMMARY{Recap} SPIN_OFF{Spin-off} other{{value}}}'**
  String episodeTypeName(String value);

  /// No description provided for @platformName.
  ///
  /// In en, this message translates to:
  /// **'{value, select, ANIMATION_DIGITAL_NETWORK{ADN} CRUNCHYROLL{Crunchyroll} DISNEY_PLUS{Disney+} NETFLIX{Netflix} PRIME_VIDEO{Prime Video} other{{value}}}'**
  String platformName(String value);

  /// No description provided for @imageTypeName.
  ///
  /// In en, this message translates to:
  /// **'{value, select, BANNER{Banner} THUMBNAIL{Thumbnail} TITLE{Title} other{{value}}}'**
  String imageTypeName(String value);
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
