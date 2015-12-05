#/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source $DIR/../../../manageUtils.sh

mirroredProject sirdslets

BASE=$HGROOT/programs/games/SIRD

case "$1" in
mirror)
  syncHg  
;;
release) 
  jar cf sirdslets.jar -C output .
  echo password | jarsigner -keystore keystore.password.jks -tsa https://tsa.safecreative.org/ sirdslets.jar selfsigned
  $HGROOT/sites/web5/upload.sh sirdslets.jar output/sirdslet_page.html bin/games/sirdslets/
  syncHg  
;;

esac

