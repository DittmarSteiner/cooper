# Cooper

_Cooper_ is a set of utilities, usually useful during an application’s init phase. 
Since all classes have no external dependencies or depend on each other, feel free to simply copy the desired `*.java` files into your project.

#### Version 1.2.0

- Introducing `Resulting`, `Executing` and `Encrypting`
- Now requires Java 11 
- Updated all dependencies and Maven plugins
- JUnit 5 (5.6.2), so all tests in `Config` and `OptionsResolver` had to be commented out.  
**TODO**: solution required"

#### Version 1.1.5

Jackson dependency (test only) updated to 2.9.9

#### Version 1.1.4

Jackson dependency (test olny) updated to 2.9.8

#### Version 1.1.3

Dependency updates, pulgin updates.

#### Version 1.1.2

_API improvement:_ now supporting `new Config.Builder(Properties properties)`.

#### Version 1.1

_API change:_ moved from `.set*(key, value)` to correctly named `put*(key, value)` modifier method in the `ConfigBuilder`.

### Complete example

Also see [Javadoc](https://dittmarsteiner.github.io/cooper/)


```Java
// some defaults
final int DEFAULT_NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors() * 4;
final int DEFAULT_PORT = 7777;

// collect some properties from the environment or commend line
// public static void main(String[] args)
Optional<String> optionalName = OptionsResolver.resolve("name", 'n', args);
int proxyThreads = OptionsResolver.resolve("proxy.threads", 't', args).map(Integer::parseInt)
    .orElse(DEFAULT_NUMBER_OF_THREADS);

// load your config as a Map
Reader reader = Files.newBufferedReader(Paths.get("config.yml");
ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
Map<String, Object> map = mapper.readValue(reader, Map.class);

// adjust the config
Config config = new Config.Builder(map)
    // will add or replace the target, but not nullify
    .putOf("name", optionalName)
    // will add the target, but not overwrite existing
    .putIfEmpty("alias", alias)
    // will add or replace the target; or delete it if proxyThreads is 'null'
    .put("proxy.threads", proxyThreads)
    .build();

// ----------------------------------------------------------------
// now read from your config
// name is empty if not found
Optional<String> name = config.get("name");

// uses the default 'orElse' if not found
String proxyName = config.get("proxy.name").orElse("Dale");

// note: you better know the type
int proxyPort = config.<Integer> get("proxy.port").orElse(9999);

// throws a ClassCastException
int someNumber = config.<Integer>get("name").orElse(42).intValue();
```

---
```
------------------------------------------------------------------------------
ISC License http://opensource.org/licenses/isc-license.txt
------------------------------------------------------------------------------
Copyright (c) 2017, Dittmar Steiner <dittmar.steiner@gmail.com>

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
```