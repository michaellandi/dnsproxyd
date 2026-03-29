# dnsproxyd

**dnsproxyd** is a fast DNS proxy daemon written in Java. It sits between DNS clients and an upstream DNS server, forwarding queries while optionally filtering domain names via whitelist or blacklist rules. DNS responses can also be cached locally to speed up resolution on slow connections.

Built for Red Hat Linux but portable to any JRE-supported environment. Ships with init scripts for both Linux (`/etc/init.d`) and FreeBSD (`/etc/rc.d`).

## Features

- Transparent DNS proxy — forwards queries to an upstream resolver
- Whitelist or blacklist filtering by hostname
- Configurable redirect IP for blocked or unresolvable queries
- Persistent DNS cache with configurable TTL and path
- Native binary compilation via `gcj` (no JVM required at runtime)
- Init script with `start`, `stop`, `restart`, and `status` support

## Requirements

- `gcj` (GNU Compiler for Java) — for building native binaries
- Root access — listens on port 53 by default

## Installation

```bash
cd dnsproxyd
sudo bash install.sh
```

The install script compiles the Java source to native binaries via `gcj`, installs them to `/sbin/`, copies the init script, and registers the service with `chkconfig` on Linux.

After installation, copy and edit the configuration file:

```bash
sudo mkdir -p /etc/dnsproxyd
sudo cp dnsproxyd/dnsproxy.conf /etc/dnsproxyd/dnsproxy.conf
```

## Configuration

Configuration is read from `/etc/dnsproxyd/dnsproxy.conf`. Restart the daemon after making changes.

| Option | Default | Description |
|--------|---------|-------------|
| `FilterMode` | `2` | `0` = no filtering, `1` = whitelist, `2` = blacklist |
| `BlockedIP` | — | IP to redirect clients to when a request is blocked |
| `UnknownIP` | — | IP to redirect clients to when a request cannot be resolved |
| `Listen` | `53` | Port to listen on |
| `Blacklist` | `/etc/dnsproxyd/blacklist.dat` | Path to blacklist file (one hostname per line) |
| `Whitelist` | `/etc/dnsproxyd/whitelist.dat` | Path to whitelist file (one hostname per line) |
| `Cache` | `true` | Enable DNS caching |
| `CacheTime` | `7` | Number of days to retain cached entries |
| `CachePath` | `/tmp/dnscache.tmp` | Path to the cache file |
| `LogPath` | `/var/log/dnsproxyd` | Directory for log files |

## Service Management

```bash
sudo service dnsproxyd start
sudo service dnsproxyd stop
sudo service dnsproxyd restart
sudo service dnsproxyd status
```

## License

MIT License. See [LICENSE](LICENSE) for details.
