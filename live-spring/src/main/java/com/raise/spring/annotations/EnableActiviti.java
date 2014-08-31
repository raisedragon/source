package com.raise.spring.annotations;

import org.springframework.context.annotation.Import;

import com.raise.spring.components.config.annotations.ActivitiConfiguration;

import java.lang.annotation.*;

/**
 * @author Josh Long
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ActivitiConfiguration.class)
public @interface EnableActiviti {
}
