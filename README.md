sbs
===

This is the source code for the installation and control app for the first version of Android Native SBS support. Native SBS makes 
it possible to use all apps (like Plex, Play Movies, Netflix, etc.) with opendive, google cardboard or similar for viewing on a large virtual screen.

- Installation might cause your phone to crash or freeze, but it doesn't do ANY permanent changes so a reboot should
  always help.
- This has only been tested on AOKP_jflte_kitkat_nightly_2014-05-24 (AOKP based on 4.4.2 on Samsung S4 - I9505), but probably works with
  all CM11 based KitKat roms. (And possibly some KitKat stock ROMs)
- You need a rooted ROM.
- Installation is not stable due to the fact that some drivers doesn't sometime crashes when we restart the framework, I've attempted
  to work around this by triggering the installation with the power button, which also deactivates the graphics.
- If installation fails, try again.
- If the phone hangs, reboot it.
- I use it with a bluetooth mouse and have watched hours and hours of movies with it (mostly when flying).
- The "fill" functionality is not implemented.
- Uninstall does not work, reboot your phone instead.

You can see it in action here: https://www.youtube.com/watch?v=74F7AbyVFl0

I have a lot of projects, a day-job and not much time for this project, in addition estetics and user interfaces aren't my 
strong side. So if someone wants to help rewrite this into something neater I'd appreciate the help.

Some ideas:

I1) Enable/disable in a way that works in both SBS and non-sbs mode. Since using the touch-screen is hard in SBS mode.
I2) Lens distortion correction (any opengl-wizards out there that would like to help ?)
I3) Incorporate the change in some open source ROMS (the patch is quite small and isolated but does need some work)
I4) Add permanent installation option.
I5) Make individual zoom settings for landscape and portrait mode.
I6) Make zoom settings persistent

