# Shikkanime admin (web)

Flutter web interface of the `:admin` module. It reads the stored animes and episodes through the
admin JSON API, and it is served by the same server: the Gradle build packages the compiled app as
the `webapp` resource of the module.

## Run it

The interface is packaged by **Gradle**, not by `flutter build web` alone: `:admin:run` and
`:admin:installDist` compile the web app and copy it into the `webapp` package the server mounts.
Starting the module without that step stops at boot with the command to run, instead of answering a
`404` on `/`.

```bash
# From the repository root, with Flutter in PATH or FLUTTER_BIN pointing at the SDK

# The schema first: the Docker entrypoint provisions it, a local run does not
./gradlew :database:run

# The admin: compiles the interface, packages it, serves it on http://localhost:8081
./gradlew :admin:run

# Or the installed distribution, which contains the same packaged interface
./gradlew :admin:installDist
admin/build/install/admin/bin/admin
```

To reuse an interface compiled elsewhere, point `WEB_APP_DIST` at a directory containing
`index.html`; the Gradle task packages it instead of invoking Flutter.

If the server runs outside the distribution (an IDE run for instance), it serves nothing from the
classpath: point `WEB_APP_DIR` at the compiled directory instead, and the module serves it from disk.
Starting without either fails with the path it looked for, never a bare `404`.

The first build of the web app takes a few minutes (`flutter build web --release`); later runs reuse
the Gradle task output.

## Dev server

The hot-reload server runs beside the admin server and calls its API:

```bash
cd admin/webapp
flutter run -d chrome --dart-define=API_BASE_URL=http://localhost:8081
```

## Request format

The settings button in the catalog selects the wire format of the administration API: JSON
(default), Protobuf or CBOR. The choice is stored in `shared_preferences`; returning to the catalog
reloads the current page with it. Protobuf and CBOR reduce the payload size, and each endpoint has
its schema in `lib/core/network/admin_protobuf_schemas.dart`.

## Check it

```bash
cd admin/webapp
dart format lib
flutter analyze
flutter test
flutter build web --release
```

`flutter analyze` runs in CI (`Admin web app` job) and a finding fails it. The Docker image compiles
the same app in its own stage, so a build that only works on a developer machine is not done.
