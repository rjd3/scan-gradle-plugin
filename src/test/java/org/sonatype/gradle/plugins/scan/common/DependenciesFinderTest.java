/*
 * Copyright (c) 2020-present Sonatype, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sonatype.gradle.plugins.scan.common;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.scan.module.model.Artifact;
import com.sonatype.insight.scan.module.model.Module;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Usage;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DependenciesFinderTest
{
  private static final String COMMONS_COLLECTIONS_DEPENDENCY = "commons-collections:commons-collections:3.1";

  private DependenciesFinder finder;

  @Before
  public void setup() {
    finder = new DependenciesFinder();
  }

  @Test
  public void testFindResolvedDependencies_includeCompileDependencies() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyAndroidDependencies() {
    Project project = buildProject("_releaseCompile", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeAndroidDependencies() {
    Project project = buildProject("releaseCompileClasspath", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyAndroidDependenciesUsingVariant() {
    Project project = buildProject("variantProd_ReleaseCompile", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeAndroidDependenciesUsingVariant() {
    Project project = buildProject("variantProdReleaseCompileClasspath", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_omitTestDependencies() {
    Project project = buildProject(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false);
    assertThat(result).isEmpty();
  }

  @Test
  public void testFindResolvedDependencies_includeTestDependencies() {
    Project project = buildProject(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, true);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_includeCompileDependencies() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_includeAndroidDependencies() {
    Project project = buildProject("releaseCompileClasspath", true);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_omitTestDependencies() {
    Project project = buildProject(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false);
    assertThat(result).isEmpty();
  }

  @Test
  public void testFindResolvedArtifacts_includeTestDependencies() {
    Project project = buildProject(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, true);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testBuildModule_withBasicInfo() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Module module = finder.buildModule(project);

    assertThat(module).isNotNull();
    assertThat(module.getIdKind()).isEqualTo("gradle");
    assertThat(module.getPathname()).isEqualTo(project.getProjectDir().getAbsolutePath());
    assertThat(module.getId()).isEqualTo(project.getName());
  }

  @Test
  public void testBuildModule_withGroup() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    project.setGroup("groupTest");
    Module module = finder.buildModule(project);

    assertThat(module).isNotNull();
    assertThat(module.getId()).isEqualTo(project.getGroup() + ":" + project.getName());
  }

  @Test
  public void testBuildModule_withGroupAndVersion() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    project.setGroup("groupTest");
    project.setVersion("0.0.1");
    Module module = finder.buildModule(project);

    assertThat(module).isNotNull();
    assertThat(module.getId()).isEqualTo(project.getGroup() + ":" + project.getName() + ":" + project.getVersion());
  }

  @Test
  public void testFindModules_singleModule() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    List<Module> modules = finder.findModules(project, false);

    assertThat(modules).hasSize(1);

    Module module = modules.get(0);
    assertThat(module.getId()).isEqualTo(project.getName());
    assertThat(module.getConsumedArtifacts()).hasSize(1);

    Artifact artifact = module.getConsumedArtifacts().get(0);
    assertThat(artifact.getId()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testFindModules_multiModule() {
    Project parentProject = ProjectBuilder.builder().withName("parent").build();
    Project childProject = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false, parentProject);
    List<Module> modules = finder.findModules(parentProject, false);

    assertThat(modules).hasSize(2);

    Module parentModule = modules.get(0);
    assertThat(parentModule.getId()).isEqualTo(parentProject.getName());
    assertThat(parentModule.getConsumedArtifacts()).isEmpty();

    Module childModule = modules.get(1);
    assertThat(childModule.getId()).isEqualTo(parentProject.getName() + ":" + childProject.getName());
    assertThat(childModule.getConsumedArtifacts()).hasSize(1);

    Artifact artifact = childModule.getConsumedArtifacts().get(0);
    assertThat(artifact.getId()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testCreateCopyConfiguration() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Configuration configuration = finder.createCopyConfiguration(project);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getName()).isEqualTo("sonatypeCopyConfiguration");

    Usage expectedUsage = project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME);
    assertThat(configuration.getAttributes().getAttribute(Usage.USAGE_ATTRIBUTE)).isEqualTo(expectedUsage);
  }

  @Test
  public void testIsGradleVersionSupportedForAttributes() {
    assertThat(finder.isGradleVersionSupportedForAttributes("3.5")).isFalse();
    assertThat(finder.isGradleVersionSupportedForAttributes("3.5.1")).isFalse();
    assertThat(finder.isGradleVersionSupportedForAttributes("4.0")).isTrue();
    assertThat(finder.isGradleVersionSupportedForAttributes("4.0.1")).isTrue();
    assertThat(finder.isGradleVersionSupportedForAttributes("4.0.2")).isTrue();
    assertThat(finder.isGradleVersionSupportedForAttributes("4.1")).isTrue();
  }

  @Test
  public void testGetDependencies_ModuleDependencies() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Configuration originalConfiguration = project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME);

    Stream<ResolvedDependency> dependencies = finder.getDependencies(project, originalConfiguration,
        resolvedConfiguration -> resolvedConfiguration.getFirstLevelModuleDependencies().stream());

    assertThat(dependencies).isNotNull();

    List<ResolvedDependency> list = dependencies.collect(Collectors.toList());
    assertThat(list).hasSize(1);

    Set<ResolvedArtifact> artifacts = list.get(0).getAllModuleArtifacts();
    assertThat(artifacts).hasSize(1);
    assertThat(artifacts.iterator().next().getId().getComponentIdentifier().toString())
        .isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testGetDependencies_ModuleDependencies_WithError() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);

    Configuration originalConfiguration = mock(Configuration.class);
    when(originalConfiguration.getResolvedConfiguration()).thenThrow(ResolveException.class);
    when(originalConfiguration.getAllDependencies())
        .thenReturn(project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME).getAllDependencies());

    Stream<ResolvedDependency> dependencies = finder.getDependencies(project, originalConfiguration,
        resolvedConfiguration -> resolvedConfiguration.getFirstLevelModuleDependencies().stream());

    assertThat(dependencies).isNotNull();

    List<ResolvedDependency> list = dependencies.collect(Collectors.toList());
    assertThat(list).hasSize(1);

    Set<ResolvedArtifact> artifacts = list.get(0).getAllModuleArtifacts();
    assertThat(artifacts).hasSize(1);
    assertThat(artifacts.iterator().next().getId().getComponentIdentifier().toString())
        .isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testGetDependencies_ResolvedArtifacts() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Configuration originalConfiguration = project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME);

    Stream<ResolvedArtifact> dependencies = finder.getDependencies(project, originalConfiguration,
        resolvedConfiguration -> resolvedConfiguration.getResolvedArtifacts().stream());

    assertThat(dependencies).isNotNull();

    List<ResolvedArtifact> list = dependencies.collect(Collectors.toList());

    assertThat(list).hasSize(1);
    assertThat(list.get(0).getId().getComponentIdentifier().toString()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testGetDependencies_ResolvedArtifacts_WithError() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);

    Configuration originalConfiguration = mock(Configuration.class);
    when(originalConfiguration.getResolvedConfiguration()).thenThrow(ResolveException.class);
    when(originalConfiguration.getAllDependencies())
        .thenReturn(project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME).getAllDependencies());

    Stream<ResolvedArtifact> dependencies = finder.getDependencies(project, originalConfiguration,
        resolvedConfiguration -> resolvedConfiguration.getResolvedArtifacts().stream());

    assertThat(dependencies).isNotNull();

    List<ResolvedArtifact> list = dependencies.collect(Collectors.toList());

    assertThat(list).hasSize(1);
    assertThat(list.get(0).getId().getComponentIdentifier().toString()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  private Project buildProject(String configurationName, boolean needToCreateConfiguration) {
    return buildProject(configurationName, needToCreateConfiguration, null);
  }

  private Project buildProject(String configurationName, boolean needToCreateConfiguration, Project parent) {
    ProjectBuilder projectBuilder = ProjectBuilder.builder();
    if (parent != null) {
      projectBuilder.withParent(parent);
    }
    Project project = projectBuilder.build();

    project.getPluginManager().apply("java");
    project.getRepositories().mavenCentral();
    if (needToCreateConfiguration) {
      project.getConfigurations().create(configurationName);
    }
    DependencyHandler dependencyHandler = project.getDependencies();
    dependencyHandler.add(configurationName, COMMONS_COLLECTIONS_DEPENDENCY);
    return project;
  }
}
