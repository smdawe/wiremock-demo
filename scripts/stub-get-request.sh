#!/bin/bash

curl -X POST --data '{ "request": { "url": "/test", "method": "GET" }, "response": { "status": 200, "body": "Hello!\n" }}' http://localhost:9999/__admin/mappings/new
