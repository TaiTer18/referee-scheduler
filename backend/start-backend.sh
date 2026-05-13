#!/bin/bash

set -e

set -a
source .env
set +a

brew services start postgresql
mvn spring-boot:run