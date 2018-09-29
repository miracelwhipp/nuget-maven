#!/usr/bin/env bash

if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then

	openssl aes-256-cbc -K $encrypted_35d725154913_key -iv $encrypted_35d725154913_iv -in deployment/codesigning.asc.enc -out deployment/codesigning.asc -d

    gpg2 --fast-import --allow-non-selfsigned-uid deployment/codesigning.asc

    echo "decrypted file"
    head -n3 deployment/codesigning.asc

    echo "keys"
    gpg2 --list-keys

    echo "secret keys"
    gpg2 --list-secret-keys

fi