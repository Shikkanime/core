#!/bin/bash
CSS_PURGED_PATH="./assets/css/purged"

echo "Purging and minifying css..."

echo "Removing old purged css..."
rm -rf $CSS_PURGED_PATH
echo "Creating new purged css directory..."
mkdir -p $CSS_PURGED_PATH
mkdir -p "$CSS_PURGED_PATH/assets/css"

# Moving the excludes files
echo "Moving the excludes files..."
mv ./templates/site/_layout.ftl .
mv ./templates/site/presentation.ftl .

echo "Purging css..."
purgecss --config ./purgecss.config.js --output "$CSS_PURGED_PATH"
mv "$CSS_PURGED_PATH/assets/css/main.css" "$CSS_PURGED_PATH/main.css"
mv "$CSS_PURGED_PATH/assets/css/bootstrap.min.css" "$CSS_PURGED_PATH/bootstrap.min.css"
rm -rf "$CSS_PURGED_PATH/assets"

# Moving back the excludes files
echo "Moving back the excludes files..."
mv _layout.ftl ./templates/site
mv presentation.ftl ./templates/site

# Minify the purged css
echo "Minifying purged css..."
minify $CSS_PURGED_PATH/main.css > $CSS_PURGED_PATH/main.min.css

# Remove the purged css
echo "Removing purged css not minified..."
rm $CSS_PURGED_PATH/main.css

echo "Done!"
# Pause
read -p "Press [Enter] key to continue..."