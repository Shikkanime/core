// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Shikkanime — Administration';

  @override
  String get searchAnime => 'Search a title or an identifier...';

  @override
  String get refresh => 'Refresh';

  @override
  String get retry => 'Retry';

  @override
  String get loading => 'Loading...';

  @override
  String get loadError => 'Unable to load the data';

  @override
  String get noAnime => 'No anime stored';

  @override
  String get noResult => 'No anime matches this search';

  @override
  String get noEpisode => 'No episode stored for this anime';

  @override
  String get episodes => 'Episodes';

  @override
  String get platforms => 'Platforms';

  @override
  String get localizations => 'Translations';

  @override
  String get details => 'Details';

  @override
  String get episodeDetails => 'Episode details';

  @override
  String get identifier => 'Identifier';

  @override
  String get animeIdentifier => 'Anime identifier';

  @override
  String get normalizedTitle => 'Normalized title';

  @override
  String get createdAt => 'Created';

  @override
  String get updatedAt => 'Updated';

  @override
  String get artwork => 'Artwork';

  @override
  String get noArtwork => 'No artwork';

  @override
  String get artworkNotDisplayed =>
      'Artwork URLs are listed but not displayed: the image host does not allow requests from this origin (CORS).';

  @override
  String get releaseDate => 'Release date';

  @override
  String get duration => 'Duration';

  @override
  String get season => 'Season';

  @override
  String get number => 'Number';

  @override
  String get episodeType => 'Type';

  @override
  String get audioLocale => 'Audio';

  @override
  String get uncensored => 'Uncensored';

  @override
  String get platformId => 'Platform identifier';

  @override
  String get url => 'URL';

  @override
  String get language => 'Language';

  @override
  String get title => 'Title';

  @override
  String get description => 'Description';

  @override
  String get noDescription => 'No description';

  @override
  String get page => 'Page';

  @override
  String ofTotal(int total) {
    return 'of $total';
  }

  @override
  String get goToPage => 'Go to page';

  @override
  String get firstPage => 'First page';

  @override
  String get previousPage => 'Previous page';

  @override
  String get nextPage => 'Next page';

  @override
  String get lastPage => 'Last page';

  @override
  String episodeIdentity(int season, String type, int number) {
    return 'Season $season • $type $number';
  }

  @override
  String durationMinutes(int minutes) {
    return '$minutes min';
  }

  @override
  String animeCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count animes',
      one: '1 anime',
      zero: 'No anime',
    );
    return '$_temp0';
  }

  @override
  String episodeCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count episodes',
      one: '1 episode',
      zero: 'No episode',
    );
    return '$_temp0';
  }

  @override
  String releaseCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count releases',
      one: '1 release',
      zero: 'No release',
    );
    return '$_temp0';
  }

  @override
  String localizationCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count translations',
      one: '1 translation',
      zero: 'No translation',
    );
    return '$_temp0';
  }

  @override
  String get settings => 'Settings';

  @override
  String get requestFormat => 'Request format';

  @override
  String get requestFormatDescription =>
      'The format used by every administration API request. JSON is the default; Protobuf and CBOR send less data over the network.';

  @override
  String get formatJson => 'JSON';

  @override
  String get formatProtobuf => 'Protobuf';

  @override
  String get formatCbor => 'CBOR';

  @override
  String get formatJsonDescription =>
      'Readable text. The default, and the easiest format to inspect.';

  @override
  String get formatProtobufDescription =>
      'Compact binary format. The smallest payload of the three.';

  @override
  String get formatCborDescription =>
      'Compact binary format. Smaller than JSON and self-describing.';

  @override
  String get formatAppliedOnNextRequest =>
      'The format applies to the next request. Refresh the list to reload the current data with it.';

  @override
  String get appShortTitle => 'Shikkanime';

  @override
  String get navigationAnimes => 'Animes';

  @override
  String get navigationEpisodes => 'Episodes';

  @override
  String get noEpisodeStored => 'No episode stored';

  @override
  String episodeTypeName(String value) {
    String _temp0 = intl.Intl.selectLogic(value, {
      'EPISODE': 'Episode',
      'FILM': 'Film',
      'SPECIAL': 'Special',
      'SUMMARY': 'Recap',
      'SPIN_OFF': 'Spin-off',
      'other': '$value',
    });
    return '$_temp0';
  }

  @override
  String platformName(String value) {
    String _temp0 = intl.Intl.selectLogic(value, {
      'ANIMATION_DIGITAL_NETWORK': 'ADN',
      'CRUNCHYROLL': 'Crunchyroll',
      'DISNEY_PLUS': 'Disney+',
      'NETFLIX': 'Netflix',
      'PRIME_VIDEO': 'Prime Video',
      'other': '$value',
    });
    return '$_temp0';
  }

  @override
  String imageTypeName(String value) {
    String _temp0 = intl.Intl.selectLogic(value, {
      'BANNER': 'Banner',
      'THUMBNAIL': 'Thumbnail',
      'TITLE': 'Title',
      'other': '$value',
    });
    return '$_temp0';
  }
}
