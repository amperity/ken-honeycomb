Change Log
==========

All notable changes to this project will be documented in this file, which
follows the conventions of [keepachangelog.com](http://keepachangelog.com/).
This project adheres to [Semantic Versioning](http://semver.org/).


## Unreleased

...

## [1.4.1] - 2026-02-10

### Changed
- Update ken to latest version.

## [1.4.0] - 2026-02-10

### Changed
- Update ken to latest version and use the new `:ken.trace/upstream-sampling`
  to set the Honeycomb sample rate.
  [PR#9](https://github.com/amperity/ken-honeycomb/pull/8)


## [1.3.0] - 2024-10-14

### Changed
- Revert "Conditionally set dataset per-event based on the `:service.name`
  property, following the OTEL spec."
  [PR#7](https://github.com/amperity/ken-honeycomb/pull/7)
- Update dependency versions, including Clojure 1.12 and libhoney 1.6.

### Fixed
- Ken does its own down-sampling logic, so events should be sent "presampled"
  to Honeycomb. Previously, the Honeycomb client would _also_ apply the sample
  rate, resulting in unintentional double-sampling.
  [PR#8](https://github.com/amperity/ken-honeycomb/pull/8)


## [1.2.0] - 2023-08-14

### Changed
- Conditionally set dataset per-event based on the `:service.name` property,
  following the OTEL spec.
  [PR#6](https://github.com/amperity/ken-honeycomb/pull/6)


## [1.1.0] - 2023-05-08

### Changed
- Update dependencies to latest stable versions.

### Added
- Added new utility functions for recording span events and links.
  [#4](https://github.com/amperity/ken-honeycomb/issues/4)
  [PR#5](https://github.com/amperity/ken-honeycomb/pull/5)


## [1.0.3] - 2022-06-02

### Changed
- Update dependencies to latest stable versions.


## [1.0.2] - 2022-02-02

### Changed
- Update libhoney-java to 1.4.1 to fix recurring `NoSuchMethodError` on the
  shaded `ConnectionClosedException` class.


## [1.0.1] - 2021-12-27

### Changed
- Update dependencies to latest stable versions.


## [1.0.0] - 2021-06-08

First production release! No changes since `0.1.0`, but this reflects full
internal adoption in Amperity's codebase.


## 0.1.0 - 2021-05-31

Initial open-source project release.


[Unreleased]: https://github.com/amperity/ken/compare/1.4.1...HEAD
[1.4.1]: https://github.com/amperity/ken/compare/1.3.0...1.4.1
[1.4.0]: https://github.com/amperity/ken/compare/1.3.0...1.4.0
[1.3.0]: https://github.com/amperity/ken/compare/1.2.0...1.3.0
[1.2.0]: https://github.com/amperity/ken/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/amperity/ken/compare/1.0.3...1.1.0
[1.0.3]: https://github.com/amperity/ken/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/amperity/ken/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/amperity/ken/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/amperity/ken/compare/0.1.0...1.0.0
