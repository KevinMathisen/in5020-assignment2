/*
 * The Spread Toolkit.
 *     
 * The contents of this file are subject to the Spread Open-Source
 * License, Version 1.0 (the ``License''); you may not use
 * this file except in compliance with the License.  You may obtain a
 * copy of the License at:
 *
 * http://www.spread.org/license/
 *
 * or in the file ``license.txt'' found in this distribution.
 *
 * Software distributed under the License is distributed on an AS IS basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License 
 * for the specific language governing rights and limitations under the 
 * License.
 *
 * The Creators of Spread are:
 *  Yair Amir, Michal Miskin-Amir, Jonathan Stanton, John Schultz.
 *
 *  Copyright (C) 1993-2006 Spread Concepts LLC <info@spreadconcepts.com>
 *
 *  All Rights Reserved.
 *
 * Major Contributor(s):
 * ---------------
 *    Ryan Caudy           rcaudy@gmail.com - contributions to process groups.
 *    Claudiu Danilov      claudiu@acm.org - scalable wide area support.
 *    Cristina Nita-Rotaru crisn@cs.purdue.edu - group communication security.
 *    Theo Schlossnagle    jesus@omniti.com - Perl, autoconf, old skiplist.
 *    Dan Schoenblum       dansch@cnds.jhu.edu - Java interface.
 *
 */


/*
// example for a file:
// 4
// 3 132.27.1.0 [4803]
//	harpo	[132.28.33.22]
//	hazard
//	hal
// 4 132.28.3.0 3377
//	bih
//	binoc
// 	bbl
//	bbc
// 2 125.32.0.0 3355
// 	rb
//	rc
// 2 132.27.1.0 
//      harry
//	harmony
*/

#include "arch.h"

#ifndef	ARCH_PC_WIN95

#include <netdb.h>
#include <sys/param.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>

#else 	/* ARCH_PC_WIN95 */

#include <winsock.h>

#endif	/* ARCH_PC_WIN95 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h> 
#include <assert.h>

#include "configuration.h"

#define ext_conf_body
#include "conf_body.h"
#undef  ext_conf_body

#include "alarm.h"
#include "memory.h"
#include "spread_params.h"

static	proc		My;

/* True means allow dangerous monitor commands like partition and flow control
 * to be handled. 
 * False means to ignore requests for those actions. THIS IS THE SAFE SETTING
 */
static  bool    EnableDangerousMonitor = FALSE;

static  port_reuse SocketPortReuse = port_reuse_auto;

static  char    *RuntimeDir = NULL;

static	char	*User = NULL;

static	char	*Group = NULL;

static  int     Link_Protocol;

static  bool    Conf_Reload_State = FALSE;
static  configuration *Config_Previous;
static  proc    *Config_Previous_Procs;
static  char    Conf_FileName[80];
static  char    Conf_MyName_buf[80];
static  char    *Conf_MyName;

static  int	Conf_prev_proc_by_id( int32u id, proc *p );
static  void    Conf_config_copy( configuration *src_conf, configuration *dst_conf, proc *src_procs, proc *dst_procs);

/* Hash function for string to 32 bit int */
static LOC_INLINE int32u conf_hash_string(const void * key, int32u key_len)
{
    const char * kit  = (const char*) key;
    const char * kend = (const char*) key + key_len;
    int32u    ret  = (int32u) key_len ^ ((int32u) key_len << 8) ^
        ((int32u) key_len << 16) ^ ((int32u) key_len << 24);

    for (; kit != kend; ++kit) {
        ret += *kit;
        ret += (ret << 10);
        ret ^= (ret >> 6);
    }

    ret += (ret << 3);
    ret ^= (ret >> 11);
    ret += (ret << 15);

    return ret;
}



void	Conf_init( char *file_name, char *my_name )
{
        strncpy(Conf_FileName, file_name, 80);
        if (my_name != NULL) {
                strncpy(Conf_MyName_buf, my_name, 80);
                Conf_MyName = &Conf_MyName_buf[0];
        } else {
                Conf_MyName = NULL;
        }

        Config = Mem_alloc( sizeof( configuration ) );
        if (Config == NULL) {
                Alarmp( SPLOG_FATAL, CONF, "Conf_init: Failed to allocate memory for configuration structure\n");
        }
        Config_procs = Mem_alloc( MAX_PROCS_RING * sizeof( proc ) );
        if (Config_procs == NULL) {
                Alarmp( SPLOG_FATAL, CONF, "Conf_init: Failed to allocate memory for configuration procs array\n");
        }

        Conf_load_conf_file( file_name, my_name);
}

bool    Conf_in_reload_state(void)
{
        return(Conf_Reload_State);
}

void    Conf_reload_state_begin(void)
{

        Conf_Reload_State = TRUE;
}

void    Conf_reload_state_end(void)
{

        Conf_Reload_State = FALSE;
}

/* Basic algorithm:
 * 1) copy Config to oldConfig
 * 2) load new spread.conf file into Config
 * 3) Check if we shuold exit;
 * 4) Check if this change is only add/sub or not. Return answer
 */
bool    Conf_reload_initiate(void)
{
        bool    need_partition = FALSE;
        proc    np, op;
        int     i, pi;

        Config_Previous = Config;
        Config_Previous_Procs = Config_procs;

        Config_Previous = Mem_alloc( sizeof( configuration ) );
        if (Config_Previous == NULL) {
                Alarmp( SPLOG_FATAL, CONF, "Conf_reload_initiate: Failed to allocate memory for old configuration structure\n");
        }
        Config_Previous_Procs = Mem_alloc( MAX_PROCS_RING * sizeof( proc ) );
        if (Config_Previous_Procs == NULL) {
                Alarmp( SPLOG_FATAL, CONF, "Conf_reload_initiate: Failed to allocate memory for old configuration procs array\n");
        }

        Conf_config_copy( Config, Config_Previous, Config_procs, Config_Previous_Procs );

        Conf_load_conf_file( Conf_FileName, Conf_MyName );

        /* Exit if:
         *      1) I am no longer in config
         *      2) My IP/Name has changed
         *      3) My broadcast/netmask has changed
         */
        if ( Conf_proc_by_id( My.id, &np ) < 0 ) {
                /* I am no longer in config */
                Alarmp(SPLOG_FATAL, CONF, "Conf_reload_initiate: I (%d.%d.%d.%d) am no longer in config, so exiting.\n", IP1(My.id), IP2(My.id), IP3(My.id), IP4(My.id));
        }
        if ( Conf_prev_proc_by_id( My.id, &op ) < 0 ) {
                Alarmp(SPLOG_FATAL, CONF, "Conf_reload_initiate: BUG! I (%d.%d.%d.%d) am not in previous config, so exiting.\n", IP1(My.id), IP2(My.id), IP3(My.id), IP4(My.id));
        } 

        if ( strncmp( np.name, op.name, MAX_PROC_NAME ) ||
             (np.num_if != op.num_if) ||
             (Config->segments[np.seg_index].bcast_address != Config_Previous->segments[op.seg_index].bcast_address) ) 
        {
                /* My identity has changed so exit */
                Alarmp( SPLOG_FATAL, CONF, "Conf_reload_initiate: My identity has changed: old name (%s), num_if: %d, bcast: %d.%d.%d.%d\t new name (%s), num_if: %d, bcast: %d.%d.%d.%d\n", op.name, op.num_if, IP1(Config_Previous->segments[op.seg_index].bcast_address), IP2(Config_Previous->segments[op.seg_index].bcast_address), IP3(Config_Previous->segments[op.seg_index].bcast_address), IP4(Config_Previous->segments[op.seg_index].bcast_address), np.name, np.num_if, IP1(Config->segments[np.seg_index].bcast_address),  IP2(Config->segments[np.seg_index].bcast_address), IP3(Config->segments[np.seg_index].bcast_address), IP4(Config->segments[np.seg_index].bcast_address) );
        }
        /* Check interfaces are identical */
        for (i = 0 ; i < np.num_if; i++) {
                if ( (np.ifc[i].ip != op.ifc[i].ip) ||
                     (np.ifc[i].port != op.ifc[i].port) ||
                     (np.ifc[i].type != op.ifc[i].type) ) 
                {
                        Alarmp( SPLOG_FATAL, CONF, "Conf_reload_initiate: My interface spec has changed so must exit: old (%d.%d.%d.%d:%d - %d) new (%d.%d.%d.%d:%d - %d)\n", IP1(op.ifc[i].ip),  IP1(op.ifc[i].ip),  IP1(op.ifc[i].ip),  IP1(op.ifc[i].ip), op.ifc[i].port, op.ifc[i].type, IP1(np.ifc[i].ip),  IP1(np.ifc[i].ip),  IP1(np.ifc[i].ip),  IP1(np.ifc[i].ip), np.ifc[i].port, np.ifc[i].type );
                }
        }
        /* Check if only new configuration contains only additions and subtractions of daemons and no changes */
	for ( pi=0; pi < Config->num_total_procs; pi++ )
	{
                np = Config_procs[pi];
                if ( Conf_prev_proc_by_id( np.id, &op ) < 0 ) {
                        Alarmp( SPLOG_INFO, CONF, "Conf_reload_initiate: Config Added daemon at %d.%d.%d.%d \n", IP1(np.id), IP2(np.id), IP3(np.id), IP4(np.id));
                } else {
                        /* compare proc entries to check if identical */
                        if ( strncmp( np.name, op.name, MAX_PROC_NAME ) ||
                             (np.num_if != op.num_if) ||
                             (Config->segments[np.seg_index].bcast_address != Config_Previous->segments[op.seg_index].bcast_address) ) 
                        {
                                need_partition = TRUE;
                                Alarmp( SPLOG_DEBUG, CONF, "Conf_reload_initiate: identity of daemon %d.%d.%d.%d has changed: old name (%s), num_if: %d, bcast: %d.%d.%d.%d\t new name (%s), num_if: %d, bcast: %d.%d.%d.%d\n", IP1(np.id), IP2(np.id), IP3(np.id), IP4(np.id), op.name, op.num_if, IP1(Config_Previous->segments[op.seg_index].bcast_address), IP2(Config_Previous->segments[op.seg_index].bcast_address), IP3(Config_Previous->segments[op.seg_index].bcast_address), IP4(Config_Previous->segments[op.seg_index].bcast_address), np.name, np.num_if, IP1(Config->segments[np.seg_index].bcast_address),  IP2(Config->segments[np.seg_index].bcast_address), IP3(Config->segments[np.seg_index].bcast_address), IP4(Config->segments[np.seg_index].bcast_address) );
                        }
                        /* Check interfaces are identical */
                        for (i = 0 ; i < np.num_if; i++) {
                                if ( (np.ifc[i].ip != op.ifc[i].ip) ||
                                     (np.ifc[i].port != op.ifc[i].port) ||
                                     (np.ifc[i].type != op.ifc[i].type) ) 
                                {
                                        need_partition = TRUE;
                                        Alarmp( SPLOG_DEBUG, CONF, "Conf_reload_initiate: daemon interface spec for %d.%d.%d.%d has changed.: old (%d.%d.%d.%d:%d - %d) new (%d.%d.%d.%d:%d - %d)\n", IP1(np.id), IP2(np.id), IP3(np.id), IP4(np.id), IP1(op.ifc[i].ip),  IP1(op.ifc[i].ip),  IP1(op.ifc[i].ip),  IP1(op.ifc[i].ip), op.ifc[i].port, op.ifc[i].type, IP1(np.ifc[i].ip),  IP1(np.ifc[i].ip),  IP1(np.ifc[i].ip),  IP1(np.ifc[i].ip), np.ifc[i].port, np.ifc[i].type );
                                }
                        }
                } /* else */
	} /* for */

        /* free old config structs and arrays since they will never be used again */
        dispose( Config_Previous );
        dispose( Config_Previous_Procs );

        Config_Previous = NULL;
        Config_Previous_Procs = NULL;
        
        Alarmp( SPLOG_DEBUG, CONF, "Conf_reload_initiate: Return need_partition = %d\n", need_partition);
        return(need_partition);
}

/* conf_convert_version_to_string()
 * char * segstr : output string
 * int strsize : length of output string space
 * int return : length of string written or -1 if error (like string not have room)
 * 
 *
 * The string will have appended to it the spread version number in the following format.
 *
 * "Version: 3.17.3\n"
 *
 */
static  int    conf_convert_version_to_string(char *segstr, int strsize)
{
    size_t      curlen = 0;
    char        temp_str[30];

    sprintf(temp_str, "Version: %d.%d", 
            SP_MAJOR_VERSION,
            SP_MINOR_VERSION );

    strncat( segstr, temp_str, strsize - curlen);
    curlen += strlen(temp_str);

    /* terminate each segment by a newline */
    strncat( segstr, "\n", strsize - curlen);
    curlen += 1;

    if (curlen > strsize) {
        /* ran out of space in string -- should never happen. */
        Alarmp( SPLOG_ERROR, CONF, "The conf hash string is too long! %d characters attemped is more then %d characters allowed", curlen, strsize);
        Alarmp( SPLOG_ERROR, CONF, "The error occured when adding the version number. Successful string was: %s\n", segstr);
        return(-1);
    }

    Alarmp( SPLOG_DEBUG, CONF, "The version string is %d characters long:\n%s", curlen, segstr);
    return(curlen);
}

void	Conf_load_conf_file( char *file_name, char *my_name )
{
        struct hostent  *host_ptr;
	char	machine_name[256];
	char	ip[16];
	int	i,j;
        int     added_len;
        unsigned int name_len;
        char    configfile_location[MAXPATHLEN];

        /* Initialize hash string */
        ConfStringRep[0] = '\0';
        ConfStringLen = 0;

	/* init Config, Config_procs from file
	   init My from host
	 */
        configfile_location[0] = '\0';
        strcat(configfile_location, SPREAD_ETCDIR);
        strcat(configfile_location, "/spread.conf");

	if (NULL != (yyin = fopen(file_name,"r")) )
                Alarm( PRINT, "Conf_load_conf_file: using file: %s\n", file_name);
	if (yyin == NULL) 
		if (NULL != (yyin = fopen("./spread.conf", "r")) )
                        Alarm( PRINT, "Conf_load_conf_file: using file: ./spread.conf\n");
	if (yyin == NULL)
		if (NULL != (yyin = fopen(configfile_location, "r")) )
                        Alarm( PRINT, "Conf_load_conf_file: using file: %s\n", configfile_location);
	if (yyin == NULL)
		Alarm( EXIT, "Conf_load_conf_file: error opening config file %s\n",
			file_name);

        /* reinitialize all the variables in the yacc parser */
        parser_init();
        
	yyparse();

        fclose(yyin);

        /* Test for localhost segemnt defined with other non-localhost segments.
         * That is an invalid configuration 
         */
        if ( Config->num_segments > 1 ) {
            int found_localhost = 0;
            int found_nonlocal = 0;
            for ( i=0; i < Config->num_segments; i++) {
                if ( ((Config->segments[i].bcast_address & 0xff000000) >> 24) == 127 ) {
                    found_localhost = 1;
                } else {
                    found_nonlocal = 1;
                }
            }
            if (found_nonlocal && found_localhost) {
                /* Both localhost and non-localhost segments exist. This is a non-functional config.*/
                Alarmp( SPLOG_PRINT, PRINT, "Conf_load_conf_file: Invalid configuration:\n");
                Conf_print( Config );
                Alarmp( SPLOG_PRINT, PRINT, "\n");
                Alarmp( SPLOG_FATAL, CONF, "Conf_load_conf_file: Localhost segments can not be used along with regular network address segments.\nMost likely you need to remove or comment out the \nSpread_Segment 127.0.0.255 {...}\n section of your configuration file.\n");
            }
        }

        /* Add Spread daemon version number to hash string */
        added_len = conf_convert_version_to_string(&ConfStringRep[ConfStringLen], MAX_CONF_STRING - ConfStringLen );
        if (added_len == -1 )
            Alarmp( SPLOG_FATAL, CONF, "Failed to update string with version number!\n");
        ConfStringLen += added_len;

        /* calculate hash value of configuration. 
         * This daemon will only work with other daemons who have an identical hash value.
         */
        Config->hash_code = conf_hash_string(ConfStringRep, ConfStringLen);
        Alarmp( SPLOG_DEBUG, CONF, "Full hash string is %d characters long:\n%s", ConfStringLen, ConfStringRep);
        Alarmp( SPLOG_INFO, CONF, "Hash value for this configuration is: %u\n", Config->hash_code);

        /* Match my IP address to entry in configuration file */
	if( my_name == NULL ){
		gethostname(machine_name,sizeof(machine_name)); 
		host_ptr = gethostbyname(machine_name);
		if( host_ptr == 0 )
			Alarm( EXIT, "Conf_load_conf_file: could not get my ip address (my name is %s)\n",
				machine_name );
                if (host_ptr->h_addrtype != AF_INET)
                        Alarm(EXIT, "Conf_load_conf_file: Sorry, cannot handle addr types other than IPv4\n");
                if (host_ptr->h_length != 4)
                        Alarm(EXIT, "Conf_load_conf_file: Bad IPv4 address length\n");
	
		i = -1;	/* in case host_ptr->h_length == 0 */
                for (j = 0; host_ptr->h_addr_list[j] != NULL; j++) {
                        memcpy(&My.id, host_ptr->h_addr_list[j], sizeof(struct in_addr));
			My.id = ntohl( My.id );
			i = Conf_proc_by_id( My.id, &My );
			if( i >= 0 ) break;
                }
		if( i < 0 ) Alarm( EXIT,
			"Conf_load_conf_file: My proc id (%d.%d.%d.%d) is not in configuration\n", IP1(My.id),IP2(My.id),IP3(My.id),IP4(My.id) );

	}else if( ! strcmp( my_name, "Monitor" ) ){
		gethostname(machine_name,sizeof(machine_name)); 
		host_ptr = gethostbyname(machine_name);

		if( host_ptr == 0 )
			Alarm( EXIT, "Conf_load_conf_file: no such monitor host %s\n",
				machine_name );

        	memcpy(&My.id, host_ptr->h_addr_list[0], 
			sizeof(int32) );
		My.id = ntohl( My.id );

		name_len = strlen( machine_name );
		if( name_len > sizeof(My.name) ) name_len = sizeof(My.name);
		memcpy(My.name, machine_name, name_len );
		Alarm( CONF, "Conf_load_conf_file: My name: %s, id: %d\n",
			My.name, My.id );
		return;
	}else{
		name_len = strlen( my_name );
		if( name_len > sizeof(My.name) ) name_len = sizeof(My.name);
		memcpy(My.name, my_name, name_len );
		i = Conf_proc_by_name( My.name, &My );
		if( i < 0  ) Alarm( EXIT,
				"Conf_load_conf_file: My proc %s is not in configuration \n",
				My.name);

	}

	Conf_id_to_str( My.id, ip );
	Alarm( CONF, "Conf_load_conf_file: My name: %s, id: %s, port: %hd\n",
		My.name, ip, My.port );

	return;
}

configuration	Conf()
{
	return *Config;
}

configuration   *Conf_ref(void)
{
        return Config;
}

proc	Conf_my()
{
	return	My;
}

void    Conf_set_link_protocol(int protocol)
{
        if (protocol < 0 || protocol >= MAX_PROTOCOLS) {
                Alarm(PRINT, "Conf_set_link_protocol: Illegal protocol type %d\n", protocol);
                return;
        }
        Link_Protocol = protocol;
}

int     Conf_get_link_protocol(void)
{
        return(Link_Protocol);
}


int	Conf_proc_by_id( int32u id, proc *p )
{
	int	i,j;

	for ( i=0; i < Config->num_total_procs; i++ )
	{
                for ( j=0; j < Config_procs[i].num_if; j++)
                {
                        if ( Config_procs[i].ifc[j].ip == id )
                        {
                                *p =  Config_procs[i] ;
                                return( i );
                        }
                }
	}
	return( -1 );
}

int 	Conf_proc_by_name( char *name, proc *p )
{
	int	i;

	for ( i=0; i < Config->num_total_procs; i++ )
	{
		if ( strcmp( Config_procs[i].name, name ) == 0 )
		{
			*p = Config_procs[i];
			return( i );
		}
	}
	return( -1 );
}

int	Conf_id_in_seg( segment *seg, int32u id )
{
	int 	i,j;

	for ( j=0; j < seg->num_procs; j++ )
	{
                for ( i=0; i < seg->procs[j]->num_if; i++)
                {
                        if ( seg->procs[j]->ifc[i].ip == id )
                                return( j );
                }
	}
	return( -1 );
}
static  int     Conf_config_lookup_id( configuration *conf, proc *procs, int32u id)
{
	int	i,j;

	for ( i=0; i < conf->num_total_procs; i++ )
	{
                for ( j=0; j < procs[i].num_if; j++)
                {
                        if ( procs[i].ifc[j].ip == id )
                        {
                                return( i );
                        }
                }
	}
	return( -1 );
}

static  void    Conf_config_copy( configuration *src_conf, configuration *dst_conf, proc *src_procs, proc *dst_procs)
{
    int i,j,p_index;

    *dst_conf = *src_conf;

    for (i=0; i < src_conf->num_total_procs; i++ )
    {
        dst_procs[i] = src_procs[i];
    }

    for (i=0; i < src_conf->num_segments; i++ )
    {
        for ( j=0; j < src_conf->segments[i].num_procs; j++ )
        {
            p_index = Conf_config_lookup_id(dst_conf, dst_procs, src_conf->segments[i].procs[j]->id);
            assert(p_index != -1);
            dst_conf->segments[i].procs[j] = &dst_procs[p_index];
        }
    }        

#ifndef NDEBUG
    /* Verify correct state after copy */
    for (i=0; i < dst_conf->num_segments; i++ )
    {
        for ( j=0; j < dst_conf->segments[i].num_procs; j++ )
        {
            assert( dst_conf->segments[i].procs[j]->id == src_conf->segments[i].procs[j]->id );
        }
    }
#endif
}

static  int     Conf_proc_ref_by_id( int32u id, proc **p )
{
	int	i,j;

	for ( i=0; i < Config->num_total_procs; i++ )
	{
                for ( j=0; j < Config_procs[i].num_if; j++)
                {
                        if ( Config_procs[i].ifc[j].ip == id )
                        {
                                *p = &Config_procs[i];
                                return( i );
                        }
                }
	}
	return( -1 );
}

static  int	Conf_prev_proc_by_id( int32u id, proc *p )
{
	int	i,j;

	for ( i=0; i < Config_Previous->num_total_procs; i++ )
	{
                for ( j=0; j < Config_Previous_Procs[i].num_if; j++)
                {
                        if ( Config_Previous_Procs[i].ifc[j].ip == id )
                        {
                                *p =  Config_Previous_Procs[i] ;
                                return( i );
                        }
                }
	}
	return( -1 );
}

int     Conf_append_id_to_seg( segment *seg, int32u id)
{
        proc *p;
        if (Conf_proc_ref_by_id(id, &p) != -1)
        {
                seg->procs[seg->num_procs] = p;
                seg->num_procs++;
                return( 0 );
        } 
        return( -1 );
}
int	Conf_id_in_conf( configuration *config, int32u id )
{
	int 	i;

	for ( i=0; i < config->num_segments; i++ )
                if ( Conf_id_in_seg(&(config->segments[i]), id) >= 0 )
                        return( i );
	return( -1 );
}

int	Conf_num_procs( configuration *config )
{
	int 	i,ret;

	ret = 0;
	for ( i=0; i < config->num_segments; i++ )
		ret += config->segments[i].num_procs;

	return( ret );
}

int     Conf_num_segments( configuration *config )
{
        return( config->num_segments );
}

int32u	Conf_leader( configuration *config )
{
        int i;

        for( i=0; i < config->num_segments; i++ )
        {
                if( config->segments[i].num_procs > 0 )
                        return( config->segments[i].procs[0]->id );
        }
        Alarm( EXIT, "Conf_leader: Empty configuration %c",Conf_print(config));
	return( -1 );
}

int32u	Conf_last( configuration *config )
{
        int i,j;

        for( i = config->num_segments-1; i >= 0; i-- )
        {
                if( config->segments[i].num_procs > 0 )
		{
			j = config->segments[i].num_procs-1;
                        return( config->segments[i].procs[j]->id );
		}
        }
        Alarm( EXIT, "Conf_last: Empty configuration %c",Conf_print(config));
	return( -1 );
}

int32u	Conf_seg_leader( configuration *config, int16 seg_index )
{
	if( config->segments[seg_index].num_procs > 0 )
	{
		return( config->segments[seg_index].procs[0]->id );
	}
        Alarm( EXIT, "Conf_seg_leader: Empty segment %d in Conf %c",
		seg_index, Conf_print(config));
	return( -1 );
}

int32u	Conf_seg_last( configuration *config, int16 seg_index )
{
	int	j;

	if( config->segments[seg_index].num_procs > 0 )
	{
		j = config->segments[seg_index].num_procs-1;
		return( config->segments[seg_index].procs[j]->id );
	}
        Alarm( EXIT, "Conf_seg_leader: Empty segment %d in Conf %c",
		seg_index, Conf_print(config));
        return(-1);
}

int	Conf_num_procs_in_seg( configuration *config, int16 seg_index )
{
	return( config->segments[seg_index].num_procs );
}

void	Conf_id_to_str( int32u id, char *str )
{
	int32u	i1,i2,i3,i4;

	i1 = (id & 0xff000000) >> 24;
	i2 = (id & 0x00ff0000) >> 16;
	i3 = (id & 0x0000ff00) >> 8;
	i4 = (id & 0x000000ff);
	sprintf( str, "%u.%u.%u.%u", i1, i2, i3, i4 );
}

char	Conf_print(configuration *config)
{
	int 	s,p,ret;
	char	ip[16];
	proc	pr;

	Alarm( PRINT, "--------------------\n" );
	Alarm( PRINT, "Configuration at %s is:\n", My.name );
	Alarm( PRINT, "Num Segments %d\n",config->num_segments );
	for ( s=0; s < config->num_segments; s++ )
	{
		Conf_id_to_str( config->segments[s].bcast_address, ip );
		Alarm( PRINT, "\t%d\t%-16s  %hd\n",
			config->segments[s].num_procs, ip,
			config->segments[s].port );
		for( p=0; p < config->segments[s].num_procs; p++)
		{
			ret = Conf_proc_by_id( config->segments[s].procs[p]->id,
					&pr );	
			Conf_id_to_str( pr.id, ip );
			Alarm( PRINT, "\t\t%-20s\t%-16s\n", pr.name, ip );
		}
	}
	Alarm( PRINT, "====================" );
	return( '\n' );
}

bool    Conf_get_dangerous_monitor_state(void)
{
        return(EnableDangerousMonitor);
}

void    Conf_set_dangerous_monitor_state(bool new_state)
{
        if (new_state == FALSE) {
                Alarm(PRINT, "disabling Dangerous Monitor Commands!\n");
        } else if (new_state == TRUE) {
                Alarm(PRINT, "ENABLING Dangerous Monitor Commands! Make sure Spread network is secured\n");
        } else {
                /* invalid setting */
                return;
        }
        EnableDangerousMonitor = new_state;
}

port_reuse Conf_get_port_reuse_type(void)
{
        return(SocketPortReuse);
}

void    Conf_set_port_reuse_type(port_reuse state)
{
        switch (state)
        {
        case port_reuse_auto:
                Alarm(PRINT, "Setting SO_REUSEADDR to auto\n");
                break;
        case port_reuse_on:
                Alarm(PRINT, "Setting SO_REUSEADDR to always on -- make sure Spread daemon host is secured!\n");
                break;
        case port_reuse_off:
                Alarm(PRINT, "Setting SO_REUSEADDR to always off\n");
                break;
        default:
                /* Inavlid type -- ignored */
                return;
        }
        SocketPortReuse = state;
}

static void set_param_if_valid(char **param, char *value, char *description, unsigned int max_value_len)
{
        if (value != NULL && *value != '\0')
        {
                unsigned int len = strlen(value);
                char *old_value = *param;
                char *buf;
                if (len > max_value_len)
                {
                    Alarm(EXIT, "set_param_if_valid: value string too long\n");
                }
                buf = Mem_alloc(len + 1);
                if (buf == NULL)
                {
                        Alarm(EXIT, "set_param_if_valid: Out of memory\n");
                }
                strncpy(buf, value, len);
                buf[len] = '\0';

                *param = buf;
                if (old_value != NULL)
                {
                    dispose(old_value);
                }
                Alarm(PRINT, "Set %s to '%s'\n", description, value);
        }
        else
        {
                Alarm(DEBUG, "Ignored invalid %s\n", description);
        }
}

char    *Conf_get_runtime_dir(void)
{
        return (RuntimeDir != NULL ? RuntimeDir : SP_RUNTIME_DIR);
}

void    Conf_set_runtime_dir(char *dir)
{
        set_param_if_valid(&RuntimeDir, dir, "runtime directory", MAXPATHLEN);
}

char    *Conf_get_user(void)
{
        return (User != NULL ? User : SP_USER);
}

void    Conf_set_user(char *user)
{
        set_param_if_valid(&User, user, "user name", 32);
}

char    *Conf_get_group(void)
{
        return (Group != NULL ? Group : SP_GROUP);
}

void    Conf_set_group(char *group)
{
        set_param_if_valid(&Group, group, "group name", 32);
}
