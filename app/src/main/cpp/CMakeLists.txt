cmake_minimum_required(VERSION 3.22.1)
project(mk64android LANGUAGES C CXX)

add_library(
    mk64android
    SHARED
    native_bridge.cpp
    crash_handler.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_compile_features(mk64android PRIVATE cxx_std_20)

target_link_libraries(
    mk64android
    ${log-lib}
    ${android-lib}
)
