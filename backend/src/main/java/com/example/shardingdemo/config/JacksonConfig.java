package com.example.shardingdemo.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern(TIME_PATTERN);

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            builder.timeZone(TimeZone.getTimeZone(DISPLAY_ZONE));
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // ========== 修复点：使用 SimpleModule 注册序列化器 ==========
            SimpleModule module = new SimpleModule();

            // 序列化
            module.addSerializer(Instant.class, new InstantJsonSerializer());
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FMT));
            module.addSerializer(java.time.LocalDate.class, new LocalDateSerializer(DATE_FMT));
            module.addSerializer(java.time.LocalTime.class, new LocalTimeSerializer(TIME_FMT));

            // 反序列化
            module.addDeserializer(Instant.class, new InstantJsonDeserializer());
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FMT));
            module.addDeserializer(java.time.LocalDate.class, new LocalDateDeserializer(DATE_FMT));
            module.addDeserializer(java.time.LocalTime.class, new LocalTimeDeserializer(TIME_FMT));

            builder.modules(module);
        };
    }

    /** UTC Instant → 前端展示：东八区格式化字符串 */
    private static final class InstantJsonSerializer extends JsonSerializer<Instant> {
        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            ZonedDateTime zdt = ZonedDateTime.ofInstant(value, DISPLAY_ZONE);
            gen.writeString(zdt.format(DATE_TIME_FMT));
        }
    }

    /** 前端/接口入参：解析为 LocalDateTime（东八区墙钟）→ ZonedDateTime → Instant（UTC） */
    private static final class InstantJsonDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            LocalDateTime ldt = LocalDateTime.parse(text.trim(), DATE_TIME_FMT);
            return ldt.atZone(DISPLAY_ZONE).toInstant();
        }
    }
}