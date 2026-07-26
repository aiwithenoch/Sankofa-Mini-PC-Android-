cmake_minimum_required(VERSION 3.22.1)
project(sankofa_runtime LANGUAGES C CXX)

add_library(
    sankofa_runtime
    SHARED
    sankofa_runtime.cpp
)

target_compile_features(sankofa_runtime PRIVATE cxx_std_17)

target_compile_options(
    sankofa_runtime
    PRIVATE
    -O3
    -fvisibility=hidden
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    sankofa_runtime
    ${log-lib}
    ${android-lib}
)

# Colibri integration point:
# 1. Fetch a pinned, audited Colibri revision into a separate source directory.
# 2. Add its C sources here as a static target.
# 3. Link that target into sankofa_runtime.
# 4. Keep the JNI contract stable so the Kotlin app does not depend on Colibri internals.
