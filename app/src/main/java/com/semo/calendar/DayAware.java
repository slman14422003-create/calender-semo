package com.semo.calendar;

/** A calendar view that shows one point in time and can be moved to another (shared between Day, Week, Month). */
interface DayAware extends Screen {
    long anchor();

    void setAnchor(long millis);
}
