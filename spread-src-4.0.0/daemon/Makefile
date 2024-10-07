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
CPPFLAGS=-I. -I$(srcdir) -I$(top_srcdir)/include -I../stdutil/src -I$(top_srcdir)/stdutil/src  $(PATHS) -DHAVE_CONFIG_H
LDFLAGS= 
LIBS=-lm -lnsl  
THLDFLAGS=
THLIBS=-lpthread 
LEX=
INSTALL=/usr/bin/install -c
SOFTLINK=ln -s
PERL=/usr/bin/perl
ENT=@ENT@
EXEEXT=

TARGETS=spread$(EXEEXT) spmonitor$(EXEEXT)

SPREADOBJS= spread.o protocol.o session.o groups.o alarm.o events.o memory.o membership.o data_link.o network.o status.o log.o flow_control.o message.o lex.yy.o y.tab.o configuration.o acm.o acp-permit.o auth-null.o auth-ip.o

MONITOR_OBJS= monitor.o  alarm.o events.o memory.o data_link.o lex.yy.o y.tab.o configuration.o acm.o

TMONITOR_OBJS= monitor.to  alarm.to events.to memory.to data_link.to lex.yy.to y.tab.to configuration.to acm.to

all: $(TARGETS)

$(SPREADOBJS): config.h
$(MONITOR_OBJS): config.h
$(TMONITOR_OBJS): config.h

.c.o:
	$(CC) $(CFLAGS) $(CPPFLAGS) -c $<

.c.to:
	$(CC) $(CFLAGS) $(CPPFLAGS) -D_REENTRANT -c $< -o $*.to

parser:
	$(YACC) -d $(srcdir)/config_parse.y
	$(LEX) $(srcdir)/config_gram.l

#Disabled now because of timestamp errors causing extra regeneration
#To build parser run "make parser" after ./configure is run.
#lex.yy.c:       config_gram.l y.tab.c
#	$(LEX) config_gram.l

#y.tab.c:        config_parse.y
#	$(YACC) -d config_parse.y

spread$(EXEEXT): $(SPREADOBJS) ../stdutil/lib/libstdutil-threaded-release.a
	$(LD) -o $@ $(SPREADOBJS) ../stdutil/lib/libstdutil-threaded-release.a $(LDFLAGS) $(LIBS)

spmonitor$(EXEEXT): $(MONITOR_OBJS)
	$(LD) -o $@ $(MONITOR_OBJS) $(LDFLAGS) $(LIBS) 

sptmonitor$(EXEEXT): $(TMONITOR_OBJS)
	$(LD) $(THLDFLAGS) -o $@ $(TMONITOR_OBJS) $(LDFLAGS) $(LIBS) $(THLIBS) 

testprog: spsend$(EXEEXT) sprecv$(EXEEXT)

spsend$(EXEEXT): s.o alarm.o data_link.o events.o memory.o
	$(LD) -o $@ s.o alarm.o data_link.o events.o memory.o $(LDFLAGS) $(LIBS)

sprecv$(EXEEXT): r.o alarm.o data_link.o
	$(LD) -o $@ r.o alarm.o data_link.o $(LDFLAGS) $(LIBS)

clean:
	rm -f *.lo *.tlo *.to *.o *.a *.dylib $(TARGETS) spsimple_user
	rm -f config.cache config.log docs/*.out core
	rm -rf autom4te.cache
	rm -rf ../bin/$(host)

distclean: clean
	rm -f Makefile config.h config.status *~

binrelease: $(TARGETS)
	$(buildtoolsdir)/mkinstalldirs ../bin/$(host)
	$(INSTALL) -m 0755 -s spmonitor$(EXEEXT) ../bin/$(host)/spmonitor$(EXEEXT)
	$(INSTALL) -m 0755 -s spread$(EXEEXT) ../bin/$(host)/spread$(EXEEXT)

install: $(TARGETS) install-files 

install-files:
	$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(bindir)
	$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(sbindir)
	$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(includedir)
	$(INSTALL) -m 0755 -s spmonitor$(EXEEXT) $(DESTDIR)$(bindir)/spmonitor$(EXEEXT)
	$(INSTALL) -m 0755 -s spread$(EXEEXT) $(DESTDIR)$(sbindir)/spread$(EXEEXT)

uninstallall:	uninstall
	-rmdir $(DESTDIR)$(bindir)
	-rmdir $(DESTDIR)$(sbindir)

uninstall: 
	-rm -f $(DESTDIR)$(bindir)/spmonitor$(EXEEXT)
	-rm -f $(DESTDIR)$(sbindir)/spread$(EXEEXT)
