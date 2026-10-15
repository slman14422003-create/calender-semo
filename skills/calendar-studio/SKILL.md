---
name: calendar-studio
description: Progress log and architecture notes for the Semo Calendar Android app (app.semo.calendar, Java/Views). Read before continuing any work on this project.
---

# Semo Calendar - progress

## Project facts
- Package app.semo.calendar, source package com.semo.calendar, Java with programmatic Views, no Compose.
- Identity copied from Gallery-semo: iOS system colours (day #007AFF on white / #F2F2F7, night #0A84FF on AMOLED black), floating capsule tab bar, grouped cards, petals launcher icon.
- Screens: MainActivity (tabs: MonthScreen, AgendaScreen, SearchScreen, SettingsScreen), EditorActivity (new / edit), ShareActivity (share text to Calendar).
- Month grid is drawn on a Canvas (MonthView); lists use RecyclerView (EventAdapter).
- Events: one JSON file (Store), repeats expanded on the fly (Recur), inexact alarms (Alarms, ReminderReceiver, BootReceiver).
- Semo ecosystem: manifest declares the app.semo.ECOSYSTEM filter and app.semo.ROLE=calendar; Semo.java finds siblings and can send an event to the Notes app.
- No network at all: CI guard fails the build if INTERNET or network classes appear, or if anything but MainActivity and ShareActivity is exported.
- Code files contain English only; strings live in res/values (ar) and res/values-en.

## Done (1.1)
- Launcher icon is now a calendar page (white sheet, blue header band, two binder rings) with the Semo petals mark in the page body; ic_launcher_foreground.xml and ic_launcher_monochrome.xml. Background stays the identity white-to-#EFF6FF gradient.
- First tab = CalendarTab: iOS segmented control Day | Week | Month over TimeScreen (1 / 7 columns, TimeGridView hour grid, all-day pills, day strip, tap empty slot to create, tap event to open, swipe to move) and MonthScreen. All three share one date (DayAware.anchor / setAnchor) and the last view is remembered (Prefs.view).
- Event rows show start / end time on the side; event details open as a bottom sheet (DetailSheet) with Edit, Send to Notes, Delete.
- Settings: first weekday, default alert, default event length, appearance, ICS export / import (Storage Access Framework, no storage permission, no FileProvider), Semo apps list.
- Ics.java: VEVENT title, place, notes, start, end, all-day, FREQ repeat and one VALARM; UID is kept as the event id so re-importing a file updates instead of duplicating.

## Not done yet / ideas
- Not compiled in the authoring environment: build through the GitHub workflow and test on a device.
- Per-occurrence edits of repeating events, multi-day all-day bars across the week grid, in-app updater like the other Semo apps (would need Net.java and the CI network guard changed on purpose).
