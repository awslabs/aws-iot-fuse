#!/bin/sh

IN=$1
OUT=$2

jq --slurp --raw-input '{"text": "\(.)", "mode": "markdown"}' < $1 | curl --data @- https://api.github.com/markdown | sed -e 's/user-content-//g' > $2
