cmake_minimum_required(VERSION 3.22.1)

project("OpacityCore")

# Build your wrapper library with a different name
add_library(OpacityCoreWrapper SHARED
        OpacityCore.cpp)

# Include headers
target_include_directories(OpacityCoreWrapper PUBLIC
        ${CMAKE_SOURCE_DIR}/../jni/include)

# Set compile definitions and flags
target_compile_definitions(OpacityCoreWrapper PRIVATE
        -DANDROID_STL=c++_shared)

# Import the SDK library
add_library(sdk SHARED IMPORTED)
set_target_properties(sdk PROPERTIES
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libOpacityCore.so
        IMPORTED_NO_SONAME ON)

# Link libraries - ensure symbols are properly exported
target_link_libraries(OpacityCoreWrapper
        sdk
        android
        log
        c++_shared)  # explicitly link the shared C++ runtime

# Export symbols properly
set_target_properties(OpacityCoreWrapper PROPERTIES
        VERSION 1.0.0
        SOVERSION 1)
