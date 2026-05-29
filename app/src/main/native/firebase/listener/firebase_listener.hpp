
#pragma once

#include <firebase/app.h>
#include <firebase/auth.h>
#include "../../core/debug.h"

using namespace firebase;
using namespace auth;
using namespace std;

// CALLED BY WRAPPER //
class c_firebase_listener : public AuthStateListener {
public:
    std::function<void(bool)> onResultForKotlinSide;

    c_firebase_listener(std::function<void(bool)> callback)
            : onResultForKotlinSide(callback) {}

    void OnAuthStateChanged(Auth* auth) override {
        User user = auth->current_user();
        bool result = false; // FALSE
        if (user.is_valid()) {
            LOG_E(
                    "LOGGED IN (Listener) | id: %s | name: %s | email: %s | photo: %s",
                    user.uid().c_str(),
                    user.display_name().c_str(),
                    user.email().c_str(),
                    user.photo_url().c_str()
            );
            result = true;
        } else {
           LOG_E("LOGGED OUT (Listener)");
        }

        onResultForKotlinSide(result);

    }
};