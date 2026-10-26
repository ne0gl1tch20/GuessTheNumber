cmake_minimum_required(VERSION 3.22.1)

project("gtn_native")

add_library(
    gtn_native
    SHARED
    gtn_native.cpp
)

target_compile_features(gtn_native PRIVATE cxx_std_20)
target_compile_options(gtn_native PRIVATE -Wall -Wextra -Werror -fvisibility=hidden)

find_library(log-lib log)
target_link_libraries(gtn_native PRIVATE ${log-lib})

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
target_link_libraries(jarrlyyydsp PRIVATE ${aaudio-lib} ${log-lib})
