package com.ray3k.skincomposer;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Stuff annotated with this will be ignored by gwt
 */
@Retention(value = CLASS)
@Target(value = {TYPE, METHOD, CONSTRUCTOR, FIELD})
@Documented
public @interface GwtIncompatible {}
