package com.bank.ingestion.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.bank.ingestion",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    /**
     * Rule 1 — Domain purity.
        * The domain layer must not import any framework (Spring, JPA).
     * Framework coupling lives exclusively in adapters.
     */
    @ArchTest
    static final ArchRule domainHasNoFrameworkImports =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence.."
                    )
                    .because("the domain must be framework-agnostic");

    /**
     * Rule 2 — Domain isolation.
     * Domain must not depend on the application or adapter layers (no outward deps).
     */
    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayers =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..application..",
                            "..adapter.."
                    )
                    .because("domain is the innermost layer and must not know about outer rings");

    /**
     * Rule 3 — Application isolation.
     * The application layer must not depend on adapter implementations.
     * It may only depend on domain ports (interfaces).
     */
    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter..")
                    .because("application use-cases must be decoupled from framework adapters");
}
