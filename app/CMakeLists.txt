# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( native-lib

             SHARED

             src/main/cpp/util.cpp
             src/main/cpp/jpegCtrl.cpp
             src/main/cpp/Image.cpp
             src/main/cpp/CompositeImage.cpp
             src/main/cpp/native-lib.cpp
                src/main/cpp/jpeg-9c/jaricom.c
                src/main/cpp/jpeg-9c/jcapimin.c
                src/main/cpp/jpeg-9c/jcapistd.c
                src/main/cpp/jpeg-9c/jcarith.c
                src/main/cpp/jpeg-9c/jccoefct.c
                src/main/cpp/jpeg-9c/jccolor.c
                src/main/cpp/jpeg-9c/jcdctmgr.c
                src/main/cpp/jpeg-9c/jchuff.c
                src/main/cpp/jpeg-9c/jcinit.c
                src/main/cpp/jpeg-9c/jcmainct.c
                src/main/cpp/jpeg-9c/jcmarker.c
                src/main/cpp/jpeg-9c/jcmaster.c
                src/main/cpp/jpeg-9c/jcomapi.c
                src/main/cpp/jpeg-9c/jcparam.c
                src/main/cpp/jpeg-9c/jcprepct.c
                src/main/cpp/jpeg-9c/jcsample.c
                src/main/cpp/jpeg-9c/jctrans.c
                src/main/cpp/jpeg-9c/jdapimin.c
                src/main/cpp/jpeg-9c/jdapistd.c
                src/main/cpp/jpeg-9c/jdarith.c
                src/main/cpp/jpeg-9c/jdatadst.c
                src/main/cpp/jpeg-9c/jdatasrc.c
                src/main/cpp/jpeg-9c/jdcoefct.c
                src/main/cpp/jpeg-9c/jdcolor.c
                src/main/cpp/jpeg-9c/jddctmgr.c
                src/main/cpp/jpeg-9c/jdhuff.c
                src/main/cpp/jpeg-9c/jdinput.c
                src/main/cpp/jpeg-9c/jdmainct.c
                src/main/cpp/jpeg-9c/jdmarker.c
                src/main/cpp/jpeg-9c/jdmaster.c
                src/main/cpp/jpeg-9c/jdmerge.c
                src/main/cpp/jpeg-9c/jdpostct.c
                src/main/cpp/jpeg-9c/jdsample.c
                src/main/cpp/jpeg-9c/jdtrans.c
                src/main/cpp/jpeg-9c/jerror.c
                src/main/cpp/jpeg-9c/jfdctflt.c
                src/main/cpp/jpeg-9c/jfdctfst.c
                src/main/cpp/jpeg-9c/jfdctint.c
                src/main/cpp/jpeg-9c/jidctflt.c
                src/main/cpp/jpeg-9c/jidctfst.c
                src/main/cpp/jpeg-9c/jidctint.c
                src/main/cpp/jpeg-9c/jquant1.c
                src/main/cpp/jpeg-9c/jquant2.c
                src/main/cpp/jpeg-9c/jutils.c
                src/main/cpp/jpeg-9c/jmemmgr.c
                src/main/cpp/jpeg-9c/jmemname.c )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )


