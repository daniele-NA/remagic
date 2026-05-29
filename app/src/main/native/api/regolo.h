#pragma once

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <curl/curl.h>

#include <jni.h>
#include <string.h>


char *llm(char *prompt,char * regolo_key) ;

size_t write_callback(void *contents, size_t size, size_t nmemb, void *user_pt) ;

void set_headers(CURL *curl, const char *api_key);

void set_data(CURL *curl, char *prompt) ;
