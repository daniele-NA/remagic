cmake_minimum_required(VERSION 3.22.1)
project(remagic LANGUAGES C CXX)
add_subdirectory(${FIREBASE_CPP_SDK_DIR} bin/ EXCLUDE_FROM_ALL)

set(CMAKE_C_STANDARD 17)
set(CMAKE_CXX_STANDARD 17)

set(firebase_libs
        firebase_auth
        firebase_database
        firebase_app
)

add_library(remagic SHARED

        jni/jni_sensors.c
        jni/jni_auth.cpp
        jni/jni_llm.c

        motion/util/utils.c

        firebase/firebase_wrapper.cpp

        api/regolo.c
        api/arcfour/arcfour.c
)

find_library(android-log-lib log)
find_library(android-lib android)

# libcurl
find_library(zlib-lib z)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/libcurl/include)
set(curl-lib ${CMAKE_CURRENT_SOURCE_DIR}/libcurl/${ANDROID_ABI}/libcurl.a)

# PASS 'local.properties' VARIABLES #
add_definitions(-DARCFOUR_SECRET_KEY=\"${ARCFOUR_SECRET_KEY}\")
add_definitions(-DREGOLO_API_KEY=\"${REGOLO_API_KEY}\")


target_link_libraries(remagic
        ${android-log-lib}
        ${android-lib}
        ${firebase_libs}

        ${curl-lib}
        ${zlib-lib}
)