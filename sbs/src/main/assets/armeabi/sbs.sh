#!/system/bin/sh
set -x
set -e

BINDIR=${0%sbs.sh}
LOG=$BINDIR/sbs-log.txt

log () {
    /system/bin/log -t SBS $*
    echo $*
}

# Busybox comands
bbcp ()     { $BINDIR/busybox cp $* ; }
bbdate ()   { $BINDIR/busybox date $* ; }
bbln ()     { $BINDIR/busybox ln $* ; }
bbmd5sum () { $BINDIR/busybox md5sum $1 ; }
bbmv ()     { $BINDIR/busybox mv $* ; }
bbpidof ()  { $BINDIR/busybox pidof $* ; }
bbrm ()     { $BINDIR/busybox rm $* ; }
bbchmod()   { $BINDIR/busybox chmod $* ; }

# Built in commands
bimount ()  { mount $* ; }
bichown ()  { chown $* ; }


# Wrappers for busybox
bbtouch () {
    $BINDIR/busybox touch $1
    bichown system.system $1
}
bbmkdir () {
    $BINDIR/busybox mkdir -p $1
    bichown system.system $1
}

# Add properties and md5sum of libsurfaceflinger if we haven't done that before
if [ ! -e /data/system/sbs/log-not-empty ] ; then
    bbmkdir /data/system/sbs
    bbtouch /data/system/sbs/log-not-empty
    getprop  > $LOG  || true
    bbmd5sum /system/lib/libsurfaceflinger.so >> $LOG
fi
# Make sure the log is readable by the email client (and everyone else)
test -e $LOG && bbchmod 0666 $LOG
# Redirect all output to the log file
exec 1>> $LOG 2>&1

log "============="
bbdate
log "ARGS: $*"

BB=$BINDIR/busybox

VER=$(getprop ro.build.version.release)
CMVER=$(getprop ro.cm.version)

log "VER:$VER CMVER:$CMVER"
echo "$VER"   | grep "^4.4" && IS44=1 > /dev/null
echo "$CMVER" | grep "^11" && ISCM11=1 > /dev/null

log "Got IS44=$IS44 ISCM11=$ISCM11"
if [ -z "$LIBSF" ] ; then
    if [ "$ISCM11" = "1" ] ; then
	LIBSF=libsurfaceflinger-cm11.so
    elif [ "$IS44" = "1" ] ; then
	LIBSF=libsurfaceflinger-aosp-4.4.so
else
	log "No matching shared library" ; exit 108
    fi
fi

log "Selected library: $LIBSF"

test -e $BB || ( log "Failed to copy busybox to $BB" 2>&1 ; exit 104 )

case $1 in
    install)
        log "Installing"
        bbmkdir /data/system/sbs
        bbrm -f /data/system/sbs/libsurfaceflinger.so
        bbln -s /data/data/com.frma.sbs/lib/$LIBSF /data/system/sbs/libsurfaceflinger.so

        log "Modifying /system"
        if [ -e /system/bin/surfaceflinger.real ] ; then
            log "/system/bin/surfaceflinger.real already exists, please uninstall first"
            exit 1
        fi
        bimount -o remount,rw /system
        bbcp /system/bin/surfaceflinger /data/data/com.frma.sbs/files/surfaceflinger.org
        bbmv /system/bin/surfaceflinger /system/bin/surfaceflinger.real
        bbcp /data/data/com.frma.sbs/files/surfaceflinger /system/bin/surfaceflinger
        bbchmod 755 /system/bin/surfaceflinger
        bimount -o remount,ro /system
        log "Done modifying system"
         ;;
    uninstall)
        rm -rf /data/system/sbs
        if [ ! -e /system/bin/surfaceflinger.real ] ; then
            log "/system/bin/surfaceflinger.real not found, can't uninstall"
        else
            bimount -o remount,rw /system
            mv /system/bin/surfaceflinger.real /system/bin/surfaceflinger
            bimount -o remount,ro /system
        fi
        ;;
    isinstalled)
        if [ -e /system/bin/surfaceflinger.real ] ; then
            log "Result: Yes"
            exit 0
        else
            log "Result: No"
            exit 1
        fi
	    ;;
    enable)
        bbmkdir /data/system/sbs
        bbtouch /data/system/sbs/enabled
        ;;
    enablepermanent)
        bbmkdir /data/system/sbs
        bbtouch /data/system/sbs/enabled
        bbtouch /data/system/sbs/permanent
        ;;
    disable)
        bbrm /data/system/sbs/enabled
        bbrm -f /data/system/sbs/permanent
        ;;
    ispermanent)
        if [ -e /data/system/sbs/enabled -a -e /data/system/sbs/permanent ] ; then
            log "Result: Yes"
            exit 0
        else
            log "Result: No"
            exit 1
        fi
        ;;
    isenabled)
        if [ -e /data/system/sbs/enabled ] ; then
            log "Result: Yes"
            exit 0
        else
            log "Result: No"
            exit 1
        fi
        ;;
    isloaded)
        if grep com.frma.sbs /proc/$(bbpidof surfaceflinger.real)/maps > /dev/null 2>&1 ; then
            log "Result: Yes"
            exit 0
        else
            log "Result: No"
            exit 1
        fi
	    ;;
	reboot)
	    sync
	    ( stop ; reboot ) &
	    ;;
    set)
        FLAGS=$2
        ZOOM=$3
        IMGDIST=$4
        R=$(service call SurfaceFlinger 4711 i32 $FLAGS i32 $ZOOM i32 $IMGDIST)
	    log "Result: $R"
	    echo "$R" | grep "^Result: Parcel(NULL)" && ( echo "Ok" ; exit 0 ) || ( log "Not installed" ; exit 106)
        ;;
    *)
	    log "SBS: Unknown subcommand: $1"
	    exit 107
	;;
esac


