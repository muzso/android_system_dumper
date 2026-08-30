# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.1] - 2026-08-30

### Added

- A number of the app's configuration parameters (useful for testing and development) can now be defined in the `local.properties` or the `.env` file.
- New file transfer method: downloads from an HTTP server. It's much faster and doesn't involve any third-parties, just two devices on the same LAN (e.g. Wi-Fi).
- The readme now links to two YouTube videos that demonstrate both file transfer methods.

### Changes

- The "(ZIP) password" expression was changed to "(ZIP) passphrase" throughout the app.
- The "IP" icon in the top-right corner of the main screen has been renamed to "TOR", because it doesn't give information about the app's external IP address since Tor connects with a different exit node to every hostname.

### Removed

- The "Is Tor Node" property has been removed from the Tor Checker screen's (JSON) content, because it didn't actually apply to the given GeoIP service's connection, it merely showed whether the Tor checker endpoint was reached through a Tor exit node.

## [1.1.0] - 2026-08-29

### Added

- New file transfer method: downloads from an HTTP server. It's much faster and doesn't involve any third-parties, just two devices on the same LAN (e.g. Wi-Fi).
- Added new packaging option: double-zipping.

### Changed

- The main screen's UI layout has been reworked. The workflow steps are now cleaner: step#1 filesystem scan, step#2 packaging, step#3 file transfer, step#4 finished.
- Help screen got updated and extended.

### Fixed

- The QR code screenshot test now actually shows a QR code.

## [1.0.1] - 2026-08-25

### Fixed

- .gitignore was buggy: the `gradle` directory and `gradlew*` scripts must be in the repo
- DefaultGofileGateway swallowed exceptions while writing to the network connection
- removed reference to the old "Dummy" file sharing service from the help description (that feature was scrapped before the first release)
- Kover quality gates for `mobile` and `automotive` modules sometimes caused the `check` task to fail (seemingly randomly), removed them until we get some code in either of these modules that actually has to be tested

### Changed

- minor modifications of readme and strings.xml texts
- default batch size is now 200 MB, because 500 MB caused upload failures on some networks (in-vehicle mobile connections)
- default batch size is now the same for both `debug` and `release` build variants
- ZIP upload retry logic now rebuilds Tor connection between each retry (and there's no +1 retry anymore beyond the initial 3)
- after a Tor connection rebuild a Tor check is done to see whether requests go through Tor indeed; if the check fails, upload is aborted
- Tor check requests use a retry logic as well (the limit is the same as for ZIP uploads)
- TorChecker is now an interface

## [1.0.0] - 2026-08-24

### Added

- Initial release (feature complete and mostly stable)

[1.1.1]: https://github.com/muzso/android_system_dumper/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/muzso/android_system_dumper/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/muzso/android_system_dumper/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/muzso/android_system_dumper/releases/tag/1.0.0
