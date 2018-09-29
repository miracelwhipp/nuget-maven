#!/usr/bin/env bash

if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then

    mvn deploy --settings deployment/settings.xml -e
	exit

else

    mvn install -e
    exit

fi