package net.intelie.lognit.cli.model;

import com.google.inject.Inject;
import net.intelie.lognit.cli.http.RestClient;
import net.intelie.lognit.cli.http.RestListener;
import net.intelie.lognit.cli.http.RestListenerHandle;
import net.intelie.lognit.cli.model.MessageBag;
import net.intelie.lognit.cli.model.SearchChannel;
import net.intelie.lognit.cli.model.Welcome;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Lognit {
    public static final String URL_WELCOME = "/rest/users/welcome";
    public static final String URL_SEARCH = "/rest/search?expression=%s&windowLength=%d";
    public static final String URL_TERMS = "/rest/terms?field=%s&term=%s";
    private final RestClient client;

    @Inject
    public Lognit(RestClient client) {
        this.client = client;
    }

    public void setServer(String server) {
        client.setServer(server);
    }
    
    public String getServer() {
        return client.getServer();
    }

    public void authenticate(String username, String password) {
        client.authenticate(username, password);
    }

    public RestListenerHandle search(String query, int windowLength, RestListener<MessageBag> listener) throws IOException {
        SearchChannel channel = client.request(make(URL_SEARCH, encode(query), windowLength), SearchChannel.class);
        return client.listen(channel.getChannel(), MessageBag.class, listener);
    }

    public Terms terms(String field, String term) throws IOException {
        return client.request(make(URL_TERMS, encode(field), encode(term)), Terms.class);
    }
    
    public Welcome welcome() throws IOException {
        return client.request(URL_WELCOME, Welcome.class);
    }

    private String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }

    private String make(String url, Object... args) {
        return String.format(url, args);
    }
}