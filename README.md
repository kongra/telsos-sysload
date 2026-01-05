# telsos-sysload

A Clojure utility library for intelligent namespace dependency resolution and dynamic system reloading in REPL-driven development.

## Features

- **Smart Namespace Loading**: Analyzes source files and resolves namespace dependencies automatically
- **Incremental Reloading**: `synch` only reloads modified namespaces and their dependents
- **Full System Reload**: `boot` performs a complete clean reload of all namespaces
- **Memory Monitoring**: Built-in utilities for tracking JVM memory usage
- **nREPL Integration**: Auto-injects utilities into the `user` namespace
- **Namespace Finalization**: Optional cleanup hooks when namespaces are removed

## Installation

Add to your `deps.edn`:

```clojure
{:deps {com.github.kongra/telsos-sysload {:mvn/version "0.1.X"}}}
```

## Usage

### Basic REPL Workflow

The library provides four main utilities that are automatically available in your REPL when using the nREPL middleware:

#### `boot` - Full System Reload

Performs a complete reload of all namespaces in dependency order:

```clojure
;; Reload all namespaces from default source directories
@boot
;; or
(boot)

;; Reload from specific source directories
(boot ["src/" "test/" "dev/"])
```

#### `synch` - Incremental Reload

Intelligently reloads only modified namespaces and their dependents:

```clojure
;; Reload only changed namespaces
@synch
;; or
(synch)

;; Synch specific source directories
(synch ["src/"])
```

#### `room` - Memory Statistics

Display current JVM memory usage:

```clojure
@room
;; Output: Used: 245.67 MB | Free: 1.23 GB | Total: 1.48 GB | Max: 4.00 GB
```

#### `gc` - Garbage Collection

Trigger garbage collection and display memory stats:

```clojure
@gc
;; Runs System/gc and shows memory statistics
```

### Configuration

Create `resources/telsos-sysload.edn` to configure source directories:

```clojure
{:source-dirs ["src/" "test/" "dev/"]}
```

If not present, defaults to `["src/" "test/"]`.

### nREPL Middleware Setup

Add to your nREPL configuration (e.g., in `deps.edn` alias or `.nrepl.edn`):

```clojure
{:middleware [telsos.sysload/middleware]}
```

Or in your `~/.lein/profiles.clj` for Leiningen:

```clojure
{:user {:repl-options {:nrepl-middleware [telsos.sysload/middleware]}}}
```

This automatically interns `boot`, `synch`, `room`, and `gc` into the `user` namespace on REPL startup.

### Namespace Finalization Hook

Define a `ns-finalize` function in any namespace to perform cleanup when the namespace is removed during reload:

```clojure
(ns my.app.core)

(defn ns-finalize
  "Called before namespace removal during boot/synch"
  []
  (println "Cleaning up my.app.core...")
  ;; Close connections, release resources, etc.
  )
```

## API Reference

### Core Functions

- `(boot)` / `(boot source-dirs)` - Full system reload
- `(synch)` / `(synch source-dirs)` - Incremental reload of modified namespaces
- `(room)` - Display JVM memory statistics
- `(gc)` - Run garbage collection and show memory stats

### State Management

- `(state-atom)` - Returns the internal state atom
- `(loadtime)` - Returns the last system load timestamp (milliseconds)
- `(set-loadtime! msecs)` - Set the load timestamp
- `(set-loadtime-current!)` - Set load timestamp to current time

### Analysis Functions

- `(analyze-system-sources source-dirs)` - Parse sources and build dependency graph
- `(namespace-names-ordered graph ns-names)` - Topologically sort namespaces by dependencies

## How It Works

1. **Dependency Analysis**: Scans source directories, parses namespace declarations, and builds a dependency graph
2. **Topological Sorting**: Orders namespaces so dependencies load before dependents
3. **Intelligent Reloading**:
   - `boot`: Removes all namespaces in reverse order, then loads in correct order
   - `synch`: Compares file modification times, reloads changed files plus all transitive dependents
4. **State Isolation**: Maintains load state in a private namespace to avoid pollution

## Development

### Building

```bash
make clean      # Clean build artifacts
make compile    # AOT compile
make jar        # Create JAR file
make install    # Install to local Maven repo
```

### Testing

```bash
make kaocha     # Run test suite
```

### Linting

```bash
make lint       # Run clj-kondo, kibit, and splint
```

### Deployment

```bash
make deploy-clojars  # Deploy to Clojars (after make jar)
```

## License

Copyright © 2024 Kongra Konrad Grzanek

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
