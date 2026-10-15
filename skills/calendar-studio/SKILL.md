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

## Not done yet / ideas
- Not compiled in the authoring environment: build through the GitHub workflow and test on a device.
- Day and week time-grid views, ICS import/export, per-occurrence edits of repeating events, in-app updater like the other Semo apps.
