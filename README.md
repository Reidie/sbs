sbs
===

This is the source code for the installation and control app for the first version of
Native SBS support for Android. Native SBS makes it possible to use all apps (like Plex,
Play Movies, Netflix, etc.) with opendive, google cardboard or similar for viewing on a large
virtual screen.

Before you go ahead and install this app make sure that you have root and a working recovery in
case something goes horribly wrong.

The code that is the core of this functionality is not part of any open API so the compatibility
of this app needs to be tested on a per android version and device type.

When you have installed and enabled SBS (in the app) your device might fail to boot. If you
selected the option of only enable "on next boot" you can just power off and power on the phone
again and it *should* boot fine. If you select the "Dangerous" option you might not be so lucky.

If your device fails to boot, read the "To manually undo everything SBS has done to your device"
chapter below.

- This has only been tested on AOKP_jflte_kitkat_nightly_2014-05-24 (AOKP based on 4.4.2 on Samsung
  S4 - I9505), but probably works with all CM11 based KitKat roms. (And possibly some KitKat stock
  ROMs)
- If the phone hangs, reboot it.
- I use it with a bluetooth mouse and have watched hours and hours of movies with it (mostly when flying).

You can see it in action here: https://www.youtube.com/watch?v=74F7AbyVFl0

I have a lot of projects, a day-job and not much time for this project, in addition estetics and user interfaces aren't my 
strong side. So if someone wants to help rewrite this into something neater I'd appreciate the help.

Please report success and fail with device name and os version in the community https://plus.google.com/u/0/communities/117807409036006925326?cfem=1

Status

Device    | ROM                    | Reported by       | Status                           
----------|------------------------|-------------------|-----------------------
LG G2     | optimusRX stock G3 ROM | Anton Osika       | Working           
LG G3     | Stock rom and root     | Grzegorz Mazur    | Working
Galaxy Note II | AOKP 4.4.4        | Luis Aviles       | Working
Galaxy S4 | CM11-M8                | Fredrik Markström | Working
Galaxy S4 | CM11-M9                | Fredrik Markström | Working
Nexus 5   | Stock                  | Miebi Sikoki      | Working
HTC One M8| GPE                    | Steven Weeks      | Working
Oppo Find 7 | CM11 nightlies       | Travis Brown      | Working
OnePlus One | CM11s 33r            | Владислав Тряпышко| Working
Sony D6503 Sirius | Stock 4.4.2    | Kristoffer Andersson | Not working
Xperia Z2 | Stock, rooted 4.4.2    | George Do         | Not working
Xperia Z1 | CM11                   | Ahmet Yildirim         |Working

Some ideas:

I1) Enable/disable in a way that works in both SBS and non-sbs mode. Since using the touch-screen is hard in SBS mode.
I2) Lens distortion correction (any opengl-wizards out there that would like to help ?)
I3) Incorporate the change in some open source ROMS (the patch is quite small and isolated but does need some work)
I4) Add permanent installation option.
I5) Make individual zoom settings for landscape and portrait mode.
I6) Make zoom settings persistent



To manually undo everything SBS has done to your device

1) Boot into recovery and connect to your phone with adb
2) Mount /system read/write

   mount -o rw /system

3) Restore /system binaries

   mv /system/bin/surfaceflinger.real /system/bin/surfaceflinger

* At this step your phone should boot fine, but if we really want to clean up we continue with:

4) Mount data

   mount /data

5) Remove sbs related files

   rm -rf /data/system/sbs

