package org.odk.collect.openrosa.http;

public class HttpCredentials implements HttpCredentialsInterface {

    private final String username;
    private final String password;
    private final boolean useToken;  // smap
    private final String authToken;  // Smap

    public HttpCredentials(String username, String password) {
        this(username, password, false, null);
    }

    public HttpCredentials(String username, String password, boolean useToken, String authToken) {
        this.username = (username == null) ? "" : username;
        this.password = (password == null) ? "" : password;
        this.useToken = useToken;
        this.authToken = authToken;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean getUseToken() {
        return useToken;
    }

    @Override
    public String getAuthToken() {
        return authToken;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (super.equals(obj)) {
            return true;
        }

        if (!(obj instanceof HttpCredentials)) {
            return false;
        }

        HttpCredentials other = (HttpCredentials) obj;
        // smap - include token fields so cached clients are not reused across auth modes
        return other.getUsername().equals(getUsername()) &&
                other.getPassword().equals(getPassword()) &&
                other.getUseToken() == getUseToken() &&
                (authToken == null ? other.getAuthToken() == null : authToken.equals(other.getAuthToken()));
    }

    @Override
    public int hashCode() {
        // smap - include token fields to distinguish token vs password auth cache entries
        return (getUsername() + getPassword() + useToken + (authToken != null ? authToken : "")).hashCode();
    }
}
