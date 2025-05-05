package org.springframework.samples.petclinic;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;

@Disabled("for manual execution")
@DisplayNameGeneration(ReplaceUnderscores.class)
class ArchUnitPerformanceTest {

	@Test
	void imports_classpath_with_ten_thousands_of_classes_within_one_minute() {
		StopWatch stopWatch = new StopWatch();

		stopWatch.start();
		JavaClasses classesInClasspath = new ClassFileImporter().importClasspath();
		stopWatch.stop();

		System.out.printf("analyzed packages: %s%n", analyzePackages(classesInClasspath));
		System.out.printf("classpath contains %,d classes%n", classesInClasspath.size());
		System.out.printf("analysis took %.3f seconds%n", stopWatch.getTotalTimeSeconds());

		assertThat(classesInClasspath).hasSizeGreaterThan(50_000);
		assertThat(stopWatch.getTotalTimeSeconds()).isLessThanOrEqualTo(60);
	}

	private String analyzePackages(JavaClasses javaClasses) {
		SortedMap<String, Long> numberOfClassesPerPackage = numberOfClassesPerPackage(javaClasses);
		return prettyPrint(numberOfClassesPerPackage);
	}

	private SortedMap<String, Long> numberOfClassesPerPackage(JavaClasses javaClasses) {
		return javaClasses.stream().collect(
			groupingBy(
				javaClass -> firstTwoPackageNameElements(javaClass.getPackage()),
				TreeMap::new,
				counting())
		);
	}

	private String firstTwoPackageNameElements(JavaPackage javaPackage) {
		String packageName = javaPackage.getName();
		if (packageName.isBlank()) {
			return "<empty>";
		}

		int firstDotIndex = packageName.indexOf('.');
		int secondDotIndex = -1;
		if (firstDotIndex != -1) {
			secondDotIndex = packageName.indexOf('.', firstDotIndex + 1);
		}

		return secondDotIndex != -1 ? packageName.substring(0, secondDotIndex) : packageName;
	}

	private String prettyPrint(Map<?, ?> map) {
		StringBuilder sb = new StringBuilder(lineSeparator());
		map.forEach((key, value) -> sb.append('\t').append(key).append(" = ").append(value).append(lineSeparator()));
		return sb.toString();
	}
}
