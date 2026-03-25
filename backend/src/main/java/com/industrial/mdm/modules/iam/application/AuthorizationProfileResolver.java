package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.common.security.AuthenticatedUser;

@FunctionalInterface
public interface AuthorizationProfileResolver {

    AuthorizationProfile resolve(AuthenticatedUser currentUser);
}
