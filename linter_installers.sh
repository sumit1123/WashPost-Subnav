#!/bin/bash

# Check if Homebrew is installed
if ! command -v brew &> /dev/null; then
  echo "🍺 Homebrew is not installed. Installing Homebrew..."
  # Install Homebrew
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  
  # Verify if Homebrew installation was successful
  if ! command -v brew &> /dev/null; then
    echo "❌ Failed to install Homebrew. Please install it manually."
    exit 1
  fi

  echo "✅ Homebrew installed successfully!"
else
  echo "🍺 Homebrew is already installed."
fi

# Ensure Homebrew is up to date
echo "🔄 Updating Homebrew..."
brew update

# Configure Homebrew environment variables for the current user
USER_HOME=$(eval echo "~$USER")
ZPROFILE="$USER_HOME/.zprofile"
BASH_PROFILE="$USER_HOME/.bash_profile"

# Configure for zsh (.zprofile)
if [ -n "$ZSH_VERSION" ] || [ "$(basename "$SHELL")" = "zsh" ]; then
  echo "🔧 Configuring Homebrew environment variables in $ZPROFILE..."
  echo '' >> "$ZPROFILE"
  echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> "$ZPROFILE"
  eval "$(/opt/homebrew/bin/brew shellenv)"
  echo "✅ Homebrew environment variables configured for zsh."
fi

# Configure for bash (.bash_profile)
if [ -n "$BASH_VERSION" ] || [ "$(basename "$SHELL")" = "bash" ]; then
  echo "🔧 Configuring Homebrew environment variables in $BASH_PROFILE..."
  echo '' >> "$BASH_PROFILE"
  echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> "$BASH_PROFILE"
  eval "$(/opt/homebrew/bin/brew shellenv)"
  echo "✅ Homebrew environment variables configured for bash."
fi

# Check if Detekt is installed
if ! brew list detekt &> /dev/null; then
  echo "🔍 detekt is not installed. Installing detekt..."
  brew install detekt
  echo "✅ detekt installed successfully!"
else
  echo "🔍 detekt is already installed."
fi

# Check if ktlint is installed
if ! brew list ktlint &> /dev/null; then
  echo "🔍 ktlint is not installed. Installing ktlint..."
  brew install ktlint
  echo "✅ ktlint installed successfully!"
else
  echo "🔍 ktlint is already installed."
fi

echo "🚀 All required tools are installed and up to date!"
