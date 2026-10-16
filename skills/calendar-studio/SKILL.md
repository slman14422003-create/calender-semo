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

## Done (1.4, iOS feel)
- Collapsing large titles (LargeNav + HeaderAdapter): Agenda, Search and Settings have a 44dp bar whose small centred title and hairline fade in when the 34sp title scrolls away.
- Event lists are inset-grouped: consecutive events share one rounded card (corners decided per row in EventAdapter), hairline between rows, highlight while pressed. Swipe a row sideways for the red iOS "Delete" strip (SwipeDelete), release asks for confirmation.
- Settings rows carry coloured icon tiles (language blue, first day red, alert orange, length green, appearance violet, export / import); Semo app rows show the real launcher icon of the sibling app.

## Done (2.0, iPhone + Samsung features)
- Views: Day, Week, Month, Year (CalendarTab, 4 segments). Tap the title to jump to a date. Tab icon shows today's date (DateIconDrawable).
- Calendars (iOS): Personal, Work, own calendars, built-in Holidays (off by default; fixed days + Islamic days from Umm al-Qura dates). CalendarsActivity toggles visibility, CalendarSheet edits name / colour. Event colour is its own or its calendar's (color -1). Store.between() applies visibility everywhere.
- Event editor: calendar, repeat (daily, weekly, every 2 weeks, weekdays, monthly, yearly) with end (never / date / count), two alerts, travel time (alert moves earlier), availability, URL, emoji (Samsung stickers), colour, notes. All-day alerts fire at 9:00.
- Detail sheet: info card + actions (Edit, Duplicate, Share, Open in Maps, Open link, Send to Notes, Delete). Holidays are read-only.
- Tasks tab (Samsung tasks / iOS Reminders): Overdue, Today, Upcoming, No date, Completed; due date and time, alert with "Mark as done" button; tasks also appear in the month day list and the agenda.
- Notifications have Snooze (10 min). Week numbers and Hijri day numbers (alternate calendar) in the month grid; Hijri month in the subtitle.
- ICS import / export carries repeat interval, weekdays, until, count, URL, free / busy and two alerts.
- Strings: 6 languages, all keys present everywhere (checked by script). Emoji list lives in res/values/arrays.xml.

## Done (2.1, iOS polish)
- Tab bar icons are drawn in code (TabIconDrawable) on a 24-unit grid like SF Symbols: outlined when idle, filled when selected (calendar with today's number cut out, bulleted list, check circle, magnifier, 8-tooth gear).
- iOS cell press: rows turn grey (Ui.highlight, colour cell_highlight) instead of shrinking; group cards clip their children to the rounded outline; event rows keep their own corners while pressed.
- Segmented control has hairline separators between idle segments (hidden next to the selected one).

## Build fixes
- Hijri.cal() uses Calendar.getInstance(new ULocale("en@calendar=islamic-umalqura")): IslamicCalendar.CalendarType is not in the SDK stubs (first CI build failed on it).

- CI signing step: a KEYSTORE_BASE64 that is not valid base64 (or a wrong password / alias) no longer fails the build; the secret is cleaned (quotes, spaces, "KEYSTORE_BASE64=" prefix) and, if it still does not open, the build falls back to the debug key with a warning naming the problem.

## Not done yet / ideas
- Not compiled in the authoring environment: build through the GitHub workflow and test on a device.
- Not possible offline or without extra permissions: invitees, subscribed (URL) calendars, birthdays from contacts, home-screen widget (needs an exported receiver, the CI guard allows only MainActivity and ShareActivity), per-occurrence edits of a repeating event, drag to move events in Day / Week.
