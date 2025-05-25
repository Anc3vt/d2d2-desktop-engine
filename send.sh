#!/bin/bash
cd $DIR_SHARED/tmp
scp -r src $DELL:
scp -r d2d2-renderer-test.jar $DELL:
