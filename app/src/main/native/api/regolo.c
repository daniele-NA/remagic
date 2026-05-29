#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <curl/curl.h>

#include <jni.h>
#include <string.h>
#include "regolo.h"


char *llm(char *prompt,char *regolo_key) {

    char *url = "https://api.regolo.ai/v1/completions";

    CURL *curl;
    CURLcode res;
    char *response_data = malloc(2048); // Heap
    response_data[0] = '\0';


    curl_global_init(CURL_GLOBAL_DEFAULT);
    curl = curl_easy_init();

    if (curl) {
        curl_easy_setopt(curl, CURLOPT_URL, url);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_callback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, response_data); // Passa il buffer alla callback

        set_headers(curl, regolo_key);
        set_data(curl, prompt);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0L);


        res = curl_easy_perform(curl);

        if (res != CURLE_OK) {
            fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
            return NULL;
        }
        curl_easy_cleanup(curl);


        const char *start = strstr(response_data, "\"text\":\"");
        if (start) {
            start += 8; // lunghezza di "\"text\":\""
            const char *end = strchr(start, '!');
            if (!end) end = strchr(start, '"');
            if (end) {
                int len = end - start;
                char *output = malloc(len + 1);
                strncpy(output, start, len);
                output[len] = '\0';
                return output;
            }
        }


        return "Hello";
    }

    curl_global_cleanup();


    return NULL;
}


size_t write_callback(void *contents, size_t size, size_t nmemb, void *user_pt) {
    size_t total_size = size * nmemb;
    strncat(user_pt, contents, total_size);
    return total_size;
}

void set_headers(CURL *curl, const char *api_key) {
    struct curl_slist *headers = NULL;
    char header[256]; //Dimensione header
    // Authorization
    snprintf(header, sizeof(header), "Authorization: Bearer %s", api_key);
    headers = curl_slist_append(headers, header);

    // Content-Type
    snprintf(header, sizeof(header), "Content-Type: application/json");
    headers = curl_slist_append(headers, header);

    // Cache-Control
    snprintf(header, sizeof(header), "Cache-Control: no-cache, no-store, must-revalidate");
    headers = curl_slist_append(headers, header);

    // Pragma
    snprintf(header, sizeof(header), "Pragma: no-cache");
    headers = curl_slist_append(headers, header);

    // Expires
    snprintf(header, sizeof(header), "Expires: 0");
    headers = curl_slist_append(headers, header);

    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
}

void set_data(CURL *curl, char *prompt) {
    const char *first_part = "{\"model\": \"Llama-3.1-8B-Instruct\", \"prompt\":\"";
    const char *second_part = "\", \"max_tokens\": 200, \"temperature\": 0.2}";

    char *full_prompt = calloc((strlen(first_part) + strlen(prompt) + strlen(second_part)),
                               sizeof(char));

    strcat(full_prompt, first_part);
    strcat(full_prompt, prompt);
    strcat(full_prompt, second_part);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, full_prompt);
}
