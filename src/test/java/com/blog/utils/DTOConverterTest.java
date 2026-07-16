package com.blog.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DTOConverterTest {

    @Test
    void convert_null_shouldReturnNull() {
        assertThat(DTOConverter.convert(null, String.class)).isNull();
    }

    @Test
    void convert_validObjects_shouldCopyProperties() {
        Source source = new Source();
        source.setName("test");
        source.setValue(123);

        Target target = DTOConverter.convert(source, Target.class);

        assertThat(target).isNotNull();
        assertThat(target.getName()).isEqualTo("test");
        assertThat(target.getValue()).isEqualTo(123);
    }

    @Test
    void convert_withCustomizer_shouldApplyCustomLogic() {
        Source source = new Source();
        source.setName("test");
        source.setValue(123);

        Target target = DTOConverter.convert(source, Target.class, t -> t.setName("custom"));

        assertThat(target.getName()).isEqualTo("custom");
        assertThat(target.getValue()).isEqualTo(123);
    }

    @Test
    void convert_withBiConsumer_shouldSeeBothSides() {
        Source source = new Source();
        source.setName("test");
        source.setValue(123);

        Target target = DTOConverter.convert(source, Target.class, (s, t) -> {
            t.setName(s.getName().toUpperCase());
            t.setValue(s.getValue() * 2);
        });

        assertThat(target.getName()).isEqualTo("TEST");
        assertThat(target.getValue()).isEqualTo(246);
    }

    @Test
    void convertList_null_shouldReturnEmpty() {
        List<Target> result = DTOConverter.convertList(null, Target.class);
        assertThat(result).isEmpty();
    }

    @Test
    void convertList_validList_shouldConvertAll() {
        Source s1 = new Source();
        s1.setName("a");
        s1.setValue(1);
        Source s2 = new Source();
        s2.setName("b");
        s2.setValue(2);

        List<Target> result = DTOConverter.convertList(List.of(s1, s2), Target.class);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("a");
        assertThat(result.get(1).getValue()).isEqualTo(2);
    }

    @Test
    void convertList_withCustomizer_shouldApplyToEach() {
        Source s1 = new Source();
        s1.setName("a");
        s1.setValue(1);

        List<Target> result = DTOConverter.convertList(
                List.of(s1),
                Target.class,
                t -> t.setName("batch")
        );

        assertThat(result.get(0).getName()).isEqualTo("batch");
    }

    @Test
    void convert_targetWithoutDefaultConstructor_shouldThrow() {
        assertThatThrownBy(() -> DTOConverter.convert(new Source(), NoDefaultConstructor.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DTO转换失败");
    }

    static class Source {
        private String name;
        private int value;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }

    static class Target {
        private String name;
        private int value;

        public Target() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }

    static class NoDefaultConstructor {
        public NoDefaultConstructor(String arg) {}
    }
}
