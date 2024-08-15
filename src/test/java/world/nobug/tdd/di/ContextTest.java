package world.nobug.tdd.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            assertSame(instance, config.getContext().get(Component.class).get());
        }

        // 将一个测试泛化为多个测试，分别测试根据：构造器注入、字段注入和方法注入的情况
        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);

            Optional<Component> component = config.getContext().get(Component.class);

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


        static class ConstructorInjection implements Component {
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

        static class FieldInjection implements Component {
            @Inject
            Dependency dependency; // 目前不支持注入私有字段

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements Component {
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
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        // could get Provider<T> from context
        @Test
        public void should_retrieve_provider_bind_type_as_provider() {
            Component component = new Component() {
            };
            config.bind(Component.class, component);
            Context context = config.getContext();

            ParameterizedType type = new TypeLiteral<Provider<Component>>(){}.getType();
            Provider provider = (Provider<Component>)context.get(type).get();

            assertSame(component, provider.get());
        }

        @Test
        public void should_not_retrieve_provider_bind_type_as_unsupported_container() {
            Component component = new Component() {
            };
            config.bind(Component.class, component);
            Context context = config.getContext();

            ParameterizedType type = new TypeLiteral<List<Component>>(){}.getType();

            assertFalse(context.get(type).isPresent());
        }

        @Test
        @Disabled
        public void java_api() {
            Component component = new Component() {
            };
            ParameterizedType type = new TypeLiteral<Provider<Component>>(){}.getType();

            assertEquals(Provider.class, type.getRawType());
            assertEquals(Component.class, type.getActualTypeArguments()[0]);
        }

        static abstract class TypeLiteral<T> {
            public ParameterizedType getType() {
                return (ParameterizedType) ((ParameterizedType)(getClass().getGenericSuperclass())).getActualTypeArguments()[0];
            }
        }

    }

    @Nested
    public class DependencyCheck {

        // dependencies not exist
        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends Component> componentType) {
            config.bind(Component.class, componentType);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });

            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", DependencyCheck.MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Field Injection", DependencyCheck.MissingDependencyField.class)),
                    Arguments.of(Named.of("Method Injection", DependencyCheck.MissingDependencyMethod.class))
            );
        }

        static class MissingDependencyConstructor implements Component{
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements Component {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements Component {
            @Inject
            public void install(Dependency dependency) {
            }
        }


        // cyclic dependencies
        // A -> B -> A
        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies(Class<? extends Component> componentType,
                                                                  Class<? extends Dependency> dependencyType) {
            config.bind(Component.class, componentType);
            config.bind(Dependency.class, dependencyType);

            CyclicDependenciesException exception =
                    assertThrows(CyclicDependenciesException.class, () -> config.getContext());

            Set<Class<?>> classes = Sets.newSet(exception.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(Component.class));
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

        static class CyclicComponentInjectConstructor implements Component {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements Component {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements Component {
            @Inject
            public void install(Dependency dependency) {
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(Component component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            Component component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            public void install(Component component) {
            }
        }

        // A -> B -> C -> A
        @ParameterizedTest(name = "transitive cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        public void should_throw_exception_if_transitive_cyclic_dependencies(Class<? extends Component> componentType,
                                                                            Class<? extends Dependency> dependencyType,
                                                                            Class<? extends AnotherDependency> anotherDependencyType) {
            config.bind(Component.class, componentType);
            config.bind(Dependency.class, dependencyType);
            config.bind(AnotherDependency.class, anotherDependencyType);

            CyclicDependenciesException exception =
                    assertThrows(CyclicDependenciesException.class, () -> config.getContext());

            List<Class<?>> components = Arrays.stream(exception.getComponents()).toList();

            assertEquals(3, components.size());
            assertTrue(components.contains(Component.class));
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
            public CyclicDependencyInjectConstructorWithComponent(Component component) {
            }
        }

        static class CyclicDependencyInjectFieldWithComponent implements AnotherDependency {
            @Inject
            Component component;
        }

        static class CyclicDependencyInjectMethodWithComponent implements AnotherDependency {
            @Inject
            public void install(Component component) {
            }
        }
    }
}
