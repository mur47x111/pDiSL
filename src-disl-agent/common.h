#ifndef _COMMON_H_
#define _COMMON_H_

#define _POSIX_C_SOURCE 200809L

#include <stdio.h>
#include <stdbool.h>
#include <errno.h>

#define __STDC_FORMAT_MACROS
#include <inttypes.h>

#ifdef MINGW
#include <winsock2.h>
#include <windows.h>
#endif


/**
 * Compiler attributes.
 */
#define PACKED __attribute__ ((__packed__))

#ifdef WHOLE
#define VISIBLE __attribute__ ((externally_visible))
#else
#define VISIBLE
#endif



/**
 * Returns the size of an array in array elements.
 */
#define sizeof_array(array) \
	(sizeof (array) / sizeof ((array) [0]))


/**
 * Returns the size of a structure member.
 */
#define sizeof_member(type, member) \
	(sizeof (((type *) 0)->member))


/**
 * Prints a debug message to stdout, unless NDEBUG is defined.
 *
 * @format	format string for printf
 * @args	arguments associated with the format string
 *
 */
#ifdef NDEBUG
#  define dprintf(args...) do {} while (0)
#else
#  define dprintf(args...) fprintf (stdout, args); fflush (stdout)
#endif


#define dlprintf(args...) { \
	dprintf ("%s:%d: ", __FUNCTION__, __LINE__); \
	dprintf (args); \
}


#define dlwrap(code) { \
	dlprintf (""); \
	code; \
	dprintf ("\n"); \
}

//

#define warn(args...) { \
	fprintf (stderr, "warning: "); \
	fprintf (stderr, args); \
}

//

int find_value_index (const char * strval, const char * values [], int nvals);

//

#define ERROR_GENERIC 10000
#define ERROR_SERVER 10003
#define ERROR_STD 10002
#define ERROR_JVMTI 10003

#define ERROR_PREFIX "disl-agent: error: "

//

void die_with_error (const char * message);
void die_with_std_error (const char * message, int errnum);


/**
 * Reports a general error and terminates the program if the provided
 * error condition is true.
 */
static inline void
check_error (bool error, const char * message) {
	if (error) {
		die_with_error (message);
	}
}


/**
 * Reports a standard library error and terminates the program if the provided
 * error condition is true.
 */
static inline void
check_std_error (bool error, const char * message) {
	if (error) {
		die_with_std_error (message, errno);
	}
}


/**
 * Warns about a standard library error if the provided
 * error condition is true.
 */
static inline void
warn_std_error (bool error, const char * message) {
	if (error) {
		warn (message, errno);
	}
}

//

#ifdef MINGW

void die_with_win_error (const char * message, DWORD errnum);


/**
 * Reports a windows error and terminates the program if the provided
 * error condition is true.
 */
static inline void
check_win_error (bool error, const char * message) {
	if (error) {
		die_with_win_error (message, GetLastError ());
	}
}

#endif /* MINGW */


#endif /* _COMMON_H_ */
