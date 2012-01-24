package net.intelie.lognit.cli.model;

import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.ReverseComparator;

import java.util.Comparator;

public class Message implements Comparable<Message> {
    private static final Comparator COMPARATOR = new NullComparator(
            new ReverseComparator(String.CASE_INSENSITIVE_ORDER), true);
    private final String id;
    private final String severity;

    private final String message;

    public Message(String id, String severity, String message) {
        this.id = id;
        this.severity = severity;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getId() {
        return id;
    }

    @Override
    public int compareTo(Message that) {
        return COMPARATOR.compare(this.id, that.id);
    }

    public String getSeverity() {
        return severity;
    }
}
