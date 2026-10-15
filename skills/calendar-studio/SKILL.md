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

## Done (1.2, iOS precision pass)
- Metrics follow iOS: large title 34sp bold (auto-shrinks), cells min 44dp, inset-grouped corner 10dp, section headers 13sp upper case, hairline separators 0.5dp inset 16dp, disclosure chevrons.
- Calendar tab top bar is an iOS navigation bar: Today | segmented control (32dp track, 9dp radius, thumb white / #636366) | "+". The round Android add button was removed on purpose (iOS keeps "+" in the bar).
- Month grid: single-letter weekdays, hairline under them, 20sp numbers; today = blue circle, any other selected day = black / white circle, today without selection = blue text.
- Dialogs are iOS: Sheets.alert (270dp, hairline buttons, red destructive) and Sheets.actionSheet (check on the current choice, separate Cancel card). No Material AlertDialog is left.
- Editor = iOS New Event: Cancel | title | Save, iOS green switch, date / time chips (blue when active) opening inline wheel pickers (DatePicker / TimePicker in spinner mode, style Wheel), action sheets for Repeat and Alert.
- Search field = iOS search bar (36dp, radius 10, magnifier).
- Colours added: seg_track, seg_thumb, alert_bg (day and night).

## Done (1.3)
- Launcher icon redesigned: big calendar page (white to pale-blue page, glossy blue header band, two binder rings, 3x4 day cells with today as a blue gradient tile), scaled to 65% (92% and 80% looked too big on the home screen). Same palette as the other Semo icons, no petals. Monochrome version for themed icons.
- Languages follow the device: English is the default (res/values), plus ar, fr, es, de, tr (values-ar ...). Settings > Language picks System or one language (AppCompatDelegate.setApplicationLocales, locales_config.xml for Android 13+, AppLocalesMetadataHolderService for older). Language names live in res/values/arrays.xml (lang_names) so code stays ASCII. To add a language: new values-xx/strings.xml with the same keys, add it to locales_config.xml, build.gradle localeFilters, the tags array in SettingsScreen and lang_names.
- Lists say Today / Tomorrow / Yesterday, today's header in the agenda is blue, past events are dimmed, a new event opens with the keyboard on the title.
- Code files are pure ASCII (non-ASCII punctuation is written as unicode escapes).

## Not done yet / ideas
- Not compiled in the authoring environment: build through the GitHub workflow and test on a device.
- Per-occurrence edits of repeating events, multi-day all-day bars across the week grid, in-app updater (needs Net.java and the CI network guard changed on purpose).
