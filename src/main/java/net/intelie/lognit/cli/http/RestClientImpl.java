package net.intelie.lognit.cli.http;

import com.google.inject.Inject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;

public class RestClientImpl implements RestClient {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HttpClient client;
    private final MethodFactory methods;
    private final Jsonizer jsonizer;

    private String server;
    private boolean authenticated;

    @Inject
    public RestClientImpl(HttpClient client, MethodFactory methods, Jsonizer jsonizer) {
        this.client = client;
        this.methods = methods;
        this.jsonizer = jsonizer;
        this.server = "localhost";
        this.authenticated = false;
    }

    @Override
    public RestState getState() {
        return new RestState(client.getState().getCookies(), server);
    }

    @Override
    public void setState(RestState state) {
        client.getState().addCookies(state.getCookies());
        server = state.getServer();
    }

    @Override
    public void authenticate(String server, String username, String password) throws MalformedURLException {
        client.getState().clearCookies();
        client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        client.getParams().setAuthenticationPreemptive(true);

        this.server = server;
        this.authenticated = true;
    }

    @Override
    public <T> T request(String uri, Class<T> responseClass) throws IOException {
        uri = prependServerIfApplicable(uri);

        HttpMethod method = execute(uri);

        String body = IOUtils.toString(method.getResponseBodyAsStream());
        return jsonizer.from(body, responseClass);
    }

    private String prependServerIfApplicable(String uri) throws MalformedURLException {
        if (server != null) {
            String safeUri = uri.startsWith("/") ? uri : "/" + uri;
            uri = String.format("http://%s%s", server, safeUri);
        }
        return uri;
    }

    private HttpMethod execute(String uri) throws IOException {
        HttpMethod method = methods.get(uri);

        method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

        method.setDoAuthentication(authenticated);
        if (client.executeMethod(method) != 200)
            throw new RequestFailedException(method.getStatusLine());
        return method;
    }

}