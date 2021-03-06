package net.intelie.lognit.cli;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import jline.ConsoleReader;
import net.intelie.lognit.cli.http.Jsonizer;
import net.intelie.lognit.cli.http.RestClient;
import net.intelie.lognit.cli.http.RestClientImpl;
import net.intelie.lognit.cli.input.EntryPoint;
import net.intelie.lognit.cli.input.UserConsole;
import net.intelie.lognit.cli.state.RestStateStorage;

import java.io.*;

public class Main extends AbstractModule {
    @Override
    protected void configure() {
        bind(RestClient.class).to(RestClientImpl.class).in(Singleton.class);
    }

    @Provides
    private ConsoleReader console() throws IOException {
        return new ConsoleReader(new FileInputStream(FileDescriptor.in),
                new PrintWriter(
                        new OutputStreamWriter(System.err,
                                System.getProperty("jline.WindowsTerminal.output.encoding", System.getProperty("file.encoding")))));
    }

    @Provides
    private UserConsole userConsole(ConsoleReader console) {
        return new UserConsole(console, new PrintWriter(System.out));
    }
    
    @Provides
    private RestStateStorage storage(Jsonizer jsonizer) {
        return new RestStateStorage(new File(new File(System.getProperty("user.home"), ".lognit"), "state"), jsonizer);
    }

    public static void main(String... args) {
        Guice.createInjector(new Main())
                .getInstance(EntryPoint.class)
                .run(args);
    }
}
