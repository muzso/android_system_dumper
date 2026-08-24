plugins {
    kotlin("jvm")
    `java-test-fixtures`
    alias(libs.plugins.pitest)
    alias(libs.plugins.kover)
}

dependencies {
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.apache.commons.validator)
    implementation(libs.zip4j)

    testFixturesApi(libs.kotlinx.coroutines.test)
    testFixturesApi(libs.junit)
    testFixturesApi(libs.mockk)
    testFixturesApi(libs.truth)
    testFixturesApi(libs.turbine)

    testImplementation(libs.junit.jupiter)
}

kover {
    reports {
        total {
            log {
                onCheck = true
            }
            xml {
                onCheck = true
            }
            html {
                onCheck = true
            }
            verify {
                rule("Domain Quality Gate") {
                    minBound(80)
                }
            }
        }
    }
}

pitest {
    targetClasses.set(listOf("hu.muzso.android_system_dumper.domain.*"))
    pitestVersion.set("1.17.0")
    threads.set(Runtime.getRuntime().availableProcessors())
    outputFormats.set(listOf("XML", "HTML"))
    timestampedReports.set(false)
    mutationThreshold.set(80)
}
