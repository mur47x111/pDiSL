#ifndef _CLASSPARSER_H
#define _CLASSPARSER_H

#include <stdint.h>
#include <stdbool.h>

struct java_class;
typedef const struct java_class * class_t;
typedef const uint8_t * utf8_t;

//

class_t class_alloc (const uint8_t * class_bytes, size_t byte_count);
void class_free (class_t java_class);

char * class_name (class_t java_class);
char * class_super_class_name (class_t java_class);
bool class_has_super_class (class_t java_class);

int class_interface_count (class_t java_class);
char * class_interface_name (class_t java_class, int index);

#endif /* _CLASSPARSER_H */

