
LOCAL_PATH := $(shell pwd)

PROJECT_ROOT = $(LOCAL_PATH)/..
EXTERNAL_ROOT = $(PROJECT_ROOT)/external

# Android NDK setup
NDK_COMPILER_VERSION=4.6
NDK_ABI=arm
NDK_BASE ?=  /usr/local/android-ndk
#platform level >= 8 required for dladdr()
NDK_PLATFORM_LEVEL ?= 8
NDK_SYSROOT=$(NDK_BASE)/platforms/android-$(NDK_PLATFORM_LEVEL)/arch-arm
NDK_UNAME=`uname -s | tr '[A-Z]' '[a-z]'`
NDK_TOOLCHAIN=$(NDK_BASE)/toolchains/$(NDK_ABI)-linux-androideabi-$(NDK_COMPILER_VERSION)/prebuilt/$(NDK_UNAME)-x86

# to use the real HOST tag, you need the latest libtool files:
# http://stackoverflow.com/questions/4594736/configure-does-not-recognize-androideabi
NDK_UNAME := `uname -s | tr '[A-Z]' '[a-z]'`
HOST := arm-linux-androideabi

LOCAL_LDFLAGS   = -L$(LOCAL_PATH)/openssl/obj/local/armeabi/ -ldl -lcrypto -lssl -lz
CFLAGS = -DANDROID -I$(LOCAL_PATH)/openssl/include
CC := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-gcc --sysroot=$(NDK_SYSROOT)
CXX := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-g++
CPP := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-cpp
LD := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ld
AR := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ar
RANLIB := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ranlib
STRIP := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-strip \

all: openssl/libs/armeabi/libcrypto.so tlsdate-build

openssl/libs/armeabi/libcrypto.so:
	cd openssl && ndk-build -j4

openssl-clean:
	-cd openssl && ndk-build clean

openssl-distclean:
	-cd openssl && ndk-build distclean

tlsdate/configure: tlsdate/configure.ac
	cd tlsdate && ./autogen.sh

tlsdate-build: tlsdate/configure
	cd tlsdate;  \
		CFLAGS="$(CFLAGS)" ./configure \
			CC="$(CC)" \
			AR=$(AR) \
			RANLIB=$(RANLIB) \
			CFLAGS="$(CFLAGS)" \
			--disable-languages \
			--enable-static \
			--host=$(HOST) \
			--prefix=$(LOCAL) && make CFLAGS="$(CFLAGS)" LDFLAGS="$(LOCAL_LDFLAGS)"

tlsdate-clean:
	-make -C tlsdate clean

tlsdate-distclean:
	-make -C tlsdate distclean

clean: openssl-clean tlsdate-clean
distclean: openssl-distclean tlsdate-distclean

.PHONY: clean openssl-clean tlsdate-clean
