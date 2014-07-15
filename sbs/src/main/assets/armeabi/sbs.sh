#!/system/bin/sh

BINDIR=${0%sbs.sh}
exec &> $BINDIR/sbs-log.txt
echo "============="
date
echo $*
test -e /system/bin/start  || ( echo "/system/bin/start doesn't exist" ; exit 101 )
test -e /system/bin/stop   || ( echo "/system/bin/stop doesn't exist" ; exit 102 )
test -e /system/bin/reboot || ( echo "/system/bin/reboot doesn't exist" ; exit 103 )

BB=$BINDIR/busybox

VER=$(getprop ro.build.version.release)
CMVER=$(getprop ro.cm.version)

echo "VER:$VER CMVER:$CMVER"
echo "$VER"   | grep "^4.4" && IS44=1
echo "$CMVER" | grep "^11" && ISCM11=1

echo "Got IS44=$IS44 ISCM11=$ISCM11"
if [ -z "$LIBSF" ] ; then
    if [ "$ISCM11" = "1" ] ; then
	LIBSF=libsurfaceflinger-cm11.so
    elif [ "$IS44" = "1" ] ; then
	LIBSF=libsurfaceflinger-aosp-4.4.so
    else
	echo "No matching shared library" ; exit 108
    fi
fi

echo "Selected library: $LIBSF"

test -e $BB || ( echo "Failed to copy busybox to $BB" 2>&1 ; exit 104 )

case $1 in
    install)
	/system/bin/stop zygote
	sleep 1
	/system/bin/stop surfaceflinger
	sleep 1
        $BB mount -o bind /data/data/com.frma.sbs/lib/$LIBSF /system/lib/libsurfaceflinger.so && ( echo "Failed to mount" ; exit 105 )
	/system/bin/start surfaceflinger
	sleep 1
	/system/bin/start zygote
	sleep 1
        ;;
    uninstall)
	/system/bin/stop zygote
	sleep 1
	/system/bin/stop surfaceflinger
	sleep 1
	umount /system/lib/libsurfaceflinger.so && ( echo "Failed to unmount" ; exit 1)
	/system/bin/start surfaceflinger
	sleep 1
	/system/bin/start zygote
	sleep 1
        ;;
    isinstalled)
	mount |grep libsurfaceflinger && ( echo "Yes" ; exit 0 ) || ( echo "No" ; exit 1 )
	;;
    restart)
	/system/bin/stop zygote
	sleep 1
	/system/bin/stop surfaceflinger
	sleep 1
	/system/bin/start surfaceflinger
	sleep 1
	/system/bin/start zygote
	sleep 1
        ;;
    set)
        ENABLE=$2
        ZOOM=$3
        ZOOM=${ZOOM:-128}
        if [ "$ENABLE" = "1" ] ; then
            R=$(service call SurfaceFlinger 4711 i32 $(( 1 + $ZOOM*16 )))
        else
            R=$(service call SurfaceFlinger 4711 i32 0)
        fi
	echo "$R"
	echo "$R" | grep "^Result: Parcel(NULL)" && ( echo "Ok" ; exit 0 ) || ( echo "Not installed" ; exit 106)
        ;;
    *)
	echo "SBS: Unknown subcommand: $1"
	exit 107
	;;
esac


