// .github/scripts/assign_reviewer.main.kts

// Required Dependencies:
// 1. github-api-1.321.jar: https://repo1.maven.org/maven2/org/kohsuke/github-api/1.321/github-api-1.321.jar
// 2. jackson-databind-2.17.0.jar: https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.17.0/jackson-databind-2.17.0.jar
// 3. jackson-core-2.17.0.jar: https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.17.0/jackson-core-2.17.0.jar
// 4. jackson-annotations-2.17.0.jar: https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.17.0/jackson-annotations-2.17.0.jar
// 5. commons-lang3-3.12.0.jar: https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar
// 6. commons-io-2.11.0.jar: https://repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar
//
// Place these .jar files in a new directory: .github/scripts/libs/

import org.kohsuke.github.GitHub
import org.kohsuke.github.GHUser
import org.kohsuke.github.GHRepository
import java.io.File
import java.util.concurrent.TimeUnit

fun findValidReviewerForFile(
    filePath: String,
    prAuthor: String,
    github: GitHub,
    authorizedLogins: Set<String>,
    nameToUsernameMap: Map<String, String>
): GHUser? {
    var process: Process? = null
    try {
        // Use %an to get the author's full name from the git commit
        process = ProcessBuilder("git", "log", "--pretty=format:%an", filePath)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        // Wait for the process to finish, with a timeout
        val finishedInTime = process.waitFor(10, TimeUnit.SECONDS)

        if (!finishedInTime) {
            println("  - Git log for '$filePath' took too long. Processing partial log.")
            process.destroyForcibly()
        }

        // Read the output, which will be partial if the process was destroyed
        val output = process.inputStream.bufferedReader().readText().lines()

        // Iterate through the history to find the first valid collaborator
        for (authorName in output) {
            if (authorName.isBlank()) {
                continue // Skip blank lines
            }

            println("  - Checking potential reviewer by git author name: $authorName")
            // Look up the git name in our map to find the corresponding GitHub username
            val githubUsername = nameToUsernameMap[authorName.lowercase()]

            if (githubUsername == null) {
                println("    -> No GitHub username mapping found for '$authorName'.")
                continue
            }

            // Check if the mapped username is authorized and not the PR author
            if (authorizedLogins.contains(githubUsername.lowercase()) && githubUsername.lowercase() != prAuthor.lowercase()) {
                try {
                    val user = github.getUser(githubUsername)
                    println("    -> Found valid user: ${user.login}")
                    return user // Found a valid reviewer, return them
                } catch (e: org.kohsuke.github.GHFileNotFoundException) {
                    println("    -> Could not find GitHub user '$githubUsername'.")
                } catch (e: Exception) {
                    println("    -> Error fetching user '$githubUsername': ${e.message}")
                }
            } else {
                println("    -> User '$githubUsername' is not in the authorized list or is the PR author.")
            }
        }
    } catch (e: Exception) {
        println("Error checking git log for $filePath: ${e.message}")
        // Ensure the process is destroyed in case of other errors
        process?.destroyForcibly()
    }

    // If the loop finishes, no valid reviewer was found in the file's history
    println("  - No valid past authorized user found for this file.")
    return null
}

fun main() {
    try {
        // --- Get environment variables ---
        val token = System.getenv("GITHUB_TOKEN")
        val prNumber = System.getenv("PR_NUMBER").toInt()
        val repoName = System.getenv("GITHUB_REPOSITORY")

        // --- Manually define the list of all possible reviewers ---
        val authorizedLogins = setOf(
            "kattim",
            "vs4505",
            "sgt2350",
            "erikdash",
            "david-kirsch",
            "ameenspost",
            "nehabalaji",
            "sabrina-chu",
            "diego-parra-wp",
            "rishi-wapo",
            "alejandrocwizeline"
        ).map { it.lowercase() }.toSet()

        // --- Map Git author names to GitHub usernames ---
        // IMPORTANT: Add mappings for your team members. The key should be the full name
        // they use in their git config, converted to lowercase. There can be duplicates of github name
        // if user has different local git configs.
        val nameToUsernameMap = mapOf(
            listOf("mukund katti") to "kattim",
            listOf("diego-parra-wp", "diego parra") to "diego-parra-wp",
            listOf("david-kirsch", "david kirsch") to "david-kirsch",
            listOf("sgt2350", "shreyas") to "sgt2350",
            listOf("ameenspost", "sheriff ameen") to "ameenspost",
            listOf("erik dash") to "erikdash",
            listOf("kolla, vijaya sankar", "kollav") to "vs4505",
            listOf("neha balaji") to "nehabalaji",
            listOf("sabrina chu") to "sabrina-chu",
            listOf("rishi-wapo") to "rishi-wapo",
            listOf("alejandrocwizeline", "alejandro cardona") to "alejandrocwizeline"
        ).flatMap { (names, username) ->
            names.map { it.lowercase() to username }
        }.toMap()

        println("Using manually defined list of ${authorizedLogins.size} authorized reviewers.")

        // --- Initialize GitHub client ---
        val github = GitHub.connectUsingOAuth(token)
        val repo = github.getRepository(repoName)
        val pr = repo.getPullRequest(prNumber)
        val prAuthor = pr.user.login

        println("Processing PR #$prNumber by $prAuthor in repo $repoName")

        // --- Get changed files and find reviewers ---
        val changedFiles = pr.listFiles()
        val reviewersToAdd = mutableSetOf<GHUser>()

        for (file in changedFiles) {
            println("Checking file: ${file.filename}")
            val validReviewer = findValidReviewerForFile(file.filename, prAuthor, github, authorizedLogins, nameToUsernameMap)
            if (validReviewer != null) {
                reviewersToAdd.add(validReviewer)
            }
        }

        // --- Assign reviewers ---
        if (reviewersToAdd.isNotEmpty()) {
            val reviewerLogins = reviewersToAdd.map { it.login }
            println("Requesting review from: $reviewerLogins")
            pr.requestReviewers(reviewersToAdd.toList())
        } else {
            println("No suitable reviewers found.")
        }

    } catch (e: Exception) {
        println("An error occurred in the main process: ${e.message}")
        e.printStackTrace()
        kotlin.system.exitProcess(1)
    }
}

main()