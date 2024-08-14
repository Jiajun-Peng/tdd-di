package world.nobug.tdd.di;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

//        @ParameterizedTest(name = "supporting {0}")
//        @MethodSource
//        public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
//            Dependency dependency = new Dependency() {
//            };
//            config.bind(Dependency.class, dependency
//            );
//            config.bind(Component.class, componentType);
//
//            Optional<Component> component = config.getContext().get(Component.class);
//            assertTrue(component.isPresent());
//            assertSame(dependency, component.get());
//        }

//        public static Stream<Arguments> should_bind_type_to_an_injectable_component() {
//            return Stream.of(
//                    Arguments.of(ComponentWithInjectConstructor.class),
//                    Arguments.of(ComponentWithInjectField.class),
//                    Arguments.of(ComponentWithInjectMethod.class)
//            );
//        }

        // component does not exist
        @Test
        public void should_return_empty_if_component_not_defined() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }
    }

    @Nested
    public class DependencyCheck {

        // dependencies not exist
        @Test
        public void should_throw_exception_if_dependency_not_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });

            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }


        // cyclic dependencies
        @Test // A -> B -> A
        public void should_throw_exception_if_cyclic_dependencies() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependedOnComponent.class);

            CyclicDependenciesException exception =
                    assertThrows(CyclicDependenciesException.class, () -> config.getContext());

            Set<Class<?>> classes = Sets.newSet(exception.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(Component.class));
            assertTrue(classes.contains(Dependency.class));
        }

        @Test // A -> B -> C -> A
        public void should_throw_exception_if_transitive_cyclic_dependencies() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

            CyclicDependenciesException exception =
                    assertThrows(CyclicDependenciesException.class, () -> config.getContext());

            List<Class<?>> components = Arrays.stream(exception.getComponents()).toList();

            assertEquals(3, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }

    }
}
