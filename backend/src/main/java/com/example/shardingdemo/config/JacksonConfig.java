package com.example.shardingdemo.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
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

/**
 * 全局时间序列化。
 * <p>
 * {@link Instant}：数据语义为 UTC；JSON 输出为东八区墙钟 {@code yyyy-MM-dd HH:mm:ss}；反序列化按同一时间串解析为东八区再转 {@link Instant}。
 * </p>
 */
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
            builder.serializers(
                    new InstantJsonSerializer(),
                    new LocalDateTimeSerializer(DATE_TIME_FMT),
                    new LocalDateSerializer(DATE_FMT),
                    new LocalTimeSerializer(TIME_FMT));
            builder.deserializers(
                    new InstantJsonDeserializer(),
                    new LocalDateTimeDeserializer(DATE_TIME_FMT),
                    new LocalDateDeserializer(DATE_FMT),
                    new LocalTimeDeserializer(TIME_FMT));
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
