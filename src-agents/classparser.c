#include "common.h"
#include "classparser.h"

#include <assert.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>

//

#ifdef MINGW

#include <ws2tcpip.h>

#else

#include <arpa/inet.h>

#endif

#define read_be16(value) ntohs(value)
#define read_be32(value) ntohl(value)

//

#define CLASS_MAGIC 0xCAFEBABE

struct java_class {
	// Class bytecode.
	const uint8_t * bytes;

	// Size of the class bytecode.
	size_t size;

	// Constant pool size in bytes.
	size_t cp_size;

	// Constant pool entry offsets.
	size_t cp_offsets [];
};



enum cp_tag {
	CP_TAG_UTF8 = 1,
	CP_TAG_LONG = 5,
	CP_TAG_DOUBLE = 6,
	CP_TAG_CLASS = 7,
	CP_TAG_STRING = 8,
	CP_TAG_METHOD_HANDLE = 15,
	CP_TAG_METHOD_TYPE = 16,
};


struct PACKED cp_info {
	uint8_t tag;

	union PACKED {
		struct PACKED cp_info_utf8 {
			uint16_t length;
			uint8_t bytes [];
		} utf8;

		struct PACKED cp_info_class {
			uint16_t name_index;
		} class;

		struct PACKED cp_info_method_handle {
			uint8_t reference_kind;
			uint16_t reference_index;
		} method_handle;

		struct PACKED cp_info_long_double {
			uint32_t high_bytes;
			uint32_t low_bytes;
		} long_double;
	};
};

//

struct PACKED class_file_start {
	uint32_t magic;
	uint16_t minor_version;
	uint16_t major_version;
	uint16_t constant_pool_count;
	struct cp_info constant_pool []; // [constant_pool_count - 1]
};

struct PACKED class_file_access {
	uint16_t access_flags;
	uint16_t this_class;
	uint16_t super_class;
	uint16_t interfaces_count;
	uint16_t interfaces []; // [interfaces_count]
};

//

static inline struct class_file_start *
__class_file_start (const uint8_t * class_bytes) {
	return (struct class_file_start *) class_bytes;
}


static inline struct class_file_access *
__class_file_access (class_t java_class) {
	int offset = sizeof (struct class_file_start) + java_class->cp_size;
	return (struct class_file_access *) (java_class->bytes + offset);
}

//

static inline bool
__class_bytes_valid (const uint8_t * class_bytes) {
	return read_be32 (__class_file_start (class_bytes)->magic) == CLASS_MAGIC;
}


static inline int
__cp_entry_count (const uint8_t * class_bytes) {
	return read_be16 (__class_file_start (class_bytes)->constant_pool_count);
}

//

static inline int
__class_this_class_index (class_t java_class) {
	return read_be16 (__class_file_access (java_class)->this_class);
}


static inline int
__class_super_class_index (class_t java_class) {
	return read_be16 (__class_file_access (java_class)->super_class);
}


static inline int
__class_interface_count (class_t java_class) {
	return read_be16 (__class_file_access (java_class)->interfaces_count);
}


static inline int
__class_interface_index (class_t java_class, int index) {
	return read_be16 (__class_file_access (java_class)->interfaces [index]);
}

//

static inline struct cp_info *
__cp_entry (const uint8_t * class_bytes, int offset) {
	const uint8_t * cp_base = class_bytes + offsetof (struct class_file_start, constant_pool);
	return (struct cp_info *) (cp_base + offset);
}


static struct cp_info *
__class_cp_get_entry (class_t java_class, int index) {
	if (index >= 1 && index < __cp_entry_count (java_class->bytes)) {
		int offset = java_class->cp_offsets [index];
		return __cp_entry (java_class->bytes, offset);

	} else {
		warn ("invalid const pool index: %d\n", index);
		return NULL;
	}
}

//

static int
__cp_scan_entries (const uint8_t * class_bytes, size_t * offsets) {
	uint_fast32_t offset = 0;
	uint_fast16_t count = __cp_entry_count (class_bytes);

	for (uint_fast16_t index = 1; index < count; index++) {
		offsets [index] = offset;

		struct cp_info * entry = __cp_entry (class_bytes, offset);
		switch (entry->tag) {

		case CP_TAG_UTF8: {
			uint_fast16_t length = read_be16 (entry->utf8.length);
			offset += sizeof (struct cp_info_utf8) + length;
			break;
		}

		case CP_TAG_CLASS:
		case CP_TAG_STRING:
		case CP_TAG_METHOD_TYPE:
			// Indices into constant pool.
			offset += sizeof (uint16_t);
			break;

		case CP_TAG_METHOD_HANDLE:
			offset += sizeof (struct cp_info_method_handle);
			break;

		case CP_TAG_LONG:
		case CP_TAG_DOUBLE:
			// 64-bit values take an extra constant pool slot.
			index++;

			offset += sizeof (struct cp_info_long_double);
			break;

		default:
			// All other entries fit into 4 bytes.
			offset += 2 * sizeof (uint16_t);
		}

		// Account for the type tag.
		offset += sizeof_member (struct cp_info, tag);
	}

	return offset;
}


//

static inline struct java_class *
__class_alloc (const uint8_t * class_bytes) {
	int const_count = 1 + __cp_entry_count (class_bytes);
	int offsets_size = const_count * sizeof_member (struct java_class, cp_offsets[0]);
	return (struct java_class *) malloc (sizeof (struct java_class) + offsets_size);
}


static inline void
__class_init (struct java_class * java_class, const uint8_t * class_bytes, size_t byte_count) {
	java_class->bytes = class_bytes;
	java_class->size = byte_count;
	java_class->cp_size = __cp_scan_entries (class_bytes, &java_class->cp_offsets[0]);
}


class_t
class_alloc (const uint8_t * class_bytes, size_t byte_count) {
	assert (byte_count > sizeof (struct class_file_start));
	assert (class_bytes != NULL);

	if (__class_bytes_valid (class_bytes)) {
		struct java_class * result = __class_alloc (class_bytes);
		if (result != NULL) {
			__class_init (result, class_bytes, byte_count);
			return result;

		} else {
			warn ("failed to allocate java_class structure");
		}
	} else {
		warn ("invalid class bytecode passed to java_class_alloc is invalid");
	}

	return NULL;
}

//

static utf8_t
class_cp_get_utf8 (class_t java_class, int index, size_t * utf8_length) {
	assert (java_class != NULL);

	struct cp_info * entry = __class_cp_get_entry (java_class, index);
	if (entry != NULL) {
		if (entry->tag == CP_TAG_UTF8) {
			if (utf8_length != NULL) {
				*utf8_length = read_be16 (entry->utf8.length);
			}

			return &entry->utf8.bytes [0];

		} else {
			warn ("invalid const tag, expected %d, found %d\n", CP_TAG_UTF8, entry->tag);
		}
	}

	return NULL;
}


static utf8_t
class_cp_get_class_name (
	class_t java_class, int class_index, size_t * utf8_length
) {
	struct cp_info * entry = __class_cp_get_entry (java_class, class_index);
	if (entry != NULL) {
		if (entry->tag == CP_TAG_CLASS) {
			int index = read_be16 (entry->class.name_index);
			return class_cp_get_utf8 (java_class, index, utf8_length);

		} else {
			warn ("invalid const tag, expected %d, found %d\n", CP_TAG_CLASS, entry->tag);
		}
	}

	return NULL;
}

//

static inline char *
__utf8dup (utf8_t bytes, size_t length) {
	size_t len = length + 1;
	char * result = (char *) malloc (len);
	if (result != NULL) {
		strncpy (result, (const char *) bytes, length);
		result [length] = '\0';
	}

	return result;
}


char *
class_name (class_t java_class) {
	assert (java_class != NULL);

	int class_index = __class_this_class_index (java_class);

	size_t length = 0;
	utf8_t bytes = class_cp_get_class_name (java_class, class_index, &length);
	return __utf8dup (bytes, length);
}


char *
class_super_class_name (class_t java_class) {
	assert (java_class != NULL);

	int class_index = __class_super_class_index (java_class);
	if (class_index != 0) {
		size_t length = 0;
		utf8_t bytes = class_cp_get_class_name (java_class, class_index, &length);
		return __utf8dup (bytes, length);

	} else {
		return NULL;
	}
}


bool
class_has_super_class (class_t java_class) {
	assert (java_class != NULL);
	return __class_super_class_index (java_class) > 1;
}


int
class_interface_count (class_t java_class) {
	assert (java_class != NULL);
	return __class_interface_count (java_class);
}


char *
class_interface_name (class_t java_class, int index) {
	assert (java_class != NULL);

	int count = __class_interface_count (java_class);
	if (0 <= index && index < count) {
		int iface_index = __class_interface_index (java_class, index);

		size_t length = 0;
		utf8_t bytes = class_cp_get_class_name (java_class, iface_index, &length);
		return __utf8dup (bytes, length);

	} else {
		warn ("requested interface index out of range: %d\n", index);
		return NULL;
	}
}


void
class_free (class_t java_class) {
	assert (java_class != NULL);

	free ((void *) java_class);
}
