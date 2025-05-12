#!/usr/bin/env sh

xargs -rt -a /atp-ram/application.pid kill -SIGTERM
sleep 29
