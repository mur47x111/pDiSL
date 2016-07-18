#!/bin/bash

## declare an array variable
declare -a arr=("dispatch" "dynamic" "guard" "marker" "scope" "smoke" "static")
# declare -a arr=("dynamic")

mkdir -p "testAll-output"

## now loop through the above array
for i in "${arr[@]}"
do
   ant -f examples/"$i"/build.xml run > "testAll-output/$i.txt" 2>&1
   echo "Tested: $i"
done