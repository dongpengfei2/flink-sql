#!/bin/bash
SELF=`hostname`
if [ -z "$NODE_LIST" ]; then
  echo
  echo Error: NODE_LIST environment variable must be set in .bash_profile
  exit 1
fi

for i in $NODE_LIST; do
  echo $i
  if [ ! $i = $SELF ]; then
    if [ $1 = "-r" ]; then
      scp -oStrictHostKeyChecking=no -r $2 $i:$3
    else
      scp -oStrictHostKeyChecking=no $1 $i:$2
    fi
  fi
done
wait

