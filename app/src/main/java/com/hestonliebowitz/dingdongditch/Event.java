package com.hestonliebowitz.dingdongditch;

public class Event {
    private String name;
    private Float occurredAt;

    private Event() {}

    public Event(String name, Float occurredAt) {
        this.name = name;
        this.occurredAt = occurredAt;
    }

    public String getName() {
        return name;
    }

    public Float getOccurredAt() {
        return occurredAt;
    }

    public String getFormattedDate() {
        return DataService.formatTimestamp(occurredAt);
    }
}
