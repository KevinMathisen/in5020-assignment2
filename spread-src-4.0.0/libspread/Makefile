.SUFFIXES: .lo .to .tlo

prefix=/usr/local
exec_prefix=${prefix}
bindir=${exec_prefix}/bin
sbindir=${exec_prefix}/sbin
libdir=${exec_prefix}/lib
includedir=${prefix}/include
mandir=${prefix}/man
mansubdir=cat
sysconfdir=${prefix}/etc
piddir=/var/run
srcdir=.
top_srcdir=..
buildtoolsdir=$(top_srcdir)/buildtools
host=x86_64-unknown-linux-gnu

DESTDIR=
VPATH= $(top_srcdir)/daemon
SPREAD_PROGRAM=${exec_prefix}/sbin/spread
SP_MAJOR_VERSION=4
SP_MINOR_VERSION=0
SP_PATCH_VERSION=0
VERSION=4.0.0

#Shared Library version -- Must be changed upon each release 
#Rules: major  -- inc for incompatible change
#     : minor  -- inc for bugfix or forward compatible change

LIBVERSION=2.0

PATHS= 

CC=gcc
LD=gcc

# don't use debugging (asserts and dprintfs) (optional)
NO_DEBUG = -DNDEBUG
#flush defines
DEFINES= $(NO_DEBUG)

CFLAGS=-g -O2 -Wall $(DEFINES)
#stdutil includes
FLUSH_INC_DIR = -I../stdutil/src -I$(top_srcdir)/stdutil/src -I$(srcdir) 
CPPFLAGS=-I$(top_srcdir)/daemon -I../daemon -I$(top_srcdir)/include $(FLUSH_INC_DIR)  $(PATHS) -DHAVE_CONFIG_H
LDFLAGS= 
LIBS=-lm -lnsl  
THLDFLAGS=
THLIBS=-lpthread 
AR=/usr/bin/ar
LEX=
SHCC=gcc
SHLD=gcc -shared
SHCFLAGS=-g -O2 -Wall -fPIC $(DEFINES)
SHCPPFLAGS= -I$(top_srcdir)/daemon -I../daemon -I$(top_srcdir)/include $(FLUSH_INC_DIR) 
SHLDFLAGS= 
SHLIBS=-lm -lnsl 
SHLDCONVERTSTATIC=-Wl,-whole-archive
SHLDCONVERTSTATICEND=-Wl,-no-whole-archive
RANLIB=ranlib
INSTALL=/usr/bin/install -c
SOFTLINK=ln -s
PERL=/usr/bin/perl
ENT=@ENT@
EXEEXT=

SP_OBJ_DIR=../daemon

TARGETS=libspread-core.a libtspread-core.a libspread.a libspread.so libspread-core.so libtspread-core.so

LIBSP_OBJS= $(SP_OBJ_DIR)/alarm.o $(SP_OBJ_DIR)/events.o $(SP_OBJ_DIR)/memory.o sp.o

LIBSP_SHOBJS= alarm.lo events.lo memory.lo sp.lo

LIBTSP_OBJS= alarm.to events.to memory.to sp.to

LIBTSP_SHOBJS= alarm.tlo events.tlo memory.tlo sp.tlo

LIBTFL_OBJS= fl.to scatp.to alarm.to events.to memory.to sp.to

LIBTFL_SHOBJS= fl.tlo scatp.tlo alarm.tlo events.tlo memory.tlo sp.tlo

# A bit of a hack to grab the -fPIC threaded object files to merge into the shared spread library
# since the stdutil.so cannot be merged, and the stdutil.a is not compiled -fPIC
# Can be fixed correctly if stdutil generates incremental shared library versions
STDUTIL_DIR=../stdutil/src

SHARED_STDUTIL= $(STDUTIL_DIR)/stdarr.lto $(STDUTIL_DIR)/stdcarr.lto $(STDUTIL_DIR)/stddll.lto $(STDUTIL_DIR)/stderror.lto $(STDUTIL_DIR)/stdfd.lto $(STDUTIL_DIR)/stdhash.lto $(STDUTIL_DIR)/stdit.lto $(STDUTIL_DIR)/stdskl.lto $(STDUTIL_DIR)/stdthread.lto $(STDUTIL_DIR)/stdtime.lto $(STDUTIL_DIR)/stdutil.lto

all: $(TARGETS)

.c.o:
	$(CC) $(CFLAGS) $(CPPFLAGS) -c $<

.c.to:
	$(CC) $(CFLAGS) $(CPPFLAGS) -D_REENTRANT -c $< -o $*.to

.c.lo:
	$(SHCC) $(SHCFLAGS) $(SHCPPFLAGS) -c $< -o $*.lo

.c.tlo:
	$(SHCC) $(SHCFLAGS) $(SHCPPFLAGS) -D_REENTRANT -c $< -o $*.tlo

none:	
	@echo "Skipping shared library creation."

install-none:
	@echo "Skipping shared library installation."

# libspread-core is the core spread library without Flush or Stdutil
libspread-core.a: $(LIBSP_OBJS)
	$(AR) rv $@ $(LIBSP_OBJS)
	$(RANLIB) $@

libspread-core.dylib:	$(LIBSP_SHOBJS)
	$(SHLD) -o libspread-core.$(LIBVERSION).dylib $(LIBSP_SHOBJS) $(SHLDFLAGS) $(SHLIBS)
	$(SOFTLINK) -f libspread-core.$(LIBVERSION).dylib $@

install-libspread-core.dylib:	libspread-core.dylib
	$(INSTALL) -m 0755 libspread-core.$(LIBVERSION).dylib $(DESTDIR)$(libdir)/libspread-core.$(LIBVERSION).dylib
	(cd $(DESTDIR)$(libdir); $(SOFTLINK) -f libspread-core.$(LIBVERSION).dylib libspread-core.dylib)

libspread-core.so:	$(LIBSP_SHOBJS)
	$(SHLD) -o $@ $(LIBSP_SHOBJS) $(SHLDFLAGS) $(SHLIBS)

install-libspread-core.so:	libspread-core.so
	$(INSTALL) -m 0755 libspread-core.so $(DESTDIR)$(libdir)/libspread-core.so.$(LIBVERSION)
	(cd $(DESTDIR)$(libdir); $(SOFTLINK) -f libspread-core.so.$(LIBVERSION) libspread-core.so)

libtspread-core.a: $(LIBTSP_OBJS)
	$(AR) rv $@ $(LIBTSP_OBJS)
	$(RANLIB) $@

libtspread-core.dylib:	$(LIBTSP_SHOBJS)
	$(SHLD) -o libtspread-core.$(LIBVERSION).dylib $(LIBTSP_SHOBJS) $(SHLDFLAGS) $(SHLIBS) $(THLIBS)
	$(SOFTLINK) -f libtspread-core.$(LIBVERSION).dylib $@

install-libtspread-core.dylib:	libtspread-core.dylib
	$(INSTALL) -m 0755 libtspread-core.$(LIBVERSION).dylib $(DESTDIR)$(libdir)/libtspread-core.$(LIBVERSION).dylib
	(cd $(DESTDIR)$(libdir); $(SOFTLINK) -f libtspread-core.$(LIBVERSION).dylib libtspread-core.dylib)

libtspread-core.so:	$(LIBTSP_SHOBJS)
	$(SHLD) -o $@ $(LIBTSP_SHOBJS) $(SHLDFLAGS) $(SHLIBS) $(THLIBS)

install-libtspread-core.so:	libtspread-core.so
	$(INSTALL) -m 0755 libtspread-core.so $(DESTDIR)$(libdir)/libtspread-core.so.$(LIBVERSION)
	(cd $(DESTDIR)$(libdir); $(SOFTLINK) -f libtspread-core.so.$(LIBVERSION) libtspread-core.so)

# Full Spread library with Fl_, SP_, and support modules linked in
libspread.a: $(LIBTFL_OBJS) ../stdutil/lib/libstdutil-threaded-release.a
	$(AR) rv $@ $(LIBTFL_OBJS)
	$(AR) xo ../stdutil/lib/libstdutil-threaded-release.a
	$(AR) rv $@ std*.to
	$(RANLIB) $@

libspread.dylib:	$(LIBTFL_SHOBJS)
	$(SHLD) -o libspread.$(LIBVERSION).dylib $(LIBTFL_SHOBJS) $(SHLDFLAGS) $(SHLDCONVERTSTATIC) ../stdutil/lib/libstdutil-threaded-release.a $(SHLDCONVERTSTATICEND) $(SHLIBS) $(THLIBS)
	$(SOFTLINK) -f libspread.$(LIBVERSION).dylib $@

install-libspread.dylib:	libspread.dylib
	$(INSTALL) -m 0755 libspread.$(LIBVERSION).dylib $(DESTDIR)$(libdir)/libspread.$(LIBVERSION).dylib
	(cd $(DESTDIR)$(libdir); $(SOFTLINK) -f libspread.$(LIBVERSION).dylib libspread.dylib)

libspread.so:	$(LIBTFL_SHOBJS)
	$(SHLD) -o $@ $(LIBTFL_SHOBJS) $(SHLDFLAGS) $(SHARED_STDUTIL) $(SHLIBS) $(THLIBS)

install-libspread.so:	libspread.so
	$(INSTALL) -m 0755 libspread.so $(DESTDIR)$(libdir)/libspread.so.$(LIBVERSION)
	(cd $(DESTDIR)$(libdir); $(SOFTLINK) -f libspread.so.$(LIBVERSION) libspread.so)

clean:
	rm -f *.lo *.tlo *.to *.o *.a *.dylib *.so $(TARGETS)
	rm -f core
	rm -rf autom4te.cache
	rm -rf ../lib/$(host)

distclean: clean
	rm -f Makefile config.status *~

binrelease: $(TARGETS)
	$(buildtoolsdir)/mkinstalldirs ../lib/$(host)
	$(INSTALL) -m 644 libspread.a ../lib/$(host)/libspread.a
	$(INSTALL) -m 644 libspread-core.a ../lib/$(host)/libspread-core.a
	$(INSTALL) -m 644 libtspread-core.a ../lib/$(host)/libtspread-core.a
ifeq (libspread.so, libspread.so)
	  $(INSTALL) -m 755 libspread.so ../lib/$(host)/libspread.so.$(LIBVERSION)
	  $(INSTALL) -m 755 libspread-core.so ../lib/$(host)/libspread-core.so.$(LIBVERSION)
	  $(INSTALL) -m 755 libtspread-core.so ../lib/$(host)/libtspread-core.so.$(LIBVERSION)
endif
ifeq (libspread.so, libspread.dylib)
	  $(INSTALL) -m 755 libspread.$(LIBVERSION).dylib ../lib/$(host)/libspread.$(LIBVERSION).dylib
	  $(INSTALL) -m 755 libspread-core.$(LIBVERSION).dylib ../lib/$(host)/libspread-core.$(LIBVERSION).dylib
	  $(INSTALL) -m 755 libtspread-core.$(LIBVERSION).dylib ../lib/$(host)/libtspread-core.$(LIBVERSION).dylib
endif

install: $(TARGETS) install-files install-libspread.so install-libspread-core.so install-libtspread-core.so

install-files:
	$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(libdir)
	$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(includedir)
	$(INSTALL) -m 644 libspread.a $(DESTDIR)$(libdir)/libspread.a
	$(INSTALL) -m 644 libspread-core.a $(DESTDIR)$(libdir)/libspread-core.a
	$(INSTALL) -m 644 libtspread-core.a $(DESTDIR)$(libdir)/libtspread-core.a
	$(INSTALL) -m 644 $(top_srcdir)/include/sp.h $(DESTDIR)$(includedir)/sp.h
	$(INSTALL) -m 644 $(top_srcdir)/include/sp_func.h $(DESTDIR)$(includedir)/sp_func.h
	$(INSTALL) -m 644 $(top_srcdir)/include/sp_events.h $(DESTDIR)$(includedir)/sp_events.h
	$(INSTALL) -m 644 $(top_srcdir)/include/fl.h $(DESTDIR)$(includedir)/fl.h

uninstallall:	uninstall
	-rmdir $(DESTDIR)$(libdir)

uninstall: 
	-rm -f $(DESTDIR)$(libdir)/libspread.a
	-rm -f $(DESTDIR)$(libdir)/libspread-core.a
	-rm -f $(DESTDIR)$(libdir)/libtspread-core.a
	-rm -f $(DESTDIR)$(libdir)/libspread.so
	-rm -f $(DESTDIR)$(libdir)/libspread-core.so
	-rm -f $(DESTDIR)$(libdir)/libtspread-core.so
	-rm -f $(DESTDIR)$(includedir)/sp.h
	-rm -f $(DESTDIR)$(includedir)/sp_func.h
	-rm -f $(DESTDIR)$(includedir)/sp_events.h
	-rm -f $(DESTDIR)$(includedir)/fl.h
