cmake_minimum_required(VERSION 3.18.1)
project("scanner_jni")

add_library(
    scanner_jni
    SHARED
    scanner_jni.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    scanner_jni
    ${log-lib}
)
