#! /bin/sh
# 
# Start this on the host that your sound-card is attached to, to enable receiption by the
# gstreamer audio probe.
#
# Synopsis: ./linux-audio-tcp.sh [device]
# [device] is an ALSA-Style sound device specification, e.g. "plughw:1" for the second 
# capture-capable audio-device present in the system. See "arecord -l" for the appropriate
# numbers
#
# DEVELOPERS NOTE: If you modify this pipeline to accomodate other clients, make sure 
# to keep the 'protocol=1' part, the receiver needs it for caps negotiation.
# 

if [ "x$1" != "x" ]
then
DEVICE="device=$1"
echo $DEVICE
else
DEVICE=""
fi

gst-launch-0.10 alsasrc $DEVICE ! tcpserversink protocol=1 sync-method=3


