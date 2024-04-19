#!/bin/bash
CSS_PURGED_PATH="./assets/css/purged"

echo "Purging and minifying css..."

echo "Removing old purged css..."
rm -rf $CSS_PURGED_PATH
echo "Creating new purged css directory..."
mkdir -p $CSS_PURGED_PATH
echo "Purging css..."
purgecss --config ./purgecss.config.js

# Minify the purged css
echo "Minifying purged css..."
minify $CSS_PURGED_PATH/main.css > $CSS_PURGED_PATH/main.min.css

# Remove the purged css
echo "Removing purged css not minified..."
rm $CSS_PURGED_PATH/main.css

echo "Done!"