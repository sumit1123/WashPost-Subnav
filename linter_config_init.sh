#!/bin/bash

# Execute the specified .sh file
sh linter_installers.sh

# Ensure the .git/hooks directory exists
if [ ! -d ".git/hooks" ]; then
  echo "📁 .git/hooks directory does not exist. Creating it..."
  mkdir -p .git/hooks
  echo "✅ .git/hooks directory created successfully."
else
  echo "📁 .git/hooks directory already exists."
fi

# Create the pre-commit file in the .git/hooks/ directory
echo '#!/bin/bash

echo "🚀 Starting ktlintFormat and Detekt checks on modified files only..."

# Get only the modified files that are in staging
fileArray=($(git diff --cached --name-only --diff-filter=ACM | grep "\.kt$"))

# Print the files being modified in Git
echo "📝 Modified files in staging:"
for file in "${fileArray[@]}"; do
  echo " - $file"
done

# Check if there are Kotlin files in staging
if [ ${#fileArray[@]} -eq 0 ]; then
  echo "✅ No Kotlin files to check. Skipping ktlintFormat and Detekt checks."
  exit 0
fi

# Convert the array into a space-separated list for ktlint
ktlintInput=$(IFS=" "; echo "${fileArray[*]}")

# Apply formatting with ktlint --format to the modified files
echo "🔍 Running ktlintFormat on modified files..."
ktlintOutput=$(/opt/homebrew/bin/ktlint -F $ktlintInput 2>&1)
ktlintExitCode=$?

if [ $ktlintExitCode -ne 0 ]; then
  echo "❌ ktlintFormat encountered an error!"
  echo "$ktlintOutput"
  echo "***********************************************"
  echo "🚨           ktlintFormat failed              🚨"
  echo "🛠️  Please fix the issues above if you want clean code 🛠️"
  echo "***********************************************"
  # Do not exit, allow the commit
fi

echo "✅ ktlintFormat process completed! 🎉"

# After formatting, update the files in staging
for file in "${fileArray[@]}"; do
  git add "$file"
done

# Convert the array into a comma-separated list for Detekt
detektInput=$(IFS=,; printf "%s" "${fileArray[*]}")

# Run Detekt only on modified files
echo "🔍 Running Detekt on modified files..."
detektOutput=$(/opt/homebrew/bin/detekt --input "$detektInput" 2>&1)
detektExitCode=$?

# Handle Detekt errors (informative only)
if [ $detektExitCode -ne 0 ]; then
  echo "⚠️ Detekt issues detected (informative only):"
  echo "$detektOutput"
  echo "***********************************************"
  echo "🚨              Detekt check (informative)      🚨"
  echo "***********************************************"
  # Do not exit, allow the commit
else
  echo "✅ Detekt check passed (informative). 🎉"
fi

# Allow the commit even if Detekt or ktlint found issues
exit 0' > .git/hooks/pre-commit

# Grant execution permissions to the pre-commit file
chmod +x .git/hooks/pre-commit

echo "✅ Pre-commit hook created and configured successfully!"
