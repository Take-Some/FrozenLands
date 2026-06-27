package org.takesome.frozenlands.engine.events;

import java.util.ArrayList;
import java.util.List;

public final class EventSubscriptionBag implements AutoCloseable {
    private final List<AutoCloseable> subscriptions = new ArrayList<>();

    public boolean isEmpty() {
        return subscriptions.isEmpty();
    }

    public int size() {
        return subscriptions.size();
    }

    public void add(AutoCloseable subscription) {
        if (subscription != null) {
            subscriptions.add(subscription);
        }
    }

    @Override
    public void close() {
        for (AutoCloseable subscription : subscriptions) {
            try {
                subscription.close();
            } catch (Exception ignored) {
            }
        }
        subscriptions.clear();
    }
}
