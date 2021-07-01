fail2ban-watcher
===========

[![Build Status](https://travis-ci.com/romibuzi/fail2ban-watcher.svg?branch=master)](https://travis-ci.com/romibuzi/fail2ban-watcher)

### Demo

![demo](demo.gif)

### Requirements

Java 8 or newer

### Tests

```
./mvnw test
```

### Package

```
./mvnw package
```

### Run

```
java -jar target/fail2ban-watcher.jar
```

By default it will try to analyze `/var/lib/fail2ban/fail2ban.sqlite3`.
This can be configured with `FAIL2BAN_DB` environment variable :

```
FAIL2BAN_DB=/custom/path/to/fail2ban.sqlite3 java -jar target/fail2ban-watcher.jar
```

The top 10 banned IPs and countries will be displayed by default, this can be changed with the `NB_DISPLAY` environment variable (e.g `NB_DISPLAY=100`).

### Credits

This project includes IP2Location LITE data available from http://www.ip2location.com
