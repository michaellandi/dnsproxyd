#!/bin/bash
set -e 

echo "Initializing..."
echo "Creating native object files from java source."
gcj -c -g -O *.java
echo "Linking native binaries from C source."
echo "Building: dnsproxyd"
gcj --main=DNSProxy -o dnsproxyd *.o
echo "Building: dnsproxyd-stop"
gcj --main=DNSProxyStop -o dnsproxyd-stop DNSProxyStop.o
echo "Copying files."
cp dnsproxyd /sbin/
cp dnsproxyd-stop /sbin/

if [ `uname -s` = "FreeBSD" ]
then
	cp dnsproxyd.sh /etc/rc.d/dnsproxyd
elif [ `uname -s` = "Linux" ]
then
	cp dnsproxyd.sh /etc/init.d/dnsproxyd
fi

echo "Changing binary permissions."
chmod 700 /sbin/dnsproxyd
chmod 700 /sbin/dnsproxyd-stop
chmod 700 /etc/init.d/dnsproxyd

echo "Platform specific service registration."
os=`uname -s`
case $os in
	Linux) chkconfig --add dnsproxyd;;
	*);;
esac
echo "Cleaning up..."
rm -f dnsproxyd
rm -f dnsproxyd-stop
rm -f *.o
echo "Completed."
