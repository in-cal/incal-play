# InCal Play [![version](https://img.shields.io/badge/version-0.3.0-green.svg)](https://in-cal.org) [![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![Build Status](https://travis-ci.com/in-cal/incal-play.svg?branch=master)](https://travis-ci.com/in-cal/incal-play)

Play Framework extended with basic readonly/crud controllers, deadbolt-backed security, json formatters, etc. 

#### Installation

All you need is **Scala 2.11**. To pull the library you have to add the following dependency to *build.sbt*

```
"org.in-cal" %% "incal-play" % "0.3.0"
```

or to *pom.xml* (if you use maven)

```
<dependency>
    <groupId>org.in-cal</groupId>
    <artifactId>incal-play_2.11</artifactId>
    <version>0.3.0</version>
</dependency>
```

#### Play

This library uses (and is compatible with) Play version **2.5.9**.