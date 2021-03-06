package net.intelie.lognit.cli.input;

import net.intelie.lognit.cli.http.UnauthorizedException;
import net.intelie.lognit.cli.model.Lognit;
import net.intelie.lognit.cli.model.Welcome;
import org.apache.commons.httpclient.StatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

public class RequestRunnerTest {
    private UserConsole console;
    private Lognit lognit;
    private RequestRunner runner;
    private BufferListenerFactory factory;

    @Before
    public void setUp() throws Exception {
        console = mock(UserConsole.class);
        lognit = mock(Lognit.class, RETURNS_DEEP_STUBS);
        factory = mock(BufferListenerFactory.class, RETURNS_DEEP_STUBS);
        runner = new RequestRunner(console, lognit, factory);
    }

    @Test
    public void whenHasNoQueryDoesNothing() throws Exception {
        runner.run(new UserOptions());
        verifyNoMoreInteractions(console, lognit);
    }

    @Test
    public void whenHasInfoPrintsWelcome() throws Exception {
        when(lognit.welcome()).thenReturn(new Welcome("blablabla"));
        runner.run(new UserOptions("-i"));
        verify(lognit).getServer();
        verify(console).println("server: %s", null);
        verify(lognit).welcome();
        verify(console).println("blablabla");
        verifyNoMoreInteractions(console, lognit);
    }


    @Test
    public void whenHasQueryExecutesSearchAndClose() throws Exception {
        runner.run(new UserOptions("blablabla", "-n", "42"));
        verify(lognit).search("blablabla", 42, factory.create(false));
        verify(lognit.search("blablabla", 42, factory.create(false))).close();
    }

    @Test
    public void whenHasQueryToFollowExecutesReleasesAndWait() throws Exception {
        runner.run(new UserOptions("blablabla", "-n", "42", "-f"));
        BufferListener listener = factory.create(false);
        verify(lognit).search("blablabla", 42, listener);
        verify(listener).releaseAll();
        verify(console).waitChar('q');
        verify(lognit.search("blablabla", 42, listener)).close();
    }

    @Test
    public void whenHasServerAlsoSetsTheServer() throws Exception {
        when(lognit.welcome()).thenReturn(new Welcome("blablabla"));
        when(lognit.getServer()).thenReturn("someserver");
        runner.run(new UserOptions("-s", "someserver"));

        verify(lognit).setServer("someserver");
        verify(lognit).getServer();
        verify(console).println("server: %s", "someserver");
        verify(lognit).welcome();
        verify(console).println("blablabla");
        verifyNoMoreInteractions(console, lognit);
    }

    @Test
    public void whenHasUserOnlyDoesNothingYet() throws Exception {
        when(lognit.welcome()).thenReturn(new Welcome("blablabla"));
        runner.run(new UserOptions("-u", "user", "-i"));

        verify(lognit).getServer();
        verify(console).println("server: %s", null);
        verify(lognit).welcome();
        verify(console).println("blablabla");
        verifyNoMoreInteractions(console, lognit);
    }


    @Test
    public void whenHasUserAndPasswordAuthenticates() throws Exception {
        when(lognit.welcome()).thenReturn(new Welcome("blablabla"));
        when(lognit.getServer()).thenReturn("someserver");
        runner.run(new UserOptions("-u", "user", "-p", "pass", "-i"));

        verify(lognit).authenticate("user", "pass");
        verify(lognit).getServer();
        verify(console).println("server: %s", "someserver");
        verify(lognit).welcome();
        verify(console).println("blablabla");
        verifyNoMoreInteractions(console, lognit);
    }

    @Test
    public void whenReceivesUnauthorizedTryAskUser() throws Exception {
        Welcome welcome = new Welcome("blablabla");
        when(lognit.getServer()).thenReturn("someserver");
        when(lognit.welcome())
                .thenThrow(new UnauthorizedException(new StatusLine("HTTP/1.0 401 OK")))
                .thenReturn(welcome);
        when(console.readLine("login: ")).thenReturn("somelogin");
        when(console.readPassword("%s's password: ", "somelogin")).thenReturn("somepass");

        runner.run(new UserOptions("-i"));

        InOrder orderly = inOrder(lognit, console);
        orderly.verify(lognit).getServer();
        orderly.verify(console).println("server: %s", "someserver");
        orderly.verify(lognit).welcome();
        orderly.verify(console).println("HTTP/1.0 401 OK");
        orderly.verify(console).readLine(anyString());
        orderly.verify(console).readPassword(anyString(), anyString());
        orderly.verify(lognit).authenticate("somelogin", "somepass");
        orderly.verify(lognit).welcome();
        orderly.verify(console).println("blablabla");
        orderly.verifyNoMoreInteractions();
    }

    @Test
    public void whenReceivesUnauthorizedTryAskPasswordOnlyIfUserIsPassed() throws Exception {
        Welcome welcome = new Welcome("blablabla");
        when(lognit.getServer()).thenReturn("someserver");
        when(lognit.welcome())
                .thenThrow(new UnauthorizedException(new StatusLine("HTTP/1.0 401 OK")))
                .thenReturn(welcome);
        when(console.readPassword("%s's password: ", "somelogin")).thenReturn("somepass");

        runner.run(new UserOptions("-u", "somelogin", "-i"));

        InOrder orderly = inOrder(lognit, console);
        orderly.verify(lognit).getServer();
        orderly.verify(console).println("server: %s", "someserver");
        orderly.verify(lognit).welcome();
        orderly.verify(console).println("HTTP/1.0 401 OK");
        orderly.verify(console).readPassword(anyString(), anyString());
        orderly.verify(lognit).authenticate("somelogin", "somepass");
        orderly.verify(lognit).welcome();
        orderly.verify(console).println("blablabla");
        orderly.verifyNoMoreInteractions();
    }

    @Test
    public void whenReceivesUnauthorizedAsksAtMostThreeTimes() throws Exception {
        when(lognit.welcome())
                .thenThrow(new UnauthorizedException(new StatusLine("HTTP/1.0 401 OK")));
        when(console.readLine("login: ")).thenReturn("somelogin");
        when(console.readPassword("%s's password: ", "somelogin")).thenReturn("somepass");

        runner.run(new UserOptions("-i"));

        verify(lognit, times(4)).welcome();
        verify(console, times(3)) .readLine(anyString());
    }

    @Test
    public void whenReceivesUnauthorizedAndHasUserAndPasswordTriesOnlyOnce() throws Exception {
        when(lognit.welcome())
                .thenThrow(new UnauthorizedException(new StatusLine("HTTP/1.0 401 OK")));

        runner.run(new UserOptions("-u", "someuser", "-p", "somepass", "-i"));

        verify(lognit).getServer();
        verify(console).println("server: %s", null);
        verify(lognit).authenticate("someuser", "somepass");
        verify(lognit, times(1)).welcome();
        verify(console).println("HTTP/1.0 401 OK");
        verifyNoMoreInteractions(lognit, console);
    }

}
