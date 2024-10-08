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
datarootdir=@datarootdir@
datadir=${prefix}/share
docdir=${datadir}/doc/spread
piddir=/var/run
srcdir=.
top_srcdir=..
buildtoolsdir=$(top_srcdir)/buildtools

DESTDIR=


PATHS= 

INSTALL=/usr/bin/install -c
SOFTLINK=ln -s
PERL=/usr/bin/perl

MANPAGES	= SP_connect.3.out SP_disconnect.3.out SP_equal_group_ids.3.out SP_error.3.out SP_get_memb_info.3.out SP_get_vs_sets_info.3.out SP_get_vs_set_members.3.out SP_join.3.out SP_leave.3.out SP_multicast.3.out SP_multigroup_multicast.3.out SP_multigroup_scat_multicast.3.out SP_poll.3.out SP_receive.3.out SP_scat_get_memb_info.3.out SP_scat_get_vs_sets_info.3.out SP_scat_get_vs_set_members.3.out SP_scat_multicast.3.out SP_scat_receive.3.out SP_version.3.out libspread.3.out spread.1.out spuser.1.out sptuser.1.out spmonitor.1.out spflooder.1.out
MANPAGES_IN	= SP_connect.3 SP_disconnect.3 SP_equal_group_ids.3 SP_error.3 SP_get_memb_info.3 SP_get_vs_sets_info.3 SP_get_vs_set_members.3 SP_join.3 SP_leave.3 SP_multicast.3 SP_multigroup_multicast.3 SP_multigroup_scat_multicast.3 SP_poll.3 SP_receive.3 SP_scat_get_memb_info.3 SP_scat_get_vs_sets_info.3 SP_scat_get_vs_set_members.3 SP_scat_multicast.3 SP_scat_receive.3 SP_version.3 libspread.3 spread.1 spuser.1 sptuser.1 spmonitor.1 spflooder.1

PAGENAMES = connect disconnect equal_group_ids error get_memb_info get_vs_sets_info get_vs_set_members join leave multicast multigroup_multicast multigroup_scat_multicast poll receive scat_get_memb_info scat_get_vs_sets_info scat_get_vs_set_members scat_multicast scat_receive

DOCFILES = DynamicConfiguration.txt MultithreadedClients.txt PORTING Short_Buffer_Handling.txt Win32BuildInstructions.pdf TODO

MANTYPE		= cat

PATHSUBS	= \
	-D/etc/spread.conf=$(sysconfdir)/spread.conf \
        -D/var/run/spread.pid=$(piddir)/spread.pid

FIXPATHSCMD	= $(PERL) $(buildtoolsdir)/fixpaths $(PATHSUBS)

all: $(MANPAGES)

$(MANPAGES): $(MANPAGES_IN)
	@if test "$(MANTYPE)" = "cat"; then \
		manpage=$(srcdir)/`echo $@ | sed 's/\.[1-9]\.out$$/\.0/'`; \
	else \
		manpage=$(srcdir)/`echo $@ | sed 's/\.out$$//'`; \
	fi; \
	if test "$(MANTYPE)" = "man"; then \
		$(FIXPATHSCMD) $${manpage} | $(PERL) $(buildtoolsdir)/mdoc2man.pl > $@; \
	else \
		$(FIXPATHSCMD) $${manpage} > $@; \
	fi

clean:
	rm -f *.out 

distclean: clean
	rm -f Makefile config.h config.status *~

mrproper: distclean

veryclean: distclean
	rm -f configure

catman-do:
	@for f in $(MANPAGES_IN) ; do \
		base=`echo $$f | sed 's/\..*$$//'` ; \
		echo "$$f -> $$base.0" ; \
		nroff -mandoc $$f | cat -v | sed -e 's/.\^H//g' \
			>$$base.0 ; \
	done

distprep: catman-do
	autoreconf

install: install-files install-man

install-man:
	$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(mandir)
	$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(mandir)/$(mansubdir)1
	$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(mandir)/$(mansubdir)3
	$(INSTALL) -m 644 spread.1.out $(DESTDIR)$(mandir)/$(mansubdir)1/spread.1
	$(INSTALL) -m 644 spuser.1.out $(DESTDIR)$(mandir)/$(mansubdir)1/spuser.1
	$(INSTALL) -m 644 sptuser.1.out $(DESTDIR)$(mandir)/$(mansubdir)1/sptuser.1
	$(INSTALL) -m 644 spflooder.1.out $(DESTDIR)$(mandir)/$(mansubdir)1/spflooder.1
	$(INSTALL) -m 644 spmonitor.1.out $(DESTDIR)$(mandir)/$(mansubdir)1/spmonitor.1
	$(INSTALL) -m 644 libspread.3.out $(DESTDIR)$(mandir)/$(mansubdir)3/libspread.3
	for page in $(PAGENAMES); \
	do \
	  $(INSTALL) -m 644 SP_$$page.3.out $(DESTDIR)$(mandir)/$(mansubdir)3/SP_$$page.3; \
	done
	for page in connect disconnect error flush join leave more_msgs multicast poll receive scat_multicast scat_receive scat_subgroupcast scat_unicast subgroupcast unicast version; \
	do \
	  $(INSTALL) -m 644 $(srcdir)/flush/man/FL_$$page.3 $(DESTDIR)$(mandir)/$(mansubdir)3/FL_$$page.3; \
	done

install-files:
	$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(docdir)
	for docfile in $(DOCFILES); \
	do \
	  $(INSTALL) -m 644 $(srcdir)/$$docfile $(DESTDIR)$(docdir)/$$docfile; \
	done
	$(INSTALL) -m 644 $(srcdir)/../Readme.txt $(DESTDIR)$(docdir)/Readme.txt; 
	$(INSTALL) -m 644 $(srcdir)/../license.txt $(DESTDIR)$(docdir)/license.txt; 
	if [ ! -d $(DESTDIR)$(sysconfdir) ]; then \
		$(buildtoolsdir)/mkinstalldirs $(DESTDIR)$(sysconfdir); \
	fi
	if [ ! -f $(DESTDIR)$(sysconfdir)/spread.conf ]; then \
		$(INSTALL) -m 644 $(srcdir)/sample.spread.conf $(DESTDIR)$(sysconfdir)/spread.conf; \
	else \
		echo "$(DESTDIR)$(sysconfdir)/spread.conf already exists, install will not overwrite"; \
	fi

uninstallall:	uninstall
	-rm -f $(DESTDIR)$(sysconfdir)/spread.conf
	-rmdir $(DESTDIR)$(sysconfdir)
	-rmdir $(DESTDIR)$(docdir)
	-rmdir $(DESTDIR)$(mandir)/$(mansubdir)1
	-rmdir $(DESTDIR)$(mandir)/$(mansubdir)3
	-rmdir $(DESTDIR)$(mandir)

uninstall: 
	-rm -f $(DESTDIR)$(mandir)/$(mansubdir)3/SP_*.3
	-rm -f $(DESTDIR)$(mandir)/$(mansubdir)3/FL_*.3
	-rm -f $(DESTDIR)$(mandir)/$(mansubdir)3/libspread.3
	-rm -f $(DESTDIR)$(mandir)/$(mansubdir)1/spread.1
	-rm -f $(DESTDIR)$(mandir)/$(mansubdir)1/spuser.1
	-rm -f $(DESTDIR)$(mandir)/$(mansubdir)1/sptuser.1
	-rm -f $(DESTDIR)$(mandir)/$(mansubdir)1/spflooder.1
	-rm -f $(DESTDIR)$(mandir)/$(mansubdir)1/spmonitor.1
	for docfile in $(DOCFILES); \
	do \
	  -rm -f $(DESTDIR)$(docdir)/$$docfile; \
	done
	-rm -f $(DESTDIR)$(docdir)/Readme.txt
	-rm -f $(DESTDIR)$(docdir)/license.txt
