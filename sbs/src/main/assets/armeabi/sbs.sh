#!/system/bin/sh
set -x
set -e

log () {
    /system/bin/log -t SBS $*
    echo $*
}


BINDIR=${0%sbs.sh}
exec 1>> $BINDIR/sbs-log.txt 2>&1

log "============="
date
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
        mkdir -p /data/system/sbs
        chown system.system /data/system/sbs
        ln -s /data/data/com.frma.sbs/lib/$LIBSF /data/system/sbs/libsurfaceflinger.so

        log "Modifying /system"
        if [ -e /system/bin/surfaceflinger.real ] ; then
            log "/system/bin/surfaceflinger.real already exists, please uninstall first"
            exit 1
        fi
        mount -o remount,rw /system
        cp /system/bin/surfaceflinger /data/data/com.frma.sbs/files/surfaceflinger.org
        mv /system/bin/surfaceflinger /system/bin/surfaceflinger.real
        cp /data/data/com.frma.sbs/files/surfaceflinger /system/bin/surfaceflinger
        chmod 755 /system/bin/surfaceflinger
        mount -o remount,ro /system
        log "Done modifying system"
        ;;
    uninstall)
        rm -rf /data/system/sbs
        if [ ! -e /system/bin/surfaceflinger.real ] ; then
            log "/system/bin/surfaceflinger.real not found, can't uninstall"
        else
            mount -o remount,rw /system
            mv /system/bin/surfaceflinger.real /system/bin/surfaceflinger
            mount -o remount,ro /system
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
        mkdir -p /data/system/sbs
        chown system.system /data/system/sbs
        su -c "touch /data/system/sbs/enabled" system
        ;;
    enablepermanent)
        mkdir -p /data/system/sbs
        chown system.system /data/system/sbs
        su -c "touch /data/system/sbs/enabled" system
        su -c "touch /data/system/sbs/permanent" system
        ;;
    disable)
        su -c "rm /data/system/sbs/enabled" system
        su -c "rm -f /data/system/sbs/permanent" system
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
        if grep com.frma.sbs /proc/$(pidof surfaceflinger.real)/maps > /dev/null 2>&1 ; then
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


