cmake_minimum_required(VERSION 3.22.1)
project(mk64android LANGUAGES C CXX)

add_library(
    mk64android
    SHARED
    native_bridge.cpp
    crash_handler.cpp
    graphics_probe.cpp
)

find_library(log-lib log)
find_library(android-lib android)
find_library(vulkan-lib vulkan)

target_compile_features(mk64android PRIVATE cxx_std_20)

target_link_libraries(
    mk64android
    ${log-lib}
    ${android-lib}
    ${vulkan-lib}
)
