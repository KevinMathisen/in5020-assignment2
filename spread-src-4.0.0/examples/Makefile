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

SPREAD_PROGRAM=${exec_prefix}/sbin/spread
SP_MAJOR_VERSION=4
SP_MINOR_VERSION=0
SP_PATCH_VERSION=0
VERSION=4.0.0

PATHS= 

CC=gcc
LD=gcc
CFLAGS=-g -O2 -Wall
CPPFLAGS=-I. -I$(srcdir) -I$(top_srcdir)/include  $(PATHS) -DHAVE_CONFIG_H
LDFLAGS=
LIBS=-lm -lnsl 
THLDFLAGS=
THLIBS=-lpthread 
AR=/usr/bin/ar
LEX=
RANLIB=ranlib
INSTALL=/usr/bin/install -c
SOFTLINK=ln -s
PERL=/usr/bin/perl
ENT=@ENT@
EXEEXT=
SP_LIBRARY_DIR=../libspread

TARGETS=spuser$(EXEEXT) spflooder$(EXEEXT) sptuser${EXEEXT} flush_user$(EXEEXT)

all: $(TARGETS) 

.c.o:
	$(CC) $(CFLAGS) $(CPPFLAGS) -c $<

.c.to:
	$(CC) $(CFLAGS) $(CPPFLAGS) -D_REENTRANT -c $< -o $*.to

.c.lo:
	$(SHCC) $(SHCFLAGS) $(SHCPPFLAGS) -c $< -o $*.lo

.c.tlo:
	$(SHCC) $(SHCFLAGS) $(SHCPPFLAGS) -D_REENTRANT -c $< -o $*.tlo

spuser$(EXEEXT): $(SP_LIBRARY_DIR)/libspread-core.a	user.o
	$(LD) -o $@ user.o $(LDFLAGS) $(SP_LIBRARY_DIR)/libspread-core.a $(LIBS)

spflooder$(EXEEXT):  $(SP_LIBRARY_DIR)/libspread-core.a flooder.o
	$(LD) -o $@ flooder.o $(LDFLAGS)  $(SP_LIBRARY_DIR)/libspread-core.a $(LIBS)

sptuser$(EXEEXT): user.to  $(SP_LIBRARY_DIR)/libtspread-core.a
	$(LD) $(THLDFLAGS) -o $@ user.to  $(SP_LIBRARY_DIR)/libtspread-core.a $(LDFLAGS) $(LIBS) $(THLIBS)

spsimple_user$(EXEEXT): simple_user.o  $(SP_LIBRARY_DIR)/libspread-core.a
	$(LD) -o $@ simple_user.o $(LDFLAGS) $(SP_LIBRARY_DIR)/libspread-core.a $(LIBS) 

flush_user$(EXEEXT): $(SP_LIBRARY_DIR)/libspread.a fl_user.to
	$(LD) $(LDFLAGS) -o flush_user fl_user.to $(SP_LIBRARY_DIR)/libspread.a $(LIBS) $(THLIBS)

sp_time_memb$(EXEEXT): $(SP_LIBRARY_DIR)/libspread.a sp_time_memb.o stats.o
	$(LD) $(LDFLAGS) -o sp_time_memb sp_time_memb.o stats.o $(LIBS)

fl_time_memb$(EXEEXT): $(SP_LIBRARY_DIR)/libspread.a fl_time_memb.o stats.o
	$(LD) $(LDFLAGS) -o fl_time_memb fl_time_memb.o stats.o $(LIBS)

clean:
	rm -f *.lo *.tlo *.to *.o *.a *.dylib $(TARGETS) spsimple_user
	rm -f core
	rm -rf ../bin/$(host)

distclean: clean
	rm -f Makefile config.h config.status *~

mrproper: distclean

veryclean: distclean
	rm -f configure

distprep: 
	autoreconf

binrelease: $(TARGETS)
	$(buildtoolsdir)/mkinstalldirs ../bin/$(host)
	$(INSTALL) -m 0755 -s spuser$(EXEEXT) ../bin/$(host)/spuser$(EXEEXT)
	$(INSTALL) -m 0755 -s sptuser$(EXEEXT) ../bin/$(host)/sptuser$(EXEEXT)
	$(INSTALL) -m 0755 -s spflooder$(EXEEXT) ../bin/$(host)/spflooder$(EXEEXT)
	$(INSTALL) -m 0755 -s flush_user$(EXEEXT) ../bin/$(host)/flush_user$(EXEEXT)

install: $(TARGETS) install-files

install-files:
	$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(bindir)
	$(INSTALL) -m 0755 -s spuser$(EXEEXT) $(DESTDIR)$(bindir)/spuser$(EXEEXT)
	$(INSTALL) -m 0755 -s sptuser$(EXEEXT) $(DESTDIR)$(bindir)/sptuser$(EXEEXT)
	$(INSTALL) -m 0755 -s spflooder$(EXEEXT) $(DESTDIR)$(bindir)/spflooder$(EXEEXT)
	$(INSTALL) -m 0755 -s flush_user$(EXEEXT) $(DESTDIR)$(bindir)/flush_user$(EXEEXT)

uninstallall:	uninstall
	-rmdir $(DESTDIR)$(bindir)

uninstall: 
	-rm -f $(DESTDIR)$(bindir)/spuser$(EXEEXT)
	-rm -f $(DESTDIR)$(bindir)/spflooder$(EXEEXT)
	-rm -f $(DESTDIR)$(bindir)/sptuser$(EXEEXT)
	-rm -f $(DESTDIR)$(bindir)/flush_user$(EXEEXT)
