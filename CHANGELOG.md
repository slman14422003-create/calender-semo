# Changelog

## 3.2
- Fix: the selected Day / Week / Month / Year segment showed a plain grey box and hid its label (the thumb's shadow made it draw above the text).
- New Updates screen (Settings > About): checks this project's GitHub releases, downloads the APK, verifies SHA-256 and installs it from inside the app. Calendar only. Optional daily automatic check.
- New Privacy Policy screen (Settings > About), English and Arabic.
- Calendar <-> Clock link: today at a glance shows the Clock's next alarm and next prayer; an event in the next 24 hours has "Set alarm in Clock"; the Clock can send a task to "Add to Calendar"; the Clock shows the next calendar event. Only times and counts are shared, never titles or notes.
- New permissions: INTERNET (updates only) and REQUEST_INSTALL_PACKAGES (installing the update you choose).

## 3.1
- iOS motion everywhere: one motion vocabulary (Motion), one touch-feedback system (PressFx) and one dialog base (SheetDialog).
- Buttons shrink, dim and spring back; text buttons dim like iOS bar buttons; list rows turn grey after a short delay and fade out after release.
- Sheets rise with the iOS curve, follow the finger when dragged down by the top edge, and fly out on a quick flick; alerts pop in and fade out; the dim fades with them.
- Editor and Calendars open as modal sheets while the screen below scales back.
- Tab bar capsule and the Day / Week / Month / Year thumb slide with a spring; icons bounce on selection; views zoom between Month and Year.
- Month grid: selection circle springs in, months slide in the swipe direction, the day list swaps softly.
- Rows in the editor and sheets open and close smoothly (inline pickers, alerts, repeat end); switches fade their track; task circles pop when ticked.
- Animated splash: the calendar page pops in, the binder rings drop in, the day cells fade in and today's tile pops; on exit the icon grows away and the app settles in.

## 3.0
- Smart add (one sentence becomes an event or task), automatic emoji, overlap warning, free-time suggestions, "today at a glance"; all on the device.
- Design audit: right-to-left padding fixes, iOS weekday letters in Arabic, mirrored list icon, fitted "Today" label.

## 2.1
- Redrawn tab icons (filled when selected), iOS grey press on every cell, separators in the segmented control.

## 2.0
- Year view, jump to date, Calendars (visibility, colours, Holidays), Tasks tab, richer event editor (repeat end, two alerts, travel time, availability, URL, emoji), event details sheet with Duplicate / Share / Maps, snooze, week numbers, Hijri calendar, ICS carries the new fields.

## 1.4
- Collapsing iOS large titles, grouped event cards, swipe to delete, icon tiles in Settings, real app icons for Semo apps.

## 1.3
- New launcher icon (bold calendar page).
- Follows the device language: English, Arabic, French, Spanish, German, Turkish, with a language picker in Settings.
- Today / Tomorrow / Yesterday labels, dimmed past events, keyboard opens on new events.

## 1.2
- iOS precision pass: navigation bar with Today / segmented control / "+", iOS alerts and action sheets, iOS switch, inline wheel date and time pickers, iOS search bar, grouped cell metrics, new month-grid selection style.

## 1.1
- New calendar-page launcher icon.
- Day and Week views with an hour grid, segmented Day | Week | Month switch.
- Event details bottom sheet, agenda-style rows with start / end times.
- ICS import and export, default event length, smoother tab transitions.

## 1.0
- First version: month grid, agenda, search, settings, event editor with repeat / alert / colour, reminders, light and dark mode, Arabic and English, Semo ecosystem hooks.
