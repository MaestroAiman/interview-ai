package com.pfa.interviewai.converter;

import com.pfa.interviewai.model.enums.InterviewType;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

@FacesConverter("InterviewTypeConverter")
public class InterviewTypeConverter implements Converter<InterviewType> {

    @Override
    public InterviewType getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) return null;
        try { return InterviewType.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, InterviewType value) {
        return value != null ? value.name() : "";
    }
}
