package world.nobug.tdd.di;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.collections.Sets;

/**
 * 因为TDD的测试主要是一种里程碑，帮助我们驱动开发的，它并不是真的站在软件测试的角度上去写的。
 *
 * 开发人员所写的测试，和测试人员所希望看到的测试的类型其实是不同的。测试人员更多的是关注测试的完备性、对条件的覆盖。
 * 这两种测试之间是存在鸿沟的，需要刻意的调整和梳理。
 *
 * 一旦我们把TDD测试的功能写完，其实我们可以通过扩展（不能讲是重构了），把它 Convert 成一个更接近于测试需要的测试。
 *
 * 因为这个时候测试的骨架已经形成，我们只需要把它变成参数化或是数据驱动的方式去做，使测试可以覆盖更大范围的场景。
 */
public class ContextTest {
    // 通过改写 ContainerTest 中的测试，使其更加靠近对外接口的形式，也更加文档化

    ContextConfig config;

    @BeforeEach
    public void setUp(){
        config = new ContextConfig();
    }

    // 组件构造相关的测试类
    @Nested
    public class TypeBinding {

        // instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            // 创建一个实现了 Component 接口的匿名内部类实例
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();
            Class<TestComponent> component = TestComponent.class;
            Optional<TestComponent> component1 = context.get(ComponentRef.of(component));
            assertSame(instance, component1.get());
        }

        // 将一个测试泛化为多个测试，分别测试根据：构造器注入、字段注入和方法注入的情况
        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends TestComponent> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(TestComponent.class, componentType);

            Context context = config.getContext();
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class))
            );
        }


        static class ConstructorInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements TestComponent {
            @Inject
            Dependency dependency; // 目前不支持注入私有字段

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            public void install(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        // component does not exist
        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Context context = config.getContext();
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        // could get Provider<T> from context
        @Test
        public void should_retrieve_provider_bind_type_as_provider() {
            TestComponent component = new TestComponent() {
            };
            config.bind(TestComponent.class, component);
            Context context = config.getContext();

            Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>() {}).get();

            assertSame(component, provider.get());
        }

        @Test
        public void should_not_retrieve_provider_bind_type_as_unsupported_container() {
            TestComponent component = new TestComponent() {
            };
            config.bind(TestComponent.class, component);
            Context context = config.getContext();

            Optional<List<TestComponent>> components = context.get(new ComponentRef<List<TestComponent>>() {});

            assertFalse(components.isPresent());
        }

        @Nested
        public class WithQualifier {
            // binding component with qualifier
            // binding component with qualifiers
            @Test
            public void should_bind_instance_with_multi_qualifiers() {
                TestComponent component = new TestComponent() {
                };
                config.bind(TestComponent.class, component, new NamedLiteral("ChosenOne"), new AnotherOneLiteral());

                Context context = config.getContext();

                TestComponent
                        chosenOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
                TestComponent
                        anotherOne = context.get(ComponentRef.of(TestComponent.class, new AnotherOneLiteral())).get();

                assertSame(chosenOne, anotherOne);
            }
            @Test
            public void should_bind_component_with_multi_qualifiers() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ConstructorInjection.class, ConstructorInjection.class, new NamedLiteral("ChosenOne"),
                        new AnotherOneLiteral());

                Context context = config.getContext();

                ConstructorInjection chosenOne =
                        context.get(ComponentRef.of(ConstructorInjection.class, new NamedLiteral("ChosenOne"))).get();
                 ConstructorInjection anotherOne =
                         context.get(ComponentRef.of(ConstructorInjection.class, new AnotherOneLiteral())).get();

                assertSame(dependency, chosenOne.dependency());
                assertSame(dependency, anotherOne.dependency());
            }

            // TODO throw illegal component if illegal qualifier
            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                TestComponent component = new TestComponent() {
                };
                assertThrows(IllegalComponentException.class, () -> config.bind(TestComponent.class, component, new TestLiteral()));
            }
            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class,
                        () -> config.bind(ConstructorInjection.class, ConstructorInjection.class, new TestLiteral()));
            }
        }

    }

    @Nested
    public class DependencyCheck {

        // dependencies not exist
        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> componentType) {
            config.bind(TestComponent.class, componentType);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });

            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(TestComponent.class, exception.getComponent());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", DependencyCheck.MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Field Injection", DependencyCheck.MissingDependencyField.class)),
                    Arguments.of(Named.of("Method Injection", DependencyCheck.MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider Inject Constructor", DependencyCheck.MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider Inject Field", DependencyCheck.MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider Inject Method", DependencyCheck.MissingDependencyProviderMethod.class))
            );
        }

        static class MissingDependencyConstructor implements TestComponent {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            @Inject
            public void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency){
            }
        }

        static class MissingDependencyProviderField implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            public void install(Provider<Dependency> dependency){
            }
        }


        // cyclic dependencies
        // A -> B -> A
        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies(Class<? extends TestComponent> componentType,
                                                                  Class<? extends Dependency> dependencyType) {
            config.bind(TestComponent.class, componentType);
            config.bind(Dependency.class, dependencyType);

            CyclicDependenciesException exception =
                    assertThrows(CyclicDependenciesException.class, () -> config.getContext());

            Set<Class<?>> classes = Sets.newSet(exception.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue(classes.contains(Dependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(Named.of("Constructor Injection", DependencyCheck.CyclicComponentInjectConstructor.class),
                    Named.of("Field Injection", DependencyCheck.CyclicComponentInjectField.class),
                    Named.of("Method Injection", DependencyCheck.CyclicComponentInjectMethod.class))) {
                for (Named dependency : List.of(Named.of("Constructor Injection", DependencyCheck.CyclicDependencyInjectConstructor.class),
                        Named.of("Field Injection", DependencyCheck.CyclicDependencyInjectField.class),
                        Named.of("Method Injection", DependencyCheck.CyclicDependencyInjectMethod.class))) {
                    arguments.add(Arguments.of(component, dependency));
                }
            }
            return arguments.stream();
        }

        static class CyclicComponentInjectConstructor implements TestComponent {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements TestComponent {
            @Inject
            public void install(Dependency dependency) {
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            TestComponent component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            public void install(TestComponent component) {
            }
        }

        // A -> B -> C -> A
        @ParameterizedTest(name = "transitive cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        public void should_throw_exception_if_transitive_cyclic_dependencies(Class<? extends TestComponent> componentType,
                                                                            Class<? extends Dependency> dependencyType,
                                                                            Class<? extends AnotherDependency> anotherDependencyType) {
            config.bind(TestComponent.class, componentType);
            config.bind(Dependency.class, dependencyType);
            config.bind(AnotherDependency.class, anotherDependencyType);

            CyclicDependenciesException exception =
                    assertThrows(CyclicDependenciesException.class, () -> config.getContext());

            List<Class<?>> components = Arrays.stream(exception.getComponents()).toList();

            assertEquals(3, components.size());
            assertTrue(components.contains(TestComponent.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(Named.of("Constructor Injection", DependencyCheck.CyclicComponentInjectConstructor.class),
                    Named.of("Field Injection", DependencyCheck.CyclicComponentInjectField.class),
                    Named.of("Method Injection", DependencyCheck.CyclicComponentInjectMethod.class))) {
                for (Named dependency : List.of(Named.of("Constructor Injection", DependencyCheck.CyclicDependencyInjectConstructorWithAnotherDependency.class),
                        Named.of("Field Injection", DependencyCheck.CyclicDependencyInjectFieldWithAnotherDependency.class),
                        Named.of("Method Injection", DependencyCheck.CyclicDependencyInjectMethodWithAnotherDependency.class))) {
                    for (Named anotherDependency : List.of(Named.of("Constructor Injection", DependencyCheck.CyclicDependencyInjectConstructorWithComponent.class),
                            Named.of("Field Injection", DependencyCheck.CyclicDependencyInjectFieldWithComponent.class),
                            Named.of("Method Injection", DependencyCheck.CyclicDependencyInjectMethodWithComponent.class))) {
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
                    }
                }
            }
            return arguments.stream();
        }

        static class CyclicDependencyInjectConstructorWithAnotherDependency implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructorWithAnotherDependency(AnotherDependency anotherDependency) {
            }
        }

        static class CyclicDependencyInjectFieldWithAnotherDependency implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        static class CyclicDependencyInjectMethodWithAnotherDependency implements Dependency {
            @Inject
            public void install(AnotherDependency anotherDependency) {
            }
        }

        static class CyclicDependencyInjectConstructorWithComponent implements AnotherDependency {
            @Inject
            public CyclicDependencyInjectConstructorWithComponent(TestComponent component) {
            }
        }

        static class CyclicDependencyInjectFieldWithComponent implements AnotherDependency {
            @Inject
            TestComponent component;
        }

        static class CyclicDependencyInjectMethodWithComponent implements AnotherDependency {
            @Inject
            public void install(TestComponent component) {
            }
        }

        static class CyclicDependencyProviderInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderInjectConstructor(Provider<TestComponent> component) {
            }
        }
        @Test
        public void should_not_throw_exception_if_cyclic_dependencies_with_provider() {
            config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderInjectConstructor.class);

            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
        }

        static class CyclicComponentProviderInjectConstructor implements TestComponent {
            @Inject
            public CyclicComponentProviderInjectConstructor(Provider<Dependency> dependency) {
            }
        }
        @Test
        public void should_not_throw_exception_if_cyclic_dependencies_with_providers() {
            config.bind(TestComponent.class, CyclicComponentProviderInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderInjectConstructor.class);

            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
        }

        @Nested
        public class WithQualifier {
            // TODO dependency missing if qualifier not match
            // TODO check cyclic dependencies with qualifier
        }
    }
}

record NamedLiteral(String value) implements jakarta.inject.Named {
    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }
}

@java.lang.annotation.Documented
@java.lang.annotation.Retention(RUNTIME)
@jakarta.inject.Qualifier
@interface AnotherOne {
}

record AnotherOneLiteral() implements AnotherOne {
    @Override
    public Class<? extends Annotation> annotationType() {
        return AnotherOne.class;
    }
}

record TestLiteral() implements Test {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}
