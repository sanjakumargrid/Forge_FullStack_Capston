package com.talentgrid.jobposting.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.jobposting.dto.embedded.AnalyticsDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AnalyticsConverter implements AttributeConverter<AnalyticsDto, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(AnalyticsDto attribute) {
        if (attribute == null) return null;
        try { return MAPPER.writeValueAsString(attribute); } catch (Exception e) { return null; }
    }

    @Override
    public AnalyticsDto convertToEntityAttribute(String dbData) {
        if (dbData == null) return new AnalyticsDto();
        try { return MAPPER.readValue(dbData, AnalyticsDto.class); } catch (Exception e) { return new AnalyticsDto(); }
    }
}
