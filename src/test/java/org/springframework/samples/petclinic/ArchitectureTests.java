package org.springframework.samples.petclinic;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.library.Architectures;
import jakarta.persistence.Entity;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.slf4j.Logger;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.ConditionEvent.createMessage;
import static com.tngtech.archunit.lang.SimpleConditionEvent.satisfied;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.conditions.ArchConditions.dependOnClassesThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.not;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.Configuration.consideringOnlyDependenciesInDiagram;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.adhereToPlantUmlDiagram;
import static de.rweisleder.archunit.spring.SpringAnnotationPredicates.springAnnotatedWith;
import static de.rweisleder.archunit.spring.framework.SpringComponentPredicates.springController;

@AnalyzeClasses(packagesOf = PetClinicApplication.class, importOptions = DoNotIncludeTests.class)
class ArchitectureTests {

	@ArchTest
	ArchRule ControllerNaming = classes()
		.that().areAnnotatedWith(Controller.class)
		.or().haveSimpleNameEndingWith("Controller")
		.should().beAnnotatedWith(Controller.class)
		.andShould().haveSimpleNameEndingWith("Controller")
		.because("controller should be easy to find");

	@ArchTest
	ArchRule RequestMappingMethods = methods()
		.that(are(springAnnotatedWith(RequestMapping.class)))
		.should().beDeclaredInClassesThat(are(springController()));

	@ArchTest
	ArchRule DependenciesBetweenModules = CompositeArchRule
		.of(
			classes()
				.that().resideInAPackage("..owner..")
				.should(not(dependOnClassesThat(resideInAPackage("..vet..")))))
		.and(
			classes()
				.that().resideInAPackage("..vet..")
				.should(not(dependOnClassesThat(resideInAPackage("..owner..")))));

	@ArchTest
	@SuppressWarnings("DataFlowIssue")
	ArchRule PlantUmlDiagram = classes()
		.should(adhereToPlantUmlDiagram(
			getClass().getResource("/modules.puml"),
			consideringOnlyDependenciesInDiagram())
		);

	@ArchTest
	ArchRule Layers = Architectures.layeredArchitecture()
		.consideringOnlyDependenciesInLayers()
		.layer("Controller").definedBy(annotatedWith(Controller.class))
		.layer("Domain").definedBy(assignableTo(Repository.class))
		.layer("Persistence").definedBy(annotatedWith(Entity.class))
		.whereLayer("Controller").mayNotBeAccessedByAnyLayer()
		.whereLayer("Domain").mayOnlyBeAccessedByLayers("Controller")
		.whereLayer("Persistence").mayOnlyBeAccessedByLayers("Controller", "Domain");

	@ArchTest
	ArchRule jMoleculesLayers = JMoleculesArchitectureRules.ensureLayering();

	@ArchTest
	ArchRule ControllerShouldLog = freeze(methods()
		.that().areAnnotatedWith(PostMapping.class)
		.should(log()));

	private static ArchCondition<? super JavaMethod> log() {
		return new ArchCondition<>("log") {
			@Override
			public void check(JavaMethod item, ConditionEvents events) {
				List<JavaMethodCall> loggingCalls = item.getMethodCallsFromSelf().stream()
					.filter(call -> call.getTargetOwner().isEquivalentTo(Logger.class))
					.toList();

				if (loggingCalls.isEmpty()) {
					events.add(violated(item, createMessage(item, "does not log")));
				} else {
					for (JavaMethodCall loggingCall : loggingCalls) {
						events.add(satisfied(item, loggingCall.getDescription()));
					}
				}
			}
		};
	}

}
