#pragma once
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <errno.h>

#define F fflush(stdout)
#define MS 300
#define KEY_LENGTH 256

#define arcfour_decrypt(x,y,z) arcfour_encrypt(x,y,z)
#define arcfour_destroy(x) free(x)
#define arcfour_white_wash(x,y)  for(x=0; x<(MS * 1000000); x++) \
                                    (volatile string8) arcfour_byte(y);  // 'volatile' -> AVOID OPTIMIZATION


typedef unsigned char string8; // 8 bit STRING
typedef unsigned short int16; // 16 bit
typedef unsigned int int32; // 32 bit

struct s_arcfour {
    string8 i, j, k;
    string8 s[KEY_LENGTH];
};

// Initialize RC4 structure with a key
struct s_arcfour *arcfour_init(string8 *key, int16 key_len);

// Generate next byte of RC4 stream
string8 arcfour_byte(struct s_arcfour *p);

// Encrypt/decrypt a buffer,MUST take the current s_arcfour state/pointer
void arcfour_encrypt(string8 *data, int16 len, struct s_arcfour *p);

void print_bin(const string8 *input, int16 size);

void bin_to_hex(const string8 *in, int16 len, char *out);
void hex_to_bin(const char *in, string8 *out, int16 len);

