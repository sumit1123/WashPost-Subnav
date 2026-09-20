apply(plugin = "checkstyle")
apply(plugin = "pmd")

tasks.register<Checkstyle>("checkstyle") {
    description = "Checks if the code meets standards"
    group = "verification"

    configFile = File("./qa-check/checkstyle.xml")
    source("src")
    include("**/*.java")
    exclude("**/gen/**")

    classpath = files()
    ignoreFailures = true
}

tasks.register<Pmd>("pmd") {
    description = "Run PMD"
    group = "verification"

    ruleSetFiles = files("./qa-check/pmd-ruleset.xml")
    ruleSets = mutableListOf()

    source("src")
    include("**/*.java")
    exclude("**/gen/**")

    reports {
        xml.getRequired().set(true)
        html.getRequired().set(false)
    }

    ignoreFailures = true
}
