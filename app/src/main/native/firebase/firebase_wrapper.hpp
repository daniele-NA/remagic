#pragma once


#include "listener/firebase_listener.hpp"

#ifdef __cplusplus
extern "C" {
#endif


using namespace firebase;
using namespace auth;

// SEMI-SINGLETON,IT NEEDS A REFRESHED JNIEnv pointer //
class firebase_wrapper {
public:

    static firebase_wrapper &instance(JNIEnv *env, jobject activity, std::function<void(bool)> callback) {
        static firebase_wrapper inst(env, activity, callback); // Always the same instance
        return inst;
    }
    Auth *auth() { return auth_; }

    App *app() { return app_; }

    void sign_up(char *email, char *pwd);

    void sign_in(char *email, char *pwd);


private:

    firebase_wrapper(JNIEnv *env, jobject activity, std::function<void(bool)> callback)
            : listener(callback) {
        app_ = App::Create(AppOptions(), env, activity);
        auth_ = Auth::GetAuth(app_);
        auth_->AddAuthStateListener(&listener);
    }

    // == WITH GETTER METHODS == //
    App *app_;
    Auth *auth_;
    c_firebase_listener listener;
};


#ifdef __cplusplus
}
#endif


