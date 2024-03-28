#!/bin/sh
JLINK_VM_OPTIONS=
DIR=`dirname $0`
$DIR/java $JLINK_VM_OPTIONS -m coderodde.WikiGameKillerFXJava/com.github.coderodde.wikipedia.game.killer.fx.WikiGameKillerFX "$@"
