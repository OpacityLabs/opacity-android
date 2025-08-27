cmake_minimum_required(VERSION 3.31.5)

project("DemoCpp")

add_library(
    ${CMAKE_PROJECT_NAME}
    SHARED
    native-lib.cpp
)


target_link_libraries(
    ${CMAKE_PROJECT_NAME}
)