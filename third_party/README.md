# Third-party native crypto dependency

The native game-data archive decryptor links the Mbed TLS crypto library statically.

- Project: Mbed TLS
- Pinned release: `mbedtls-3.6.7`
- Upstream: https://github.com/Mbed-TLS/mbedtls/tree/mbedtls-3.6.7
- License: Apache-2.0 OR GPL-2.0-or-later
- Build: fetched by CMake FetchContent and linked as `mbedcrypto`

The full upstream license text is in [MBEDTLS-LICENSE.txt](MBEDTLS-LICENSE.txt).
The Mbed TLS 3.6 LTS branch is supported until March 2027. Update the CMake pin and this notice together before then.
