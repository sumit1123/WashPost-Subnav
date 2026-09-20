import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.washingtonpost.convention.configureCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class ComposeConventionPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            val extension: CommonExtension? = try {
                extensions.getByType<ApplicationExtension>()
            } catch (ex: Exception) {
                try {
                    extensions.getByType<LibraryExtension>()
                } catch (ex: Exception) {
                    null
                }
            }
            extension?.let {
                configureCompose(it)
            }
        }

    }
}
