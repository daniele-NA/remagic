
#include "firebase_wrapper.hpp"

#include <firebase/app.h>
#include <firebase/auth.h>
#include "firebase/ump.h"
#include "../core/debug.h"
#include <jni.h>

using namespace firebase;
using namespace auth;

void firebase_wrapper::sign_up(char *email,char *pwd) {
    auto result = auth_->CreateUserWithEmailAndPassword(email,pwd);
    // == ASYNC TASK == //
    result.OnCompletion([](const Future<AuthResult>& f) {
        if (f.error() == false) LOG_E("REGISTERED");
        else LOG_E("sign_up error %s",f.error_message());
    });
}

void firebase_wrapper::sign_in(char *email,char *pwd) {
    auto result = auth_->SignInWithEmailAndPassword(email,pwd);
    // == ASYNC TASK == //
    result.OnCompletion([](const Future<AuthResult>& f) {
        if (f.error() == false) LOG_E("LOGGED");
        else LOG_E("sign_in error %s",f.error_message());
    });
}