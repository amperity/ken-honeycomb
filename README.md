ken-honeycomb
=============

[![CircleCI](https://circleci.com/gh/amperity/ken-honeycomb.svg?style=shield&circle-token=...)](https://circleci.com/gh/amperity/ken-honeycomb)
[![codecov](https://codecov.io/gh/amperity/ken-honeycomb/branch/main/graph/badge.svg)](https://codecov.io/gh/amperity/ken-honeycomb)
[![cljdoc badge](https://cljdoc.org/badge/com.amperity/ken-honeycomb)](https://cljdoc.org/d/com.amperity/ken-honeycomb/CURRENT)

Observability integration between [ken](https://github.com/amperity/ken) and
[honeycomb.io](https://www.honeycomb.io/).


## Usage

Releases are published on Clojars; to use the latest version with Leiningen,
add the following to your project dependencies:

[![Clojars Project](http://clojars.org/com.amperity/ken-honeycomb/latest-version.svg)](http://clojars.org/com.amperity/ken-honeycomb)

To report Ken events to Honeycomb, you'll need to configure a **Dataset** and a
**WriteKey** in your account. Construct a `HoneyObserver`
component and provide the two
values:

```clojure
(require
  '[com.stuartsierra.component :as component]
  '[ken.honeycomb :as hc])

(def hc-observer
  (component/start
    (hc/honey-observer dataset write-key)))
```

In practice, this observer would probably be part of a larger
[component system](https://github.com/stuartsierra/component), but you can also
set up a global singleton like this since it has no dependencies.

Once started, the observer subscribes a listener to the
[ken tap](https://github.com/amperity/ken#subscriptions) which will send all
the events it receives to the specified Honeycomb dataset.

### Event Processing

By default, the listener sends all event fields provided, after some formatting
to make them compatible with Honeycomb's supported types. You can provide some
custom preprocessing logic by setting the `:transform` key to a function which
accepts the event data and returns an updated map of data to send.

One use of this is to set `rename-default-fields` to map ken's internal keys
to match the default Honeycomb schema:

```clojure
(hc/honey-observer
  dataset write-key
  :transform hc/rename-default-fields)
```

If the function returns nil or an empty map, the event will be discarded, so
this can also be used to filter out events.


### Response Hooks

If you need to respond to various kinds of results of sending an event, you can
initialize the component with a `:response-observer` map, containing any of the
following keys:

- `:on-client-rejected`
- `:on-server-accepted`
- `:on-server-rejected`
- `:on-unknown`

These should each provide a function which is called with the relevant event.


## License

Copyright © 2021 Amperity, Inc.

Distributed under the MIT License.
