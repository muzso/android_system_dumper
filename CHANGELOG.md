# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.4] - 2026-09-02

### Changed

- The "Reset" button in the fatal error dialog was renamed to "Close".
- The "Fatal Error" dialog has been renamed to just "Error" and the message was renamed from "Upload crashed" to "Upload failed".

### Fixed

- Fatal errors during upload do not reset the filesystem scan results.
- Stopping the upload during Tor checks doesn't throw a fatal error with a "StandaloneCoroutine was canceled" message anymore.

## [1.1.3] - 2026-09-01

### Added

- New input field `Retry limit`. Controls the maximum number of retry attempts for file uploads and Tor check requests.

### Changed

- UI defaults got centralized. Previously all default values were stored in two places.

### Fixed

- `customBatchSize` was String in `UploadParameters`, but other numeric fields were already Int.

## [1.1.2] - 2026-08-31

### Added

- Introduced the NETWORK_TIMEOUT_MS configuration parameter for .env files. Controls HTTP connect, read and write timeouts with a 30s default.

### Changed

- `strings.xml` was consolidated: a number of similar strings were merged together or replaced by a template and other shorter strings.
- The five "List of ..." toggles on the main screen were merged into a single "File lists" toggle.
- The "TOR" icon in the top-right corner of the main screen has been renamed back to "IP", because it's actually independent of the "Use Tor network" toggle. If the latter is disabled, then the IP Information screen shows GeoIP info on the real IP of the device.

### Fixed

- A number of string literals were moved into `strings.xml`.
- There was a bug in both the upload and download progress indicators (progress bar and progress texts): the progress indicators were driven by written bytes of network connections, thus when the upload of a file finished, its last state of progress remained visible even while the next file was being prepared (e.g. next ZIP was being created). These are now fixed.
- Previously if an HTTP call timed out or failed and Tor was used, the Tor circuits were rebuilt and the HTTP call retried. Testing on real devices showed that this doesn't always solve the problem and the retries fail too. This was now changed to restart the entire Tor service before a retry is attempted.

## [1.1.1] - 2026-08-30

### Added

- A number of the app's configuration parameters (useful for testing and development) can now be defined in the `local.properties` or the `.env` file.
- The readme now links to two YouTube videos that demonstrate both file transfer methods.
- The IpInfoScreen didn't have a screenshot test, until now.

### Changes

- The "(ZIP) password" expression was changed to "(ZIP) passphrase" throughout the app.
- The "IP" icon in the top-right corner of the main screen has been renamed to "TOR", because it doesn't give information about the app's external IP address since Tor connects with a different exit node to every hostname.

### Removed

- The "Is Tor Node" property has been removed from the Tor Checker screen's (JSON) content, because it didn't actually apply to the given GeoIP service's connection. It merely showed whether the Tor checker endpoint was reached through a Tor exit node.

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

[1.1.4]: https://github.com/muzso/android_system_dumper/compare/1.1.3...1.1.4
[1.1.3]: https://github.com/muzso/android_system_dumper/compare/1.1.2...1.1.3
[1.1.2]: https://github.com/muzso/android_system_dumper/compare/1.1.1...1.1.2
[1.1.1]: https://github.com/muzso/android_system_dumper/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/muzso/android_system_dumper/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/muzso/android_system_dumper/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/muzso/android_system_dumper/releases/tag/1.0.0
