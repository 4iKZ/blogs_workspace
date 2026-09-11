package com.blog.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 分层依赖约束：service 层不得依赖 controller 层。
 */
class LayeringArchitectureTest {

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.blog");

    @Test
    void serviceLayerShouldNotDependOnControllerLayer() {
        org.junit.jupiter.api.Assertions.assertTrue(
                MAIN_CLASSES.size() > 0, "ArchUnit 未导入任何类，规则将空跑通过");

        ArchRule rule = noClasses()
                .that().resideInAPackage("com.blog.service..")
                .should().dependOnClassesThat().resideInAPackage("com.blog.controller..");

        rule.check(MAIN_CLASSES);
    }
}
