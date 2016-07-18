#!/bin/bash

for (( i = 0; i < 10; i++ )); do
    java -agentpath:output/lib/libdislagent.jnilib -Xbootclasspath/a:output/lib/disl-bypass.jar:output/lib/dislre-dispatch.jar -Ddisl.instrumentationjarpath=example-inst1.jar -jar example-app1.jar &
    java -agentpath:output/lib/libdislagent.jnilib -Xbootclasspath/a:output/lib/disl-bypass.jar:output/lib/dislre-dispatch.jar -Ddisl.instrumentationjarpath=example-inst.jar -jar example-app.jar &
done