package com._6.ems.converter;

import java.time.Duration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DurationConvert implements AttributeConverter<Duration, Long>{

    @Override
    public Long convertToDatabaseColumn(Duration duration) {
        return (duration == null) ? null : duration.getSeconds();
    }

    @Override
    public Duration convertToEntityAttribute(Long seconds) {
        return (seconds == null) ? null : Duration.ofSeconds(seconds);
    }
}
