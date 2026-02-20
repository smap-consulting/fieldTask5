package org.odk.collect.openrosa.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.Test;

// smap - tests for token auth equality behaviour in HttpCredentials
public class HttpCredentialsTest {

    @Test
    public void sameUsernameAndPassword_areEqual() {
        HttpCredentials a = new HttpCredentials("user", "pass");
        HttpCredentials b = new HttpCredentials("user", "pass");
        assertThat(a, equalTo(b));
        assertThat(a.hashCode(), equalTo(b.hashCode()));
    }

    @Test
    public void differentUsername_areNotEqual() {
        HttpCredentials a = new HttpCredentials("user1", "pass");
        HttpCredentials b = new HttpCredentials("user2", "pass");
        assertThat(a, not(equalTo(b)));
    }

    @Test
    public void tokenCredentials_areNotEqualToPasswordCredentials() {
        // Regression: cached non-token client was returned for token auth requests
        // because equals() previously ignored useToken/authToken fields.
        HttpCredentials password = new HttpCredentials("user", "pass", false, null);
        HttpCredentials token = new HttpCredentials("user", "pass", true, "mytoken");
        assertThat(password, not(equalTo(token)));
        assertThat(password.hashCode(), not(equalTo(token.hashCode())));
    }

    @Test
    public void sameTokenCredentials_areEqual() {
        HttpCredentials a = new HttpCredentials("user", "pass", true, "mytoken");
        HttpCredentials b = new HttpCredentials("user", "pass", true, "mytoken");
        assertThat(a, equalTo(b));
        assertThat(a.hashCode(), equalTo(b.hashCode()));
    }

    @Test
    public void differentTokenValues_areNotEqual() {
        // Regression: rotating tokens must produce different cache keys so the
        // new token client is created rather than reusing one with the old token.
        HttpCredentials oldToken = new HttpCredentials("user", "pass", true, "oldtoken");
        HttpCredentials newToken = new HttpCredentials("user", "pass", true, "newtoken");
        assertThat(oldToken, not(equalTo(newToken)));
        assertThat(oldToken.hashCode(), not(equalTo(newToken.hashCode())));
    }
}
