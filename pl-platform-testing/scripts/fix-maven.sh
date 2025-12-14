#!/bin/bash
# Script to fix Maven installation in WSL
# This script attempts to fix the missing Google Guice dependency issue

echo ""
echo "========================================"
echo "  Fixing Maven Installation"
echo "========================================"
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo "This script needs root privileges. Please run with sudo."
    echo "Usage: sudo bash scripts/fix-maven.sh"
    exit 1
fi

echo "Attempting to reinstall Maven..."
echo ""

# Try to reinstall Maven
if command -v apt-get &> /dev/null; then
    echo "Using apt-get to reinstall Maven..."
    apt-get update
    apt-get install --reinstall maven -y
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "Maven reinstalled successfully!"
        echo "Try running: mvn --version"
    else
        echo ""
        echo "Reinstallation failed. Trying alternative fix..."
        echo ""
        echo "Installing missing Google Guice dependency..."
        
        # Download guice jar if needed
        GUICE_JAR="/usr/share/maven/lib/guice.jar"
        if [ ! -f "$GUICE_JAR" ]; then
            echo "Downloading Google Guice..."
            wget -O "$GUICE_JAR" https://repo1.maven.org/maven2/com/google/inject/guice/4.2.3/guice-4.2.3.jar
        fi
    fi
else
    echo "Package manager not found. Please manually reinstall Maven."
    echo ""
    echo "For Ubuntu/Debian:"
    echo "  sudo apt-get update"
    echo "  sudo apt-get install --reinstall maven"
    echo ""
    echo "Or download Maven manually from: https://maven.apache.org/download.cgi"
fi

echo ""
echo "If Maven still doesn't work, use the javac scripts instead:"
echo "  bash scripts/compile.sh"
echo "  bash scripts/run-tests.sh simple"
echo ""

