.SUFFIXES: .do .to .tdo .lo .ldo .lto .ltdo
.PHONY: all standard libdir clean distclean uberclean

LIBVERSION=1.0

############################################ PROGRAMS #########################################

CC=gcc
LD=@LD@
SHCC=gcc
SHLD=gcc -shared

AR=/usr/bin/ar
SOFTLINK=ln -s
INSTALL=/usr/bin/install -c

srcdir=.
top_srcdir=..

buildtoolsdir=$(top_srcdir)/buildtools

############################################# FLAGS ###########################################

CFLAGS=-Wall
CPPFLAGS= -I./ -I$(srcdir)
LDFLAGS=
LIBS=

SHCFLAGS=-Wall -fPIC
SHCPPFLAGS= -I./ -I$(srcdir)
SHLDFLAGS= 
SHLIBS=

THLIBS=-lpthread 
THLDFLAGS=-pthread 

RELEASEFLAGS=-DNDEBUG -O2
DEBUGFLAGS=-g -DSTDSAFETY_CHECKS -DSTDBOUNDS_CHECKS
THREADFLAGS=-D_REENTRANT

############################################# OBJECTS #########################################

STATIC_NOTHREAD_RELEASE_OBJS=stdutil.o stderror.o stdthread.o stdtime.o stdfd.o stdit.o stdarr.o stdcarr.o stddll.o stdhash.o stdskl.o
STATIC_NOTHREAD_DEBUG_OBJS=stdutil.do stderror.do stdthread.do stdtime.do stdfd.do stdit.do stdarr.do stdcarr.do stddll.do stdhash.do stdskl.do
STATIC_THREADED_RELEASE_OBJS=stdutil.to stderror.to stdthread.to stdtime.to stdfd.to stdit.to stdarr.to stdcarr.to stddll.to stdhash.to stdskl.to
STATIC_THREADED_DEBUG_OBJS=stdutil.tdo stderror.tdo stdthread.tdo stdtime.tdo stdfd.tdo stdit.tdo stdarr.tdo stdcarr.tdo stddll.tdo stdhash.tdo stdskl.tdo
SHARED_NOTHREAD_RELEASE_OBJS=stdutil.lo stderror.lo stdthread.lo stdtime.lo stdfd.lo stdit.lo stdarr.lo stdcarr.lo stddll.lo stdhash.lo stdskl.lo
SHARED_NOTHREAD_DEBUG_OBJS=stdutil.ldo stderror.ldo stdthread.ldo stdtime.ldo stdfd.ldo stdit.ldo stdarr.ldo stdcarr.ldo stddll.ldo stdhash.ldo stdskl.ldo
SHARED_THREADED_RELEASE_OBJS=stdutil.lto stderror.lto stdthread.lto stdtime.lto stdfd.lto stdit.lto stdarr.lto stdcarr.lto stddll.lto stdhash.lto stdskl.lto
SHARED_THREADED_DEBUG_OBJS=stdutil.ltdo stderror.ltdo stdthread.ltdo stdtime.ltdo stdfd.ltdo stdit.ltdo stdarr.ltdo stdcarr.ltdo stddll.ltdo stdhash.ltdo stdskl.ltdo

############################################# TARGETS #########################################

BINDIR=../bin
LIBDIR=../lib

STATIC_NOTHREAD_RELEASE_LIB=$(LIBDIR)/libstdutil-nothread-release.a
STATIC_NOTHREAD_DEBUG_LIB=$(LIBDIR)/libstdutil-nothread-debug.a
STATIC_THREADED_RELEASE_LIB=$(LIBDIR)/libstdutil-threaded-release.a
STATIC_THREADED_DEBUG_LIB=$(LIBDIR)/libstdutil-threaded-debug.a
SHARED_NOTHREAD_RELEASE_LIB=$(LIBDIR)/libstdutil-nothread-release.$(LIBVERSION).so
SHARED_NOTHREAD_DEBUG_LIB=$(LIBDIR)/libstdutil-nothread-debug.$(LIBVERSION).so
SHARED_THREADED_RELEASE_LIB=$(LIBDIR)/libstdutil-threaded-release.$(LIBVERSION).so
SHARED_THREADED_DEBUG_LIB=$(LIBDIR)/libstdutil-threaded-debug.$(LIBVERSION).so

STATIC_LIBS=$(STATIC_NOTHREAD_RELEASE_LIB) $(STATIC_NOTHREAD_DEBUG_LIB) $(STATIC_THREADED_RELEASE_LIB) $(STATIC_THREADED_DEBUG_LIB)
SHARED_LIBS=$(SHARED_NOTHREAD_RELEASE_LIB) $(SHARED_NOTHREAD_DEBUG_LIB) $(SHARED_THREADED_RELEASE_LIB) $(SHARED_THREADED_DEBUG_LIB)

ALLTARGETS=$(STATIC_LIBS) $(SHARED_LIBS)

########################################### BUILD RULES ########################################

standard: libdir $(STATIC_THREADED_RELEASE_LIB) $(SHARED_THREADED_RELEASE_LIB)

all: libdir $(ALLTARGETS)

libdir:
	$(buildtoolsdir)/mkinstalldirs $(LIBDIR)

$(STATIC_NOTHREAD_RELEASE_LIB): $(STATIC_NOTHREAD_RELEASE_OBJS)
	$(AR) rvs $@ $(STATIC_NOTHREAD_RELEASE_OBJS)

$(STATIC_NOTHREAD_DEBUG_LIB): $(STATIC_NOTHREAD_DEBUG_OBJS)
	$(AR) rvs $@ $(STATIC_NOTHREAD_DEBUG_OBJS)

$(STATIC_THREADED_RELEASE_LIB): $(STATIC_THREADED_RELEASE_OBJS)
	$(AR) rvs $@ $(STATIC_THREADED_RELEASE_OBJS)
	$(SOFTLINK) -f $@ $(LIBDIR)/libstdutil.a

$(STATIC_THREADED_DEBUG_LIB): $(STATIC_THREADED_DEBUG_OBJS)
	$(AR) rvs $@ $(STATIC_THREADED_DEBUG_OBJS)

$(SHARED_NOTHREAD_RELEASE_LIB): $(SHARED_NOTHREAD_RELEASE_OBJS)
	$(SHLD) $(SHLDFLAGS) -o $@ $(SHARED_NOTHREAD_RELEASE_OBJS) $(SHLIBS)

$(SHARED_NOTHREAD_DEBUG_LIB): $(SHARED_NOTHREAD_DEBUG_OBJS)
	$(SHLD) $(SHLDFLAGS) -o $@ $(SHARED_NOTHREAD_DEBUG_OBJS) $(SHLIBS)

$(SHARED_THREADED_RELEASE_LIB): $(SHARED_THREADED_RELEASE_OBJS) 
	$(SHLD) $(SHLDFLAGS) -o $@ $(SHARED_THREADED_RELEASE_OBJS) $(SHLIBS) $(THLIBS)
	$(SOFTLINK) -f $@ $(LIBDIR)/libstdutil.so

$(SHARED_THREADED_DEBUG_LIB): $(SHARED_THREADED_DEBUG_OBJS)
	$(SHLD) $(SHLDFLAGS) -o $@ $(SHARED_THREADED_DEBUG_OBJS) $(SHLIBS) $(THLIBS)

clean:
	rm -f *.o *.do *.to *.tdo *.lo *.ldo *.lto *.ltdo core* *~ $(ALLTARGETS) $(LIBDIR)/libstdutil.a $(LIBDIR)/libstdutil.so

distclean: clean
	rm -f Makefile stdutil/private/stdarch_autoconf.h

uberclean: distclean

.c.o:
	$(CC) $(CFLAGS) $(CPPFLAGS) $(RELEASEFLAGS) -c $<

.c.do:
	$(CC) $(CFLAGS) $(CPPFLAGS) $(DEBUGFLAGS) -c $< -o $*.do

.c.to:
	$(CC) $(CFLAGS) $(CPPFLAGS) $(RELEASEFLAGS) $(THREADFLAGS) -c $< -o $*.to

.c.tdo:
	$(CC) $(CFLAGS) $(CPPFLAGS) $(DEBUGFLAGS) $(THREADFLAGS) -c $< -o $*.tdo

.c.lo:
	$(SHCC) $(SHCFLAGS) $(SHCPPFLAGS) $(RELEASEFLAGS) -c $< -o $*.lo

.c.ldo:
	$(SHCC) $(SHCFLAGS) $(SHCPPFLAGS) $(DEBUGFLAGS) -c $< -o $*.ldo

.c.lto:
	$(SHCC) $(SHCFLAGS) $(SHCPPFLAGS) $(RELEASEFLAGS) $(THREADFLAGS) -c $< -o $*.lto

.c.ltdo:
	$(SHCC) $(SHCFLAGS) $(SHCPPFLAGS) $(DEBUGFLAGS) $(THREADFLAGS) -c $< -o $*.ltdo
