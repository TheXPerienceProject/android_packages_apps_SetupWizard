#!/bin/bash

# Carpeta raíz del source (por defecto es el directorio actual)
SOURCE_DIR="${1:-.}"

# Buscar todos los archivos relevantes
find "$SOURCE_DIR" -type f \( -name '*.xml' -o -name '*.java' -o -name '*.kt' -o -name '*.mk' -o -name '*.bp' -o -name '*.html' -o -name '*.md' \) | while read -r file; do
    # Evitar reemplazar "The LineageOS Project", pero cambiar los demás "LineageOS"
    sed -i -E '/The LineageOS Project/! s/\bLineageOS\b/XPerience/g' "$file"

    # Reemplazar enlaces lineageos.org → thexperienceproject.klozz.dev
    sed -i -E 's|https://lineageos\.org|https://thexperienceproject.klozz.dev|g' "$file"
done
