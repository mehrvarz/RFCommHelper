rm -rf bin gen
ant -Ddbglog=true -f pre-build.xml && ant compile && cd bin/classes && jar cf ../rfcommhelper.jar * && cd ../..

