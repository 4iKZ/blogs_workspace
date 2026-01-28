package com.blog.integration;

import com.blog.utils.RedisUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis Jackson序列化/反序列化测试
 * 覆盖各种序列化/反序列化场景，确保系统在各种情况下都能正常工作
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class RedisJacksonSerializationTest {

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 测试前清除所有测试相关的Redis缓存
     */
    @BeforeEach
    public void setUp() {
        log.info("测试前清除所有测试相关的Redis缓存");
        Set<String> testKeys = redisUtils.keys("test:serialization:*");
        if (testKeys != null && !testKeys.isEmpty()) {
            redisUtils.delete(testKeys);
        }
    }

    /**
     * 测试后清除所有测试相关的Redis缓存
     */
    @AfterEach
    public void tearDown() {
        log.info("测试后清除所有测试相关的Redis缓存");
        setUp();
    }

    /**
     * 测试基本数据类型的序列化/反序列化
     */
    @Test
    public void testBasicDataTypes() {
        log.info("测试基本数据类型的序列化/反序列化");

        // 测试字符串类型
        String stringKey = "test:serialization:string";
        redisUtils.set(stringKey, "测试字符串");
        String stringValue = redisUtils.get(stringKey);
        assertEquals("测试字符串", stringValue, "字符串类型序列化/反序列化失败");
        log.info("字符串类型测试通过");

        // 测试整数类型
        String intKey = "test:serialization:int";
        redisUtils.set(intKey, 123);
        Integer intValue = redisUtils.get(intKey);
        assertEquals(123, intValue, "整数类型序列化/反序列化失败");
        log.info("整数类型测试通过");

        // 测试长整数类型
        String longKey = "test:serialization:long";
        redisUtils.set(longKey, 1234567890L);
        Number longValue = redisUtils.get(longKey);
        assertNotNull(longValue, "长整数类型序列化/反序列化失败");
        assertEquals(1234567890L, longValue.longValue(), "长整数类型序列化/反序列化失败");
        log.info("长整数类型测试通过");

        // 测试布尔类型
        String booleanKey = "test:serialization:boolean";
        redisUtils.set(booleanKey, true);
        Boolean booleanValue = redisUtils.get(booleanKey);
        assertEquals(true, booleanValue, "布尔类型序列化/反序列化失败");
        log.info("布尔类型测试通过");

        // 测试浮点数类型
        String doubleKey = "test:serialization:double";
        redisUtils.set(doubleKey, 3.14159);
        Double doubleValue = redisUtils.get(doubleKey);
        assertEquals(3.14159, doubleValue, "浮点数类型序列化/反序列化失败");
        log.info("浮点数类型测试通过");
    }

    /**
     * 测试空值的序列化/反序列化
     */
    @Test
    public void testNullValues() {
        log.info("测试空值的序列化/反序列化");

        // 测试null值
        String nullKey = "test:serialization:null";
        redisUtils.set(nullKey, null);
        Object nullValue = redisUtils.get(nullKey);
        assertNull(nullValue, "null值序列化/反序列化失败");
        log.info("null值测试通过");

        // 测试空字符串
        String emptyStringKey = "test:serialization:emptyString";
        redisUtils.set(emptyStringKey, "");
        String emptyStringValue = redisUtils.get(emptyStringKey);
        assertEquals("", emptyStringValue, "空字符串序列化/反序列化失败");
        log.info("空字符串测试通过");

        // 测试空列表
        String emptyListKey = "test:serialization:emptyList";
        List<String> emptyList = new ArrayList<>();
        redisUtils.set(emptyListKey, emptyList);
        List<String> emptyListValue = redisUtils.get(emptyListKey);
        assertNotNull(emptyListValue, "空列表序列化/反序列化失败");
        assertTrue(emptyListValue.isEmpty(), "空列表序列化/反序列化后不为空");
        log.info("空列表测试通过");

        // 测试空对象
        String emptyObjectKey = "test:serialization:emptyObject";
        TestObject emptyObject = new TestObject();
        redisUtils.set(emptyObjectKey, emptyObject);
        TestObject emptyObjectValue = redisUtils.get(emptyObjectKey);
        assertNotNull(emptyObjectValue, "空对象序列化/反序列化失败");
        assertNull(emptyObjectValue.getName(), "空对象序列化/反序列化后名称不为空");
        assertNull(emptyObjectValue.getValue(), "空对象序列化/反序列化后值不为空");
        log.info("空对象测试通过");
    }

    /**
     * 测试集合类型的序列化/反序列化
     */
    @Test
    public void testCollectionTypes() {
        log.info("测试集合类型的序列化/反序列化");

        // 测试列表类型
        String listKey = "test:serialization:list";
        List<String> list = Arrays.asList("元素1", "元素2", "元素3");
        redisUtils.set(listKey, list);
        List<String> listValue = redisUtils.get(listKey);
        assertNotNull(listValue, "列表类型序列化/反序列化失败");
        assertEquals(3, listValue.size(), "列表类型序列化/反序列化后大小不一致");
        assertEquals(list, listValue, "列表类型序列化/反序列化后内容不一致");
        log.info("列表类型测试通过");

        // 测试集合类型
        String setKey = "test:serialization:set";
        Set<String> set = new HashSet<>(Arrays.asList("元素1", "元素2", "元素3"));
        redisUtils.set(setKey, set);
        Set<String> setValue = redisUtils.get(setKey);
        assertNotNull(setValue, "集合类型序列化/反序列化失败");
        assertEquals(3, setValue.size(), "集合类型序列化/反序列化后大小不一致");
        assertEquals(set, setValue, "集合类型序列化/反序列化后内容不一致");
        log.info("集合类型测试通过");

        // 测试映射类型
        String mapKey = "test:serialization:map";
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 123);
        map.put("key3", true);
        redisUtils.set(mapKey, map);
        Map<String, Object> mapValue = redisUtils.get(mapKey);
        assertNotNull(mapValue, "映射类型序列化/反序列化失败");
        assertEquals(3, mapValue.size(), "映射类型序列化/反序列化后大小不一致");
        assertEquals(map, mapValue, "映射类型序列化/反序列化后内容不一致");
        log.info("映射类型测试通过");
    }

    /**
     * 测试日期时间类型的序列化/反序列化
     */
    @Test
    public void testDateTimeTypes() {
        log.info("测试日期时间类型的序列化/反序列化");

        // 测试Date类型
        String dateKey = "test:serialization:date";
        Date date = new Date();
        redisUtils.set(dateKey, date);
        Date dateValue = redisUtils.get(dateKey);
        assertNotNull(dateValue, "Date类型序列化/反序列化失败");
        assertEquals(date.getTime(), dateValue.getTime(), "Date类型序列化/反序列化后时间不一致");
        log.info("Date类型测试通过");
    }

    /**
     * 测试自定义对象的序列化/反序列化
     */
    @Test
    public void testCustomObjects() {
        log.info("测试自定义对象的序列化/反序列化");

        // 测试基本自定义对象
        String testObjectKey = "test:serialization:testObject";
        TestObject testObject = new TestObject();
        testObject.setName("测试对象");
        testObject.setValue(123);
        testObject.setCreatedAt(new Date());
        redisUtils.set(testObjectKey, testObject);
        TestObject testObjectValue = redisUtils.get(testObjectKey);
        assertNotNull(testObjectValue, "自定义对象序列化/反序列化失败");
        assertEquals(testObject.getName(), testObjectValue.getName(), "自定义对象序列化/反序列化后名称不一致");
        assertEquals(testObject.getValue(), testObjectValue.getValue(), "自定义对象序列化/反序列化后值不一致");
        assertEquals(testObject.getCreatedAt().getTime(), testObjectValue.getCreatedAt().getTime(), "自定义对象序列化/反序列化后创建时间不一致");
        log.info("基本自定义对象测试通过");

        // 测试嵌套自定义对象
        String nestedObjectKey = "test:serialization:nestedObject";
        NestedTestObject nestedTestObject = new NestedTestObject();
        nestedTestObject.setName("嵌套测试对象");
        nestedTestObject.setTestObject(testObject);
        nestedTestObject.setValues(Arrays.asList("值1", "值2", "值3"));
        redisUtils.set(nestedObjectKey, nestedTestObject);
        NestedTestObject nestedTestObjectValue = redisUtils.get(nestedObjectKey);
        assertNotNull(nestedTestObjectValue, "嵌套自定义对象序列化/反序列化失败");
        assertEquals(nestedTestObject.getName(), nestedTestObjectValue.getName(), "嵌套自定义对象序列化/反序列化后名称不一致");
        assertNotNull(nestedTestObjectValue.getTestObject(), "嵌套自定义对象序列化/反序列化后内部对象为空");
        assertEquals(nestedTestObject.getTestObject().getName(), nestedTestObjectValue.getTestObject().getName(), "嵌套自定义对象序列化/反序列化后内部对象名称不一致");
        assertEquals(nestedTestObject.getValues(), nestedTestObjectValue.getValues(), "嵌套自定义对象序列化/反序列化后列表内容不一致");
        log.info("嵌套自定义对象测试通过");
    }

    /**
     * 测试泛型类型的序列化/反序列化
     */
    @Test
    public void testGenericTypes() {
        log.info("测试泛型类型的序列化/反序列化");

        // 测试泛型对象
        String genericObjectKey = "test:serialization:genericObject";
        GenericTestObject<String> genericTestObject = new GenericTestObject<>();
        genericTestObject.setData("泛型测试数据");
        genericTestObject.setTimestamp(new Date());
        redisUtils.set(genericObjectKey, genericTestObject);
        GenericTestObject<String> genericTestObjectValue = redisUtils.get(genericObjectKey);
        assertNotNull(genericTestObjectValue, "泛型对象序列化/反序列化失败");
        assertEquals(genericTestObject.getData(), genericTestObjectValue.getData(), "泛型对象序列化/反序列化后数据不一致");
        assertEquals(genericTestObject.getTimestamp().getTime(), genericTestObjectValue.getTimestamp().getTime(), "泛型对象序列化/反序列化后时间戳不一致");
        log.info("泛型对象测试通过");

        // 测试不同类型的泛型
        String genericObjectIntegerKey = "test:serialization:genericObjectInteger";
        GenericTestObject<Integer> genericTestObjectInteger = new GenericTestObject<>();
        genericTestObjectInteger.setData(456);
        genericTestObjectInteger.setTimestamp(new Date());
        redisUtils.set(genericObjectIntegerKey, genericTestObjectInteger);
        GenericTestObject<Integer> genericTestObjectIntegerValue = redisUtils.get(genericObjectIntegerKey);
        assertNotNull(genericTestObjectIntegerValue, "整数泛型对象序列化/反序列化失败");
        assertEquals(genericTestObjectInteger.getData(), genericTestObjectIntegerValue.getData(), "整数泛型对象序列化/反序列化后数据不一致");
        log.info("整数泛型对象测试通过");
    }

    /**
     * 测试推荐文章缓存的序列化/反序列化
     */
    @Test
    public void testRecommendedArticlesSerialization() {
        log.info("测试推荐文章缓存的序列化/反序列化");

        // 测试空的推荐文章列表
        String emptyRecommendedArticlesKey = "test:serialization:recommendedArticlesEmpty";
        List<String> emptyRecommendedArticles = new ArrayList<>();
        redisUtils.set(emptyRecommendedArticlesKey, emptyRecommendedArticles);
        List<String> emptyRecommendedArticlesValue = redisUtils.get(emptyRecommendedArticlesKey);
        assertNotNull(emptyRecommendedArticlesValue, "空推荐文章列表序列化/反序列化失败");
        assertTrue(emptyRecommendedArticlesValue.isEmpty(), "空推荐文章列表序列化/反序列化后不为空");
        log.info("空推荐文章列表测试通过");

        // 测试非空的推荐文章列表
        String recommendedArticlesKey = "test:serialization:recommendedArticles";
        List<String> recommendedArticles = Arrays.asList("文章1", "文章2", "文章3");
        redisUtils.set(recommendedArticlesKey, recommendedArticles);
        List<String> recommendedArticlesValue = redisUtils.get(recommendedArticlesKey);
        assertNotNull(recommendedArticlesValue, "非空推荐文章列表序列化/反序列化失败");
        assertFalse(recommendedArticlesValue.isEmpty(), "非空推荐文章列表序列化/反序列化后为空");
        assertEquals(recommendedArticles.size(), recommendedArticlesValue.size(), "非空推荐文章列表序列化/反序列化后大小不一致");
        assertEquals(recommendedArticles, recommendedArticlesValue, "非空推荐文章列表序列化/反序列化后内容不一致");
        log.info("非空推荐文章列表测试通过");
    }

    /**
     * 测试各种边界情况的序列化/反序列化
     */
    @Test
    public void testEdgeCases() {
        log.info("测试各种边界情况的序列化/反序列化");

        // 测试大字符串
        String largeStringKey = "test:serialization:largeString";
        StringBuilder largeStringBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeStringBuilder.append("这是一个测试字符串，用于测试大字符串的序列化/反序列化。");
        }
        String largeString = largeStringBuilder.toString();
        redisUtils.set(largeStringKey, largeString);
        String largeStringValue = redisUtils.get(largeStringKey);
        assertEquals(largeString, largeStringValue, "大字符串序列化/反序列化失败");
        log.info("大字符串测试通过");

        // 测试包含特殊字符的字符串
        String specialCharsKey = "test:serialization:specialChars";
        String specialChars = "这是一个包含特殊字符的字符串：!@#$%^&*()_+-=[]{}|;:,.<>?/\"'\\";
        redisUtils.set(specialCharsKey, specialChars);
        String specialCharsValue = redisUtils.get(specialCharsKey);
        assertEquals(specialChars, specialCharsValue, "包含特殊字符的字符串序列化/反序列化失败");
        log.info("包含特殊字符的字符串测试通过");
    }

    /**
     * 测试对象
     */
    @Data
    private static class TestObject {
        private String name;
        private Integer value;
        private Date createdAt;
    }

    /**
     * 嵌套测试对象
     */
    @Data
    private static class NestedTestObject {
        private String name;
        private TestObject testObject;
        private List<String> values;
    }

    /**
     * 泛型测试对象
     */
    @Data
    private static class GenericTestObject<T> {
        private T data;
        private Date timestamp;
    }
}
