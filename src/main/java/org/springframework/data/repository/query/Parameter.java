/*
 * Copyright 2008-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.query;

import static java.lang.String.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Vector;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.util.ClassUtils;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Class to abstract a single parameter of a query method. It is held in the context of a {@link Parameters} instance.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Greg Turnquist
 * @author Johannes Englmeier
 */
public class Parameter {

	static final List<Class<?>> TYPES;

	private static final String NAMED_PARAMETER_TEMPLATE = ":%s";
	private static final String POSITION_PARAMETER_TEMPLATE = "?%s";

	private final MethodParameter parameter;
	private final Class<?> parameterType;
	private final boolean isScoreRange;
	private final boolean isDynamicProjectionParameter;
	private final Lazy<Optional<String>> name;

	static {

		List<Class<?>> types = new ArrayList<>(
				Arrays.asList(ScrollPosition.class, Pageable.class, Sort.class, Limit.class));

		// consider Kotlin Coroutines Continuation a special parameter. That parameter is synthetic and should not get
		// bound to any query.

		ClassUtils.ifPresent("kotlin.coroutines.Continuation", Parameter.class.getClassLoader(), types::add);

		TYPES = Collections.unmodifiableList(types);
	}


	/**
	 * Creates a new {@link Parameter} for the given {@link MethodParameter} and domain {@link TypeInformation}.
	 *
	 * @param parameter must not be {@literal null}.
	 * @param domainType must not be {@literal null}.
	 * @since 3.0.2
	 */
	protected Parameter(MethodParameter parameter, TypeInformation<?> domainType) {

		Assert.notNull(parameter, "MethodParameter must not be null");
		Assert.notNull(domainType, "TypeInformation must not be null!");

		this.parameter = parameter;
		this.isScoreRange = Range.class.isAssignableFrom(parameter.getParameterType())
				&& ResolvableType.forMethodParameter(parameter).getGeneric(0).isAssignableFrom(Score.class);
		this.parameterType = potentiallyUnwrapParameterType(parameter);
		this.isDynamicProjectionParameter = isDynamicProjectionParameter(parameter, domainType);
		this.name = Lazy.of(() -> {
			Param annotation = parameter.getParameterAnnotation(Param.class);
			return Optional.ofNullable(annotation == null ? parameter.getParameterName() : annotation.value());
		});
	}

	/**
	 * Returns whether the parameter is a special parameter.
	 *
	 * @return
	 * @see #TYPES
	 */
	public boolean isSpecialParameter() {
		return isDynamicProjectionParameter || isSpecialParameterType(parameter.getParameterType());
	}

	/**
	 * Returns whether the {@link Parameter} is to be bound to a query.
	 *
	 * @return
	 */
	public boolean isBindable() {
		return !isSpecialParameter();
	}

	/**
	 * Returns whether the current {@link Parameter} is the one used for dynamic projections.
	 *
	 * @return
	 */
	public boolean isDynamicProjectionParameter() {
		return isDynamicProjectionParameter;
	}

	/**
	 * Returns the placeholder to be used for the parameter. Can either be a named one or positional.
	 *
	 * @return
	 */
	@SuppressWarnings("NullAway")
	public String getPlaceholder() {

		if (isNamedParameter()) {
			return format(NAMED_PARAMETER_TEMPLATE, getName().get());
		} else {
			return format(POSITION_PARAMETER_TEMPLATE, getIndex());
		}
	}

	/**
	 * Returns the position index the parameter is bound to in the context of its surrounding {@link Parameters}.
	 *
	 * @return
	 */
	public int getIndex() {
		return parameter.getParameterIndex();
	}

	/**
	 * Returns whether the parameter is annotated with {@link Param} or has a method parameter name.
	 *
	 * @return
	 * @see Param
	 * @see ParameterNameDiscoverer
	 */
	public boolean isNamedParameter() {
		return !isSpecialParameter() && getName().isPresent();
	}

	/**
	 * Returns whether the parameter is named explicitly, i.e. annotated with {@link Param}.
	 *
	 * @return
	 * @since 1.11
	 * @see Param
	 */
	public boolean isExplicitlyNamed() {
		return parameter.hasParameterAnnotation(Param.class);
	}

	/**
	 * Returns the name of the parameter (through {@link Param} annotation or method parameter naming).
	 *
	 * @return the optional name of the parameter.
	 * @see Param
	 * @see org.springframework.core.ParameterNameDiscoverer
	 */
	public Optional<String> getName() {
		return this.name.get();
	}

	/**
	 * Returns the required name of the parameter (through {@link Param} annotation or method parameter naming) or throws
	 * {@link IllegalStateException} if the parameter has no name.
	 *
	 * @return the required parameter name.
	 * @throws IllegalStateException if the parameter has no name.
	 * @since 3.4
	 * @see Param
	 * @see org.springframework.core.ParameterNameDiscoverer
	 */
	public String getRequiredName() {

		return getName().orElseThrow(() -> new IllegalStateException("Parameter " + parameter
				+ " is not named. For queries with named parameters you need to provide names for method parameters; Use @Param for query method parameters, or use the javac flag -parameters."));
	}

	/**
	 * Returns the type of the {@link Parameter}.
	 *
	 * @return the type
	 */
	public Class<?> getType() {
		return parameterType;
	}


	@Override
	public String toString() {
		return format("%s:%s", isNamedParameter() ? getName() : "#" + getIndex(), getType().getName());
	}

	/**
	 * @return {@literal true} if the {@link Parameter} is a {@link Vector} parameter.
	 * @since 4.0
	 */
	boolean isVector() {
		return Vector.class.isAssignableFrom(getType());
	}

	/**
	 * @return {@literal true} if the {@link Parameter} is a {@link Score} parameter.
	 * @since 4.0
	 */
	boolean isScore() {
		return Score.class.isAssignableFrom(getType());
	}

	/**
	 * @return {@literal true} if the {@link Parameter} is a {@link Range} of {@link Score} parameter.
	 * @since 4.0
	 */
	boolean isScoreRange() {
		return isScoreRange;
	}

	/**
	 * @return {@literal true} if the {@link Parameter} is a {@link ScrollPosition} parameter.
	 * @since 3.1
	 */
	boolean isScrollPosition() {
		return ScrollPosition.class.isAssignableFrom(getType());
	}

	/**
	 * @return {@literal true} if the {@link Parameter} is a {@link Pageable} parameter.
	 */
	boolean isPageable() {
		return Pageable.class.isAssignableFrom(getType());
	}

	/**
	 * @return {@literal true} if the {@link Parameter} is a {@link Sort} parameter.
	 */
	boolean isSort() {
		return Sort.class.isAssignableFrom(getType());
	}

	/**
	 * @return {@literal true} if the {@link Parameter} is a {@link Limit} parameter.
	 * @since 3.2
	 */
	boolean isLimit() {
		return Limit.class.isAssignableFrom(getType());
	}

	/**
	 * Returns whether the given {@link MethodParameter} is a dynamic projection parameter, which means it carries a
	 * dynamic type parameter which is identical to the type parameter of the actually returned type.
	 * <p>
	 * <code>
	 * <T> Collection<T> findBy…(…, Class<T> type);
	 * </code>
	 *
	 * @param parameter must not be {@literal null}.
	 * @param domainType the reference domain type, must not be {@literal null}.
	 * @return
	 */
	private static boolean isDynamicProjectionParameter(MethodParameter parameter, TypeInformation<?> domainType) {

		if (!parameter.getParameterType().equals(Class.class)) {
			return false;
		}

		if (parameter.hasParameterAnnotation(Param.class)) {
			return false;
		}

		var method = parameter.getMethod();

		if (method == null) {
			throw new IllegalArgumentException("Parameter is not associated with any method");
		}

		var returnType = TypeInformation.fromReturnTypeOf(method, parameter.getContainingClass());
		var unwrapped = QueryExecutionConverters.unwrapWrapperTypes(returnType);
		var reactiveUnwrapped = ReactiveWrapperConverters.unwrapWrapperTypes(unwrapped);

		if (domainType.isAssignableFrom(reactiveUnwrapped)) {
			return false;
		}

		return reactiveUnwrapped.equals(TypeInformation.fromMethodParameter(parameter).getComponentType());
	}

	/**
	 * Returns whether the {@link MethodParameter} is wrapped in a wrapper type.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return
	 * @see QueryExecutionConverters
	 */
	private static boolean isWrapped(MethodParameter parameter) {
		return QueryExecutionConverters.supports(parameter.getParameterType())
				|| ReactiveWrapperConverters.supports(parameter.getParameterType());
	}

	/**
	 * Returns whether the {@link MethodParameter} should be unwrapped.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return
	 * @see QueryExecutionConverters
	 */
	private static boolean shouldUnwrap(MethodParameter parameter) {
		return QueryExecutionConverters.supportsUnwrapping(parameter.getParameterType());
	}

	/**
	 * Returns the component type if the given {@link MethodParameter} is a wrapper type and the wrapper should be
	 * unwrapped.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return
	 */
	private static Class<?> potentiallyUnwrapParameterType(MethodParameter parameter) {

		Class<?> originalType = parameter.getParameterType();

		if (isWrapped(parameter) && shouldUnwrap(parameter)) {
			return ResolvableType.forMethodParameter(parameter).getGeneric(0).resolve(Object.class);
		}

		return originalType;
	}

	/**
	 * Identify is a given {@link Class} is either part of {@code TYPES} or an instanceof of one of its members. For
	 * example, {@code PageRequest} is an instance of {@code Pageable} (a member of {@code TYPES}).
	 *
	 * @param parameterType must not be {@literal null}.
	 * @return boolean
	 */
	private static boolean isSpecialParameterType(Class<?> parameterType) {

		for (Class<?> specialParameterType : TYPES) {
			if (specialParameterType.isAssignableFrom(parameterType)) {
				return true;
			}
		}

		return false;
	}
}
