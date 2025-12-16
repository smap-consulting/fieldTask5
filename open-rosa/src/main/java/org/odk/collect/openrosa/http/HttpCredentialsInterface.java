package org.odk.collect.openrosa.http;

public interface HttpCredentialsInterface {
    String getUsername();

    String getPassword();

    boolean getUseToken();

    String getAuthToken();
}
