#/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source $DIR/../../../manageUtils.sh

githubProject sirdslets

BASE=$HGROOT/programs/games/SIRD

case "$1" in
mirror)
  syncHg  
;;

esac

