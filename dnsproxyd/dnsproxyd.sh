#!/bin/sh
### BEGIN INIT INFO
# Provides: dnsproxyd
# Required-Start: $network
# Required-Stop: $network
# Default-Start: 3 4 5
# Description: Starts the DNS filtering proxy server..
### END INIT INFO
. /etc/init.d/functions
prog="dnsproxyd"

start() {
	echo -n $"Starting $prog: "
	cd /sbin
	./dnsproxyd &
	touch /var/lock/subsys/dnsproxyd
	success $"$prog server startup"
	echo
}

stop() {
	echo -n $"Stopping $prog: "
	cd /sbin
	./dnsproxyd-stop
	rm -f /var/lock/subsys/dnsproxyd
	success $"$prog server stop"
	echo
}

restart() {
	stop
	sleep 3
	start
}

case $1 in
	start) start;;
	stop)  stop;;
	status) status $prog;;
	restart|reload|condrestart) restart;;
	*) echo $"Usage: $0 {start|stop|restart|status}"
	   exit 1
esac
exit 0
