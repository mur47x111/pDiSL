# Import local variable definitions
-include Makefile.local

INCLUDE_DIR ?= ../src-jdk-include
LIBRARY_BASE ?= dislagents

ifeq ($(OS),Windows_NT)
    ifeq (,$(findstring /cygdrive/,$(PATH)))
        $(error Building on the Windows platform is only supported under Cygwin)
    endif

    HOST_ENV := Cygwin
else
    HOST_ENV := $(shell uname -s)
    HOST_CPU := $(shell uname -m)
endif


TARGET_ENV ?= $(HOST_ENV)
TARGET_CPU ?= $(HOST_CPU)

LIBRARY_WINDOWS = $(LIBRARY_BASE).dll
LIBRARY_MACOSX = lib$(LIBRARY_BASE).jnilib
LIBRARY_LINUX = lib$(LIBRARY_BASE).so

ifneq ($(HOST_ENV),$(TARGET_ENV))
    ifeq (,$(filter MinGW DarwinX,$(TARGET_ENV)))
        $(error Cross-compiling is only supported for TARGET_ENV=MinGW or TARGET_ENV=DarwinX)
    endif
endif

ifneq (,$(filter MinGW Cygwin,$(TARGET_ENV)))
    JDK_TARGET := windows
    LIBRARY := $(LIBRARY_WINDOWS)

    ifeq (MinGW,$(TARGET_ENV))
        CC := $(TARGET_CPU)-w64-mingw32-gcc

        CFLAGS += -DMINGW -mthreads
        CFLAGS_LD += -Wl,--kill-at
        CFLAGS_PARTS := -flto
        CFLAGS_WHOLE := -flto -fwhole-program

        LIBS += -lws2_32
    endif

else ifneq (,$(filter DarwinX Darwin,$(TARGET_ENV)))
    JDK_TARGET := macosx
    LIBRARY := $(LIBRARY_MACOSX)

    ifeq (DarwinX,$(TARGET_ENV))
        CC := darwinx-gcc
        CFLAGS += -DDARWINX
    endif

else ifeq (Linux,$(TARGET_ENV))
    LIBRARY := $(LIBRARY_LINUX)
    JDK_TARGET := linux

    #
    # Despite the GCC documentation saying that -flto should not be used with
    # -fwhole-program, the option needs to be there, otherwise GCC produces a
    # library with undefined symbols (bypass bytecode) despite them being present.
    #
    CFLAGS_PARTS := -flto
    CFLAGS_WHOLE := -flto -fwhole-program
else
    $(error Target environment $(TARGET_ENV) is not supported)
endif


# Source and object files needed to create the library
SOURCES = bytecode.c common.c jvmtiutil.c connection.c \
	connpool.c network.c classparser.c \
	dislagent.c shared/buffer.c shared/buffpack.c shared/blockingqueue.c \
	shared/threadlocal.c shared/messagetype.c \
	tagger.c sender.c pbmanager.c redispatcher.c netref.c \
	globalbuffer.c tlocalbuffer.c freehandler.c message.c

HEADERS = $(wildcard *.h) codeflags.h
GENSRCS = bytecode.c codeflags.h
OBJECTS = $(SOURCES:%.c=%.o)
SRCDEPS = $(SOURCES:%.c=%.d)



# Base options depending on the build
ifeq (,$(DEBUG))
    CFLAGS += -O3 -DNDEBUG
else
    CFLAGS += -g3 -DDEBUG
endif

# Tell the linker to create a shared library.
CFLAGS_LD += -shared

#
# For non-MinGW targets, use position independent code
# and dynamically link to the pthread library.
#
ifneq (MinGW,$(TARGET_ENV))
    CFLAGS += -fPIC
    LIBS += -lpthread
endif

# JNI and JVMTI includes
CFLAGS += -I$(INCLUDE_DIR) -I$(INCLUDE_DIR)/$(JDK_TARGET)

# Source code options
CFLAGS += -std=gnu99 -W -Wall -Wextra -Wno-unused-parameter

# Fix-up quirks before first target
-include Makefile.quirks


# Default goal

.PHONY: agent
agent: $(GENSRCS) $(LIBRARY)


# Convenience targets

.PHONY: whole
whole:
	@$(MAKE) WHOLE=1

.PHONY: debug
debug:
	@$(MAKE) DEBUG=1

# Generated files

bytecode.c:
	ant -f ../build.xml prepare-disl-agent-bytecode

codeflags.h:
	ant -f ../build.xml prepare-disl-agent-codeflags


# Compilation and linking targets

ifneq (,$(WHOLE))

CFLAGS += -DWHOLE $(CFLAGS_WHOLE)

$(LIBRARY): $(HEADERS) $(SOURCES)
	$(CC) $(CFLAGS) $(TARGET_ARCH) $(CFLAGS_LD) $(SOURCES) $(LIBS) $(OUTPUT_OPTION)

else

CFLAGS += $(CFLAGS_PARTS)

$(LIBRARY): $(OBJECTS)
	$(CC) $(CFLAGS) $(TARGET_ARCH) $(CFLAGS_LD) $(OBJECTS) $(LIBS) $(OUTPUT_OPTION)

%.d: %.c
	@$(CC) -MM $< | sed 's,\($*\)\.o[ :]*,\1.o $@ : ,g' > $@

-include $(SRCDEPS)

endif


# Cleanup targets

.PHONY: clean
clean:
	-rm -f $(OBJECTS)
	-rm -f $(SRCDEPS)

.PHONY: cleanall
cleanall: clean
	-rm -f $(LIBRARY_LINUX) $(LIBRARY_MACOSX) $(LIBRARY_WINDOWS)

.PHONY: pristine
pristine: cleanall
	-rm -f $(GENSRCS)
