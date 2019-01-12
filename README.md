akka-fail2ban-watcher
===========

[![Build Status](https://travis-ci.org/romibuzi/akka-fail2ban-watcher.svg?branch=master)](https://travis-ci.org/romibuzi/akka-fail2ban-watcher)

### Demo

![demo](demo.gif)

### Requirements

Java8 or newer and recent version of [maven](https://maven.apache.org/) (> 3.5.4)

### Tests

```
mvn clean test
```

### Package

```
mvn package
```

### Run

```
java -jar target/akka-fail2ban-watcher-0.0.1-shaded.jar
```

By default it will try to watch `/var/lib/fail2ban/fail2ban.sqlite3`.
This can be configured with `FAIL2BAN_DB` environment variable :

```
FAIL2BAN_DB=/custom/path/to/fail2ban.sqlite3 java -jar target/akka-fail2ban-watcher-0.0.1-shaded.jar
```
