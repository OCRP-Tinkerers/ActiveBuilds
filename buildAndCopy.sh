#! /bin/bash
rm ./build/libs/*.*
gradle shadowJar
rm ~/bin/spigot_testserv/plugins/ActiveBuilds-*
cp ./build/libs/ActiveBuilds-*_mc*.jar ~/bin/spigot_testserv/plugins
