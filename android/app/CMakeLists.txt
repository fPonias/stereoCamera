cmake_minimum_required(VERSION 3.4.1)

add_library( jpeg-lib

            STATIC

            #including libjpeg in our native-lib as I couldn't figure out how to link to it without getting a runtime crash
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
                            src/main/cpp/jpeg-9c/jmemansi.c
                            )

add_library( native-lib

             SHARED

             src/main/cpp/util.cpp
             src/main/cpp/jpegCtrl.cpp
             src/main/cpp/Image.cpp
             src/main/cpp/SplitCompositeImage.cpp
             src/main/cpp/AnaglyphCompositeImage.cpp
             src/main/cpp/RedCyanCompositeImage.cpp
             src/main/cpp/GreenMagentaCompositeImage.cpp
             src/main/cpp/native-lib.cpp
 )



find_library( # Sets the name of the path variable.
              log-lib
              log )

include_directories(src/main/cpp/include
                    src/main/cpp/jpeg-9c
                    src/main/cpp)

target_link_libraries( # Specifies the target library.
                       native-lib
                       jpeg-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
        libc++_static.a )

add_executable(stereoCameraImageProcessor

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
        src/main/cpp/jpeg-9c/jmemansi.c
        src/main/cpp/util.cpp
        src/main/cpp/jpegCtrl.cpp
        src/main/cpp/Image.cpp
        src/main/cpp/SplitCompositeImage.cpp
        src/main/cpp/AnaglyphCompositeImage.cpp
        src/main/cpp/RedCyanCompositeImage.cpp
        src/main/cpp/GreenMagentaCompositeImage.cpp
        src/main/cpp/main.cpp
        )

target_link_libraries(stereoCameraImageProcessor
        libc++_static.a)

#add_custom_command(TARGET stereoCameraImageProcessor POST_BUILD
#        COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_FILE:stereoCameraImageProcessor> D:\\programming\\stereoCamera\\android\\stereoCameraImageProcessor
#)

