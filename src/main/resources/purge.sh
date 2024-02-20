#!/bin/bash
PURGE_PATH="./assets/css/purged"

echo "Purging and minifying css..."

echo "Removing old purged css..."
rm -rf $PURGE_PATH
echo "Creating new purged css directory..."
mkdir -p $PURGE_PATH
echo "Purging css..."
purgecss --config ./purgecss.config.js

# Minify the purged css
echo "Minifying purged css..."
minify $PURGE_PATH/main.css > $PURGE_PATH/main.min.css

# Remove the purged css
echo "Removing purged css not minified..."
rm $PURGE_PATH/main.css

echo "Done!"