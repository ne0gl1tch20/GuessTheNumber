cmake_minimum_required(VERSION 3.22.1)

project("gtn")

# Native game-data decryption uses a pinned, maintained AES-GCM implementation.
# Mbed TLS 3.6 is an LTS branch; update this pin before its support window ends.
include(FetchContent)
set(ENABLE_PROGRAMS OFF CACHE BOOL "" FORCE)
set(ENABLE_TESTING OFF CACHE BOOL "" FORCE)
set(USE_STATIC_MBEDTLS_LIBRARY ON CACHE BOOL "" FORCE)
set(USE_SHARED_MBEDTLS_LIBRARY OFF CACHE BOOL "" FORCE)
set(MBEDTLS_FATAL_WARNINGS OFF CACHE BOOL "" FORCE)
set(GEN_FILES OFF CACHE BOOL "" FORCE)
FetchContent_Declare(
    mbedtls
    GIT_REPOSITORY https://github.com/Mbed-TLS/mbedtls.git
    GIT_TAG mbedtls-3.6.7
    GIT_SHALLOW TRUE
    GIT_SUBMODULES_RECURSE TRUE
)
FetchContent_MakeAvailable(mbedtls)

add_library(
    gtn
    SHARED
    gtn_native.cpp
)

target_compile_features(gtn PRIVATE cxx_std_20)
target_compile_options(gtn PRIVATE -Wall -Wextra -Werror -fvisibility=hidden)

find_library(log-lib log)
target_link_libraries(gtn PRIVATE mbedcrypto ${log-lib})

# Reusable procedural audio engine. The stream is opened lazily by the first play request.
add_library(
    jarrlyyydsp
    SHARED
    jarrlyy_dsp_engine.cpp
    jarrlyy_dsp_jni.cpp
)
target_compile_features(jarrlyyydsp PRIVATE cxx_std_20)
target_compile_options(jarrlyyydsp PRIVATE -Wall -Wextra -Werror -fvisibility=hidden)
find_library(aaudio-lib aaudio)
if(NOT aaudio-lib)
    message(FATAL_ERROR "Android NDK AAudio library is required for jarrlyyydsp")
endif()
target_link_libraries(jarrlyyydsp PRIVATE ${aaudio-lib} ${log-lib})
