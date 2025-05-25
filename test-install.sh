#!/bin/bash
mvn clean install -Passembly
cp "target/d2d2-renderer-test.jar" ~/tmp/ -v

