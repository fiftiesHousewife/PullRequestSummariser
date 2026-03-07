package org.fifties.housewife;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;

final class ProxyAwareHttpClient {

    private ProxyAwareHttpClient() {
    }

    static HttpClient create() {
        final HttpClient.Builder builder = HttpClient.newBuilder()
                .proxy(ProxySelector.getDefault());
        final String proxyUser = System.getProperty("https.proxyUser");
        final String proxyPassword = System.getProperty("https.proxyPassword");
        if (proxyUser != null && proxyPassword != null) {
            builder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                }
            });
        }
        return builder.build();
    }
}
