# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-08-25

### Fixed

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

[1.0.1]: https://github.com/muzso/android_system_dumper/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/muzso/android_system_dumper/releases/tag/1.0.0
