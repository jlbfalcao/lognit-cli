package net.intelie.lognit.cli.commands;

import net.intelie.lognit.cli.http.CookieStorage;
import org.apache.commons.httpclient.HttpClient;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class StateKeeperTest {

    private CookieStorage storage;
    private HttpClient client;
    private StateKeeper keeper;

    @Before
    public void setUp() throws Exception {
        storage = mock(CookieStorage.class);
        client = mock(HttpClient.class);
        keeper = new StateKeeper(client, storage);
        verifyZeroInteractions(storage,  client);
    }

    @Test
    public void testBegin() throws Exception {
        keeper.begin();
        verify(storage).recoverTo(client);
    }

    @Test
    public void testEnd() throws Exception {
        keeper.end();
        verify(storage).storeFrom(client);
    }
}