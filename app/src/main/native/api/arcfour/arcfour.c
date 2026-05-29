#include "arcfour.h"
#include <android/log.h>

// Initialize RC4 state with the given key
struct s_arcfour *arcfour_init(string8 *key, int16 key_len) {
    struct s_arcfour *p;
    int16 x;
    string8 tmp;
    int32 n;

    p = malloc(sizeof(struct s_arcfour));
    assert(p != NULL);

    // Initialize state array to 0..255
    for (x = 0; x < KEY_LENGTH; x++)
        p->s[x] = x;

    p->i = p->j = p->k = 0;

    // Key-scheduling algorithm (KSA)
    for (x = 0; x < KEY_LENGTH; x++) {
        p->j = (p->j + p->s[x] + key[x % key_len]) % KEY_LENGTH;
        tmp = p->s[x];
        p->s[x] = p->s[p->j];
        p->s[p->j] = tmp;
    }

    p->i = p->j = 0;
    arcfour_white_wash(n,p);
    return p;
}

// Generate next byte of RC4 stream
string8 arcfour_byte(struct s_arcfour *p) {
    p->i = (p->i + 1) % KEY_LENGTH;
    p->j = (p->j + p->s[p->i]) % KEY_LENGTH;
    string8 tmp = p->s[p->i];
    p->s[p->i] = p->s[p->j];
    p->s[p->j] = tmp;
    return p->s[(p->s[p->i] + p->s[p->j]) % KEY_LENGTH];
}

// Encrypt/decrypt buffer
void arcfour_encrypt(string8 *data, const int16 len, struct s_arcfour *p) {
    for (int16 x = 0; x < len; x++)
        data[x] = data[x] ^ arcfour_byte(p);  // XOR operator
}

// Print buffer in hex with space every 2 bytes
void print_bin(const string8 *input, const int16 size) {
    for (int16 i = 0; i < size; i++) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR,"ARCFOUR-LOG","%02x ", input[i]);
#endif

#ifdef __WIN32
        printf("%02x ", input[i]);  // aa bb cc dd
#endif
    }
    F; // Flush
}

void bin_to_hex(const string8 *in, int16 len, char *out) {
    for (int16 i = 0; i < len; i++)
        sprintf(out + (i * 2), "%02x", in[i]);
    out[len * 2] = '\0';
}

void hex_to_bin(const char *in, string8 *out, int16 len) {
    for (int16 i = 0; i < len; i++) {
        sscanf(in + 2*i, "%2hhx", &out[i]);
    }
}
