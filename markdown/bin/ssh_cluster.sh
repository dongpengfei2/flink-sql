#!/bin/bash
if [ -z "$NODE_LIST" ]; then
  echo
  echo Error: NODE_LIST environment variable must be set in .bash_profile
  exit 1
fi

if [[ $1 = '--background' ]]; then
  shift
  for i in $NODE_LIST; do
    echo $i
    ssh -oStrictHostKeyChecking=no -n $i "$@" &
  done
else
  for i in $NODE_LIST; do
    echo $i
    ssh -oStrictHostKeyChecking=no $i "$@"
  done
fi
wait

