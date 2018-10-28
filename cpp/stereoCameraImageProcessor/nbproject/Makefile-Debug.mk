#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=arm-none-eabi-gcc
CCC=arm-none-eabi-g++
CXX=arm-none-eabi-g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=arm-MacOSX
CND_DLIB_EXT=dylib
CND_CONF=Debug
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/main.o \
	${OBJECTDIR}/src/AnaglyphCompositeImage.o \
	${OBJECTDIR}/src/GreenMagentaCompositeImage.o \
	${OBJECTDIR}/src/Image.o \
	${OBJECTDIR}/src/RedCyanCompositeImage.o \
	${OBJECTDIR}/src/SplitCompositeImage.o \
	${OBJECTDIR}/src/jpeg-9c/cdjpeg.o \
	${OBJECTDIR}/src/jpeg-9c/jaricom.o \
	${OBJECTDIR}/src/jpeg-9c/jcapimin.o \
	${OBJECTDIR}/src/jpeg-9c/jcapistd.o \
	${OBJECTDIR}/src/jpeg-9c/jcarith.o \
	${OBJECTDIR}/src/jpeg-9c/jccoefct.o \
	${OBJECTDIR}/src/jpeg-9c/jccolor.o \
	${OBJECTDIR}/src/jpeg-9c/jcdctmgr.o \
	${OBJECTDIR}/src/jpeg-9c/jchuff.o \
	${OBJECTDIR}/src/jpeg-9c/jcinit.o \
	${OBJECTDIR}/src/jpeg-9c/jcmainct.o \
	${OBJECTDIR}/src/jpeg-9c/jcmarker.o \
	${OBJECTDIR}/src/jpeg-9c/jcmaster.o \
	${OBJECTDIR}/src/jpeg-9c/jcomapi.o \
	${OBJECTDIR}/src/jpeg-9c/jcparam.o \
	${OBJECTDIR}/src/jpeg-9c/jcprepct.o \
	${OBJECTDIR}/src/jpeg-9c/jcsample.o \
	${OBJECTDIR}/src/jpeg-9c/jctrans.o \
	${OBJECTDIR}/src/jpeg-9c/jdapimin.o \
	${OBJECTDIR}/src/jpeg-9c/jdapistd.o \
	${OBJECTDIR}/src/jpeg-9c/jdarith.o \
	${OBJECTDIR}/src/jpeg-9c/jdatadst.o \
	${OBJECTDIR}/src/jpeg-9c/jdatasrc.o \
	${OBJECTDIR}/src/jpeg-9c/jdcoefct.o \
	${OBJECTDIR}/src/jpeg-9c/jdcolor.o \
	${OBJECTDIR}/src/jpeg-9c/jddctmgr.o \
	${OBJECTDIR}/src/jpeg-9c/jdhuff.o \
	${OBJECTDIR}/src/jpeg-9c/jdinput.o \
	${OBJECTDIR}/src/jpeg-9c/jdmainct.o \
	${OBJECTDIR}/src/jpeg-9c/jdmarker.o \
	${OBJECTDIR}/src/jpeg-9c/jdmaster.o \
	${OBJECTDIR}/src/jpeg-9c/jdmerge.o \
	${OBJECTDIR}/src/jpeg-9c/jdpostct.o \
	${OBJECTDIR}/src/jpeg-9c/jdsample.o \
	${OBJECTDIR}/src/jpeg-9c/jdtrans.o \
	${OBJECTDIR}/src/jpeg-9c/jerror.o \
	${OBJECTDIR}/src/jpeg-9c/jfdctflt.o \
	${OBJECTDIR}/src/jpeg-9c/jfdctfst.o \
	${OBJECTDIR}/src/jpeg-9c/jfdctint.o \
	${OBJECTDIR}/src/jpeg-9c/jidctflt.o \
	${OBJECTDIR}/src/jpeg-9c/jidctfst.o \
	${OBJECTDIR}/src/jpeg-9c/jidctint.o \
	${OBJECTDIR}/src/jpeg-9c/jmemansi.o \
	${OBJECTDIR}/src/jpeg-9c/jmemmgr.o \
	${OBJECTDIR}/src/jpeg-9c/jquant1.o \
	${OBJECTDIR}/src/jpeg-9c/jquant2.o \
	${OBJECTDIR}/src/jpeg-9c/jutils.o \
	${OBJECTDIR}/src/jpeg-9c/rdbmp.o \
	${OBJECTDIR}/src/jpeg-9c/rdcolmap.o \
	${OBJECTDIR}/src/jpeg-9c/rdgif.o \
	${OBJECTDIR}/src/jpeg-9c/rdppm.o \
	${OBJECTDIR}/src/jpeg-9c/rdrle.o \
	${OBJECTDIR}/src/jpeg-9c/rdswitch.o \
	${OBJECTDIR}/src/jpeg-9c/rdtarga.o \
	${OBJECTDIR}/src/jpeg-9c/transupp.o \
	${OBJECTDIR}/src/jpeg-9c/wrbmp.o \
	${OBJECTDIR}/src/jpeg-9c/wrgif.o \
	${OBJECTDIR}/src/jpeg-9c/wrppm.o \
	${OBJECTDIR}/src/jpeg-9c/wrrle.o \
	${OBJECTDIR}/src/jpeg-9c/wrtarga.o \
	${OBJECTDIR}/src/jpegCtrl.o \
	${OBJECTDIR}/src/util.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/stereocameraimageprocessor

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/stereocameraimageprocessor: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.cc} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/stereocameraimageprocessor ${OBJECTFILES} ${LDLIBSOPTIONS}

${OBJECTDIR}/main.o: main.cpp
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/main.o main.cpp

${OBJECTDIR}/src/AnaglyphCompositeImage.o: src/AnaglyphCompositeImage.cpp
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/AnaglyphCompositeImage.o src/AnaglyphCompositeImage.cpp

${OBJECTDIR}/src/GreenMagentaCompositeImage.o: src/GreenMagentaCompositeImage.cpp
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/GreenMagentaCompositeImage.o src/GreenMagentaCompositeImage.cpp

${OBJECTDIR}/src/Image.o: src/Image.cpp
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/Image.o src/Image.cpp

${OBJECTDIR}/src/RedCyanCompositeImage.o: src/RedCyanCompositeImage.cpp
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/RedCyanCompositeImage.o src/RedCyanCompositeImage.cpp

${OBJECTDIR}/src/SplitCompositeImage.o: src/SplitCompositeImage.cpp
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/SplitCompositeImage.o src/SplitCompositeImage.cpp

${OBJECTDIR}/src/jpeg-9c/cdjpeg.o: src/jpeg-9c/cdjpeg.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/cdjpeg.o src/jpeg-9c/cdjpeg.c

${OBJECTDIR}/src/jpeg-9c/jaricom.o: src/jpeg-9c/jaricom.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jaricom.o src/jpeg-9c/jaricom.c

${OBJECTDIR}/src/jpeg-9c/jcapimin.o: src/jpeg-9c/jcapimin.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcapimin.o src/jpeg-9c/jcapimin.c

${OBJECTDIR}/src/jpeg-9c/jcapistd.o: src/jpeg-9c/jcapistd.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcapistd.o src/jpeg-9c/jcapistd.c

${OBJECTDIR}/src/jpeg-9c/jcarith.o: src/jpeg-9c/jcarith.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcarith.o src/jpeg-9c/jcarith.c

${OBJECTDIR}/src/jpeg-9c/jccoefct.o: src/jpeg-9c/jccoefct.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jccoefct.o src/jpeg-9c/jccoefct.c

${OBJECTDIR}/src/jpeg-9c/jccolor.o: src/jpeg-9c/jccolor.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jccolor.o src/jpeg-9c/jccolor.c

${OBJECTDIR}/src/jpeg-9c/jcdctmgr.o: src/jpeg-9c/jcdctmgr.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcdctmgr.o src/jpeg-9c/jcdctmgr.c

${OBJECTDIR}/src/jpeg-9c/jchuff.o: src/jpeg-9c/jchuff.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jchuff.o src/jpeg-9c/jchuff.c

${OBJECTDIR}/src/jpeg-9c/jcinit.o: src/jpeg-9c/jcinit.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcinit.o src/jpeg-9c/jcinit.c

${OBJECTDIR}/src/jpeg-9c/jcmainct.o: src/jpeg-9c/jcmainct.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcmainct.o src/jpeg-9c/jcmainct.c

${OBJECTDIR}/src/jpeg-9c/jcmarker.o: src/jpeg-9c/jcmarker.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcmarker.o src/jpeg-9c/jcmarker.c

${OBJECTDIR}/src/jpeg-9c/jcmaster.o: src/jpeg-9c/jcmaster.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcmaster.o src/jpeg-9c/jcmaster.c

${OBJECTDIR}/src/jpeg-9c/jcomapi.o: src/jpeg-9c/jcomapi.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcomapi.o src/jpeg-9c/jcomapi.c

${OBJECTDIR}/src/jpeg-9c/jcparam.o: src/jpeg-9c/jcparam.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcparam.o src/jpeg-9c/jcparam.c

${OBJECTDIR}/src/jpeg-9c/jcprepct.o: src/jpeg-9c/jcprepct.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcprepct.o src/jpeg-9c/jcprepct.c

${OBJECTDIR}/src/jpeg-9c/jcsample.o: src/jpeg-9c/jcsample.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jcsample.o src/jpeg-9c/jcsample.c

${OBJECTDIR}/src/jpeg-9c/jctrans.o: src/jpeg-9c/jctrans.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jctrans.o src/jpeg-9c/jctrans.c

${OBJECTDIR}/src/jpeg-9c/jdapimin.o: src/jpeg-9c/jdapimin.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdapimin.o src/jpeg-9c/jdapimin.c

${OBJECTDIR}/src/jpeg-9c/jdapistd.o: src/jpeg-9c/jdapistd.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdapistd.o src/jpeg-9c/jdapistd.c

${OBJECTDIR}/src/jpeg-9c/jdarith.o: src/jpeg-9c/jdarith.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdarith.o src/jpeg-9c/jdarith.c

${OBJECTDIR}/src/jpeg-9c/jdatadst.o: src/jpeg-9c/jdatadst.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdatadst.o src/jpeg-9c/jdatadst.c

${OBJECTDIR}/src/jpeg-9c/jdatasrc.o: src/jpeg-9c/jdatasrc.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdatasrc.o src/jpeg-9c/jdatasrc.c

${OBJECTDIR}/src/jpeg-9c/jdcoefct.o: src/jpeg-9c/jdcoefct.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdcoefct.o src/jpeg-9c/jdcoefct.c

${OBJECTDIR}/src/jpeg-9c/jdcolor.o: src/jpeg-9c/jdcolor.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdcolor.o src/jpeg-9c/jdcolor.c

${OBJECTDIR}/src/jpeg-9c/jddctmgr.o: src/jpeg-9c/jddctmgr.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jddctmgr.o src/jpeg-9c/jddctmgr.c

${OBJECTDIR}/src/jpeg-9c/jdhuff.o: src/jpeg-9c/jdhuff.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdhuff.o src/jpeg-9c/jdhuff.c

${OBJECTDIR}/src/jpeg-9c/jdinput.o: src/jpeg-9c/jdinput.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdinput.o src/jpeg-9c/jdinput.c

${OBJECTDIR}/src/jpeg-9c/jdmainct.o: src/jpeg-9c/jdmainct.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdmainct.o src/jpeg-9c/jdmainct.c

${OBJECTDIR}/src/jpeg-9c/jdmarker.o: src/jpeg-9c/jdmarker.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdmarker.o src/jpeg-9c/jdmarker.c

${OBJECTDIR}/src/jpeg-9c/jdmaster.o: src/jpeg-9c/jdmaster.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdmaster.o src/jpeg-9c/jdmaster.c

${OBJECTDIR}/src/jpeg-9c/jdmerge.o: src/jpeg-9c/jdmerge.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdmerge.o src/jpeg-9c/jdmerge.c

${OBJECTDIR}/src/jpeg-9c/jdpostct.o: src/jpeg-9c/jdpostct.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdpostct.o src/jpeg-9c/jdpostct.c

${OBJECTDIR}/src/jpeg-9c/jdsample.o: src/jpeg-9c/jdsample.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdsample.o src/jpeg-9c/jdsample.c

${OBJECTDIR}/src/jpeg-9c/jdtrans.o: src/jpeg-9c/jdtrans.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jdtrans.o src/jpeg-9c/jdtrans.c

${OBJECTDIR}/src/jpeg-9c/jerror.o: src/jpeg-9c/jerror.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jerror.o src/jpeg-9c/jerror.c

${OBJECTDIR}/src/jpeg-9c/jfdctflt.o: src/jpeg-9c/jfdctflt.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jfdctflt.o src/jpeg-9c/jfdctflt.c

${OBJECTDIR}/src/jpeg-9c/jfdctfst.o: src/jpeg-9c/jfdctfst.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jfdctfst.o src/jpeg-9c/jfdctfst.c

${OBJECTDIR}/src/jpeg-9c/jfdctint.o: src/jpeg-9c/jfdctint.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jfdctint.o src/jpeg-9c/jfdctint.c

${OBJECTDIR}/src/jpeg-9c/jidctflt.o: src/jpeg-9c/jidctflt.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jidctflt.o src/jpeg-9c/jidctflt.c

${OBJECTDIR}/src/jpeg-9c/jidctfst.o: src/jpeg-9c/jidctfst.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jidctfst.o src/jpeg-9c/jidctfst.c

${OBJECTDIR}/src/jpeg-9c/jidctint.o: src/jpeg-9c/jidctint.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jidctint.o src/jpeg-9c/jidctint.c

${OBJECTDIR}/src/jpeg-9c/jmemansi.o: src/jpeg-9c/jmemansi.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jmemansi.o src/jpeg-9c/jmemansi.c

${OBJECTDIR}/src/jpeg-9c/jmemmgr.o: src/jpeg-9c/jmemmgr.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jmemmgr.o src/jpeg-9c/jmemmgr.c

${OBJECTDIR}/src/jpeg-9c/jquant1.o: src/jpeg-9c/jquant1.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jquant1.o src/jpeg-9c/jquant1.c

${OBJECTDIR}/src/jpeg-9c/jquant2.o: src/jpeg-9c/jquant2.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jquant2.o src/jpeg-9c/jquant2.c

${OBJECTDIR}/src/jpeg-9c/jutils.o: src/jpeg-9c/jutils.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/jutils.o src/jpeg-9c/jutils.c

${OBJECTDIR}/src/jpeg-9c/rdbmp.o: src/jpeg-9c/rdbmp.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/rdbmp.o src/jpeg-9c/rdbmp.c

${OBJECTDIR}/src/jpeg-9c/rdcolmap.o: src/jpeg-9c/rdcolmap.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/rdcolmap.o src/jpeg-9c/rdcolmap.c

${OBJECTDIR}/src/jpeg-9c/rdgif.o: src/jpeg-9c/rdgif.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/rdgif.o src/jpeg-9c/rdgif.c

${OBJECTDIR}/src/jpeg-9c/rdppm.o: src/jpeg-9c/rdppm.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/rdppm.o src/jpeg-9c/rdppm.c

${OBJECTDIR}/src/jpeg-9c/rdrle.o: src/jpeg-9c/rdrle.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/rdrle.o src/jpeg-9c/rdrle.c

${OBJECTDIR}/src/jpeg-9c/rdswitch.o: src/jpeg-9c/rdswitch.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/rdswitch.o src/jpeg-9c/rdswitch.c

${OBJECTDIR}/src/jpeg-9c/rdtarga.o: src/jpeg-9c/rdtarga.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/rdtarga.o src/jpeg-9c/rdtarga.c

${OBJECTDIR}/src/jpeg-9c/transupp.o: src/jpeg-9c/transupp.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/transupp.o src/jpeg-9c/transupp.c

${OBJECTDIR}/src/jpeg-9c/wrbmp.o: src/jpeg-9c/wrbmp.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/wrbmp.o src/jpeg-9c/wrbmp.c

${OBJECTDIR}/src/jpeg-9c/wrgif.o: src/jpeg-9c/wrgif.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/wrgif.o src/jpeg-9c/wrgif.c

${OBJECTDIR}/src/jpeg-9c/wrppm.o: src/jpeg-9c/wrppm.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/wrppm.o src/jpeg-9c/wrppm.c

${OBJECTDIR}/src/jpeg-9c/wrrle.o: src/jpeg-9c/wrrle.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/wrrle.o src/jpeg-9c/wrrle.c

${OBJECTDIR}/src/jpeg-9c/wrtarga.o: src/jpeg-9c/wrtarga.c
	${MKDIR} -p ${OBJECTDIR}/src/jpeg-9c
	${RM} "$@.d"
	$(COMPILE.c) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpeg-9c/wrtarga.o src/jpeg-9c/wrtarga.c

${OBJECTDIR}/src/jpegCtrl.o: src/jpegCtrl.cpp
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/jpegCtrl.o src/jpegCtrl.cpp

${OBJECTDIR}/src/util.o: src/util.cpp
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -g -Isrc/include -Isrc/jpeg-9c -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/util.o src/util.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
