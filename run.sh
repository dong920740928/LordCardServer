#########################################################################
# File Name: run.sh
# Author: dong920740928
# mail: dong920740928@gmail.com
# Created Time: 2017年07月26日 星期三 18时46分35秒
#########################################################################

#!/bin/bash

HOME=/home/yizhe/workplace/team2/server/LordCardServer
cd $HOME

while true; do
    STATE=`cat $HOME/state`
    if [ "x$STATE" == "xrunning" ]; then
        /usr/lib/jdk1.8.0_131/bin/java \
            -Dfile.encoding=UTF-8 \
            -classpath /usr/lib/jdk1.8.0_131/jre/lib/charsets.jar:/usr/lib/jdk1.8.0_131/jre/lib/deploy.jar:/usr/lib/jdk1.8.0_131/jre/lib/ext/cldrdata.jar:/usr/lib/jdk1.8.0_131/jre/lib/ext/dnsns.jar:/usr/lib/jdk1.8.0_131/jre/lib/ext/jaccess.jar:/usr/lib/jdk1.8.0_131/jre/lib/ext/jfxrt.jar:/usr/lib/jdk1.8.0_131/jre/lib/ext/localedata.jar:/usr/lib/jdk1.8.0_131/jre/lib/ext/nashorn.jar:/usr/lib/jdk1.8.0_131/jre/lib/ext/sunec.jar:/usr/lib/jdk1.8.0_131/jre/lib/ext/sunjce_provider.jar:/usr/lib/jdk1.8.0_131/jre/lib/ext/sunpkcs11.jar:/usr/lib/jdk1.8.0_131/jre/lib/ext/zipfs.jar:/usr/lib/jdk1.8.0_131/jre/lib/javaws.jar:/usr/lib/jdk1.8.0_131/jre/lib/jce.jar:/usr/lib/jdk1.8.0_131/jre/lib/jfr.jar:/usr/lib/jdk1.8.0_131/jre/lib/jfxswt.jar:/usr/lib/jdk1.8.0_131/jre/lib/jsse.jar:/usr/lib/jdk1.8.0_131/jre/lib/management-agent.jar:/usr/lib/jdk1.8.0_131/jre/lib/plugin.jar:/usr/lib/jdk1.8.0_131/jre/lib/resources.jar:/usr/lib/jdk1.8.0_131/jre/lib/rt.jar:/home/yizhe/workplace/team2/server/LordCardServer/target/classes:/home/yizhe/.m2/repository/org/apache/logging/log4j/log4j-api/2.8.2/log4j-api-2.8.2.jar:/home/yizhe/.m2/repository/org/apache/logging/log4j/log4j-core/2.8.2/log4j-core-2.8.2.jar:/home/yizhe/.m2/repository/org/json/json/20160810/json-20160810.jar \
    server.MainServer
    else
        break
    fi
done

#    -javaagent:/home/yizhe/Tools/idea-IC-171.4694.70/lib/idea_rt.jar=41443:/home/yizhe/Tools/idea-IC-171.4694.70/bin \
